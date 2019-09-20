package PNPLibrary;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static PNPLibrary.SafezoneManager.SAFEZONES_FOLDER_PATH;


public class Safezone {

    /*----------------------*/
    /*ATTRIBUTES OF SAFEZONE*/
    /*----------------------*/

    private int safezone_id;
    private String password ;
    private int sync_time;

    /*flag that will be update when it's done a change to the safezone without
    * communicating the change to other peers*/
    private boolean isSyn;

    /*keepers ip*/
    private ArrayList<String> keepers_ip;

    /*resources*/
    private ArrayList<Resource> resources;

    /*communication variable*/
    private Courier courier = null;


    /*-------------*/
    /* COSTRUCTORS */
    /*-------------*/

    Safezone(int safezone_id,String password){
        this(safezone_id,password,1);
    }

    Safezone(int safezone_id, String password, int sync_time){
        if(safezone_id > 0)
            this.safezone_id = safezone_id;

        this.password = password;
        this.sync_time = sync_time;
        isSyn = true;
        keepers_ip = new ArrayList<>();
        resources = new ArrayList<>();
        courier = CourierManager.Manager().createCourier();
    }


    /*--------------------------------*/
    /*JOINING AND EXITING THE SAFEZONE*/
    /*--------------------------------*/

    public void leave(){
        try {
            delete();
            Courier courier = CourierManager.Manager().createCourier();

            for(String keeper : keepers_ip){
                courier.connect(keeper);
                courier.leave_safezone(safezone_id,password);
                courier.disconnect();
            }

        }catch (IllegalArgumentException e){
            System.out.println("The safezone can't delete folder");
            e.printStackTrace();
        }catch (IOException e){
            System.out.println("Error sending leaving message to the keepers");
            e.printStackTrace();
        }
    }

    /*--------------------------------*/
    /*SYNCHRONIZATION WITH OTHER PEERS*/
    /*--------------------------------*/

    /*try to find  an online keeper*/
    private String search_a_keeper() throws  IOException{
        String keeper = null;


        for(int i = 0; i< keepers_ip.size() && keeper == null ; i++){
            if(NetworkManger.isOnlinePeer(keepers_ip.get(i)))
                keeper = keepers_ip.get(i);
        }

        if(keeper == null)
            throw new IOException();

        return keeper;
    }

    /*see the documentation */
    public void syn() throws IOException {

        System.out.println("[SAFEZONE "+getID()+"]"  +" start syn...");

        load_online_log_file();

        for(int i = 0; i< resources.size(); i++){
            Resource res = resources.get(i);

            // SYNC TYPE = RESTRICTED
            if(res.getSyn_type() == Resource.RESTRICTED  ){
                if( res.getOwner_ip().equals(NetworkManger.getMyIP()) )
                    update_resource(res);
                else {
                    if(res.isLocal_modified())
                        create_local_copy(res);
                    if(res.isRemote_modified())
                        retrieve_new_version(res);
                }


            }

            // SYNC TYPE = SHARED
            if(res.getSyn_type() == Resource.SHARED){
                if(res.isLocal_modified()){
                    if(res.isRemote_modified()){
                        create_local_copy(res);
                        retrieve_new_version(res);
                    }else update_resource(res);

                    res.addLog_local(new BaseLog(new Date(),"CHK",NetworkManger.getMyIP()));
                }else if(res.isRemote_modified())
                        retrieve_new_version(res);
            }

        }
        System.out.println("[SAFEZONE "+getID()+"]"  +" SYN DONE");

    }

    /*------------------------*/
    /*FILE MANAGEMENTS METHODS*/
    /*------------------------*/

    /*downloads the cloud version of that resources*/
    private void retrieve_new_version(Resource res) throws IOException  {
        String keeper = search_a_keeper();
        courier.connect(keeper,Courier.PORT);
        byte[] file = courier.file_request(safezone_id,password,res.getName());
        courier.disconnect();
        overwrite_file(res.getName(),file);
        res.setRemote_modified(false);
    }

    /*Replaces a file with a new data file*/
    public void overwrite_file(String file_name, byte[] file_data) throws IOException {
        Files.write(Paths.get(getFolderPath() + "\\" + file_name),file_data);
    }

    /*creates a local copy of a resource*/
    private void create_local_copy(Resource res) throws IOException {
        Path copied = Paths.get(getFolderPath()+"\\local_"+res.getName());
        Path originalPath = Paths.get( getFolderPath()+"\\"+res.getName());
        Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
    }

    /*reports to other peers that a file has been updated*/
    private void update_resource(Resource res) {
        for (String keeper_ip: keepers_ip ) {
            try {
                courier.connect(keeper_ip,Courier.PORT);
                courier.report_file_update(safezone_id, password, res.getName());
                courier.disconnect();
            } catch (IOException e) {
                System.out.println(keeper_ip+" is unreachable");
            }

        }
    }

    /*delete the whole safezone folder and the resources owned by it*/
    private void delete() throws  IllegalArgumentException{
        File folder = new File(getFolderPath());
        for(String s : folder.list()){
            delete_file(s);
        }
        delete_file(folder.getPath());
    }

    private void delete_file(String path) throws IllegalArgumentException{
        // Creo un oggetto file
        File f = new File(path);

        // Mi assicuro che il file esista
        if (!f.exists())
            throw new IllegalArgumentException("Il File o la Directory non esiste: " + path);

        // Mi assicuro che il file sia scrivibile
        if (!f.canWrite())
            throw new IllegalArgumentException("Non ho il permesso di scrittura: " + path);

        // Se è una cartella verifico che sia vuota
        if (f.isDirectory()) {
            String[] files = f.list();
            if (files.length > 0)
                throw new IllegalArgumentException("La Directory non è vuota: " + path);
        }

        // Profo a cancellare
        boolean success = f.delete();

        // Se si è verificato un errore...
        if (!success)
            throw new IllegalArgumentException("Cancellazione fallita");
    }

    /*-----------------------------*/
    /*LOG MANAGEMENTS METHODS*/
    /*-----------------------------*/

    /*loading the info file*/


    public void load_info_file(byte[] file){
        String info_file = new String(file);
        String[] fields =  info_file.split("\r\n");
        setID( Integer.parseInt(fields[0]) );


        int line_counter = 0;

        line_counter++;
        setPassword(fields[line_counter]);

        line_counter++;
        setSync_time(Integer.parseInt(fields[line_counter]) );

        setSyn(false);

        line_counter++;
        int n_keepers = Integer.parseInt(fields[line_counter]);

        String keeper = null;


        for(int k = 0; k< n_keepers; k++) {
            line_counter++;
            keeper = fields[line_counter];
            if(!keeper.equals(NetworkManger.getMyIP()))
                addKeepers(keeper);
        }

        line_counter++;
        int n_resources = 0;
        try {
            n_resources = Integer.parseInt(fields[line_counter]);
        }catch (NumberFormatException | IndexOutOfBoundsException e){
            n_resources = 0;
        }




        for(int k = 0; k< n_resources; k++){
            line_counter++;
            String[] fields2 = fields[line_counter].split(" ");

            String name = fields2[0];
            String owner = fields2[1];
            String syn_type = fields[2].replace(" ","");
            addResource( Resource.importResource( name,
                    owner , Integer.parseInt(syn_type)  ));
        }

    }

    public synchronized void load_info_file(){
        try{
            String info_file_path = SAFEZONES_FOLDER_PATH + getID() + "\\" + getID() + ".sz";
            load_info_file(Files.readAllBytes(Paths.get(info_file_path)));
            /*
            BufferedReader br = new BufferedReader(
                    new FileReader(info_file_path));
            setID( Integer.parseInt(  br.readLine()) );

            setPassword(br.readLine());

            setSync_time(Integer.parseInt(br.readLine()));
            setSyn(false);

            /SETTING THE KEEPERS/

            int n_keepers = Integer.parseInt(br.readLine() );
            String keeper = null;

            for(int k = 0; k< n_keepers; k++) {
                keeper = br.readLine();
                if(!keeper.equals(NetworkManger.getMyIP()))
                    addKeepers(keeper);
            }

            int n_resources = Integer.parseInt(
                    br.readLine());
            for(int k = 0; k< n_resources; k++){
                String[] fields = br.readLine().split(" ");
                addResource( Resource.importResource( fields[0],
                        fields[1] , Integer.parseInt(fields[2])  ));
            }
            br.close();*/
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*writing info file into file or byte[]*/

    public byte[] get_info_file() {

        StringBuilder stringBuilder =  new StringBuilder();
        stringBuilder.append(getID()).append("\r\n");
        stringBuilder.append(getPassword()).append("\r\n");
        stringBuilder.append(getSync_time()).append("\r\n");
        stringBuilder.append( (getKeepersCount()+1)  ).append("\r\n");

        for(int i = 0; i< getKeepersCount(); i++) {
            stringBuilder.append(getKeeper(i)).append("\r\n");
        }

        stringBuilder.append(NetworkManger.getMyIP()).append("\r\n");

        int resources_count = getResourcesCount();
        stringBuilder.append(resources_count);

        for(int i = 0; i< resources_count; i++){
            stringBuilder.append("\r\n");
            Resource res =getResource(i);

            String line = res.getName()+" " + res.getOwner_ip()+" "+ res.getSyn_type();
            stringBuilder.append(line);
        }


        return stringBuilder.toString().getBytes();
    }


    /*update the safezone file*/
    public void update_safezone_file() throws IOException {
        /*Writing on file*/
        File file = new File(getInfoPath());
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));

        /*bw.write(int c) is used for character not int*/
        bw.write(String.valueOf(getID()) );
        bw.newLine();
        bw.write(getPassword());
        bw.newLine();
        bw.write(String.valueOf(getSync_time()) );
        bw.newLine();

        int keepers_count = getKeepersCount();
        bw.write( String.valueOf(keepers_count+1) );
        bw.newLine();
        for(int i = 0; i< keepers_count; i++) {
            bw.write(getKeeper(i));
            bw.newLine();
        }

        bw.write(NetworkManger.getMyIP());
        bw.newLine();

        int resources_count = getResourcesCount();
        bw.write(String.valueOf(resources_count));

        for(int i = 0; i< resources_count; i++){
            bw.newLine();
            Resource res =getResource(i);

            String line = res.getName()+" " + res.getOwner_ip()+" "+ res.getSyn_type();
            bw.write(line);
        }


        bw.close();
    }

    /*rewrite the current log file*/
    public void update_log_file() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(getCloudLogPath()))){
            for(int i = 0 ; i< resources.size(); i++) {
                Resource res = resources.get(i);
                bw.write("#" + (i+1));
                bw.newLine();
                bw.write(res.getFullOnlineLog());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*read the online log and adds to the log of the resources*/
    public void reload_online_log(){
        String log_file_path_cloud = getCloudLogPath();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(log_file_path_cloud));
            add_log(br,false);
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*retrieve the online file*/
    private   void load_online_log_file(){
        try{
            courier.connect(search_a_keeper(),Courier.PORT);

            String log_file_name = safezone_id+"_log.rs";
            byte[] b_log_file = courier.file_request(safezone_id, password, log_file_name);


            String log_file = new String(b_log_file);
            String lines[] = log_file.split("\r\n");
            int cnt = 0;

            String line = "";
            Resource res = null;

            while( cnt < lines.length) {
                line = lines[cnt];


                if( line.charAt(0) == '#'){
                    //in log file the resources start to count with 1
                    res = getResource( Integer.parseInt(line.substring(1))-1 );
                    cnt++;
                    continue;
                }

                String[] fields = line.split(" ");
                // the moment in which the change request or report is arrived
                Date date = new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss").parse(fields[0]);
                //type of modification
                String type_modif =fields[1];
                String peer = fields[2];


                assert res != null;

                if(type_modif.equals("RFU") && !peer.equals(NetworkManger.getMyIP())
                         && date.after(res.get_online_log(0).getDate() ) ){
                    res.setRemote_modified(true);
                    System.out.println("[SAFEZONE] the resource "+res.getName() +" is remotely changed ");
                }

                cnt++;
            }

            courier.disconnect();

            overwrite_file(log_file_name,b_log_file);
            reload_online_log();
        }catch (IOException | ParseException e) {
            System.out.println("[SAFEZONE] None of the keepers are reachable");
        }

    }

    /*actually it read the whole log_file  and check if the resources were modified*/
    public  void load_local_log_file(){
        String log_file_path = getLocalLogPath();
        String log_file_path_cloud = getCloudLogPath();

        try{

            File file = new File(log_file_path);
            if(!file.exists())
                file.createNewFile();

            BufferedReader br = new BufferedReader(new FileReader(log_file_path));
            add_log(br,true);
            br.close();

            br = new BufferedReader(new FileReader(log_file_path_cloud));
            add_log(br,false);
            br.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    /*for now the log file isn't full read but it is used only for knowing if the resources were modified locally*/
    private  void add_log(BufferedReader br,boolean is_local_file) throws IOException, ParseException {
        String line = "";
        Resource res = null;


        boolean goes_next_res = false;
        boolean first_update = true;
        /*when it meets the SYN, it means that the rest of message were already read by the peer and updated*/
        while( (line = br.readLine() ) != null){

            if( line.charAt(0) == '#'){
                //in log file the resources start to count with 1
                res = getResource( Integer.parseInt(line.substring(1))-1 );
                first_update = true;
                goes_next_res = false;

                res.clear_online_log();
                res.clear_local_log();

                continue;
            }

            String[] fields = line.split(" ");
            // the moment in which the change request or report is arrived
            Date date = new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss").parse(fields[0]);
            //type of modification
            String type_modif =fields[1];
            String who = fields[2];


            if(is_local_file && type_modif.equals("CHK") )
                goes_next_res = true;

            if(goes_next_res)
                continue;

            if(is_local_file) {
                if( first_update && type_modif.equals("RFU")){
                    System.out.println("[SAFEZONE] the resource "+res.getName() +" is locally changed ");
                    res.setLocal_modified(true);
                    res.setLast_update(date);
                    first_update = false;
                }
                res.addLog_local(new BaseLog(date, type_modif,who));
            }else {
                if(first_update && type_modif.equals("RFU") ){
                    res.setLast_update(date);
                    first_update = false;
                }

                res.addLog_online(new BaseLog(date, type_modif,who));
            }
        }
    }

    /*------------------------------------*/
    /*ACCESSORS METHOD: GETTERS/ SETTERS */
    /*------------------------------------*/
    public boolean isSyn(){
        return isSyn;
    }

    public String getLocalLogPath(){
        return SAFEZONES_FOLDER_PATH+ getID()+"\\"+getID()+"_log_local.rs";
    }

    public String getCloudLogPath(){
        return SAFEZONES_FOLDER_PATH+ getID()+"\\"+getID()+"_log.rs";
    }

    public Resource getResourceByName(String name){
        for(int i = 0; i< resources.size(); i++)
            if(resources.get(i).getName().equals(name))
                return resources.get(i);
        return null;
    }

    public Resource getResourceByIndex(int idx ){
        return resources.get(idx);
    }

    public String getInfoPath(){
        return getFolderPath()+"\\"+safezone_id+".sz";
    }

    public String getFolderPath(){
        return SAFEZONES_FOLDER_PATH+"\\"+safezone_id;
    }

    public int getID() {
        return safezone_id;
    }

    public void setID(int id){this.safezone_id = id;}

    public boolean isIDEqual(int id){
        return id == this.safezone_id;
    }

    public Resource getResource(int idx){
        return resources.get(idx);
    }

    public String getKeeper(int idx){
        return keepers_ip.get(idx);
    }

    public int getKeepersCount(){
        return keepers_ip.size();
    }

    public int getResourcesCount(){
        return resources.size();
    }

    public String getPassword() {
        return password;
    }

    public int getSync_time() {
        return sync_time;
    }

    public void addResource(Resource res){
        resources.add(res);
    }

    public void addKeepers(String keeper){
        keepers_ip.add(keeper);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setSync_time(int sync_time) {
        this.sync_time = sync_time;
    }

    public void setSyn(boolean syn) {
        isSyn = syn;
    }

    public boolean removeKeeper(String ip){
        return keepers_ip.remove(ip);
    }

    public boolean hasResource(String fname){
        for(Resource res : resources)
            if(res.getName().equals(fname))
                return true;
        return false;
    }

    public void addResource(String path)   {



        Path _path = Paths.get(path);
        String fname = new File(path).getName();

        if(hasResource(fname)){
            System.out.println("[SAFEZONE"+getID()+"] Resource already exists");
            return ;
        }

        try {
            Files.copy(Paths.get(path) , Paths.get(getFolderPath()+"\\"+fname) );
        } catch (IOException e) {
            System.out.println("[Safezone "+getID()+"] A Resource with the same name already exists");
            e.printStackTrace();
        }


        Resource res = new Resource(fname);
        resources.add(res);
        res.addLog(new BaseLog(new Date(), new String(Courier.PSPacket.RFA),NetworkManger.getMyIP()) );
        update_log_file();

        try {
            update_safezone_file();
        } catch (IOException e) {
            System.out.println("[Safezone "+getID()+"] FAILED TO UPDATE SAFEZONE FILE");
            e.printStackTrace();
        }
        update_resource(res);
    }

}


