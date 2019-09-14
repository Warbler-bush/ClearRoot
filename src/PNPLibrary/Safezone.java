package PNPLibrary;

import javafx.util.Pair;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class Safezone {
    private int safezone_id;
    private String password ;
    private int sync_time ;

    private ArrayList<String> keepers_ip;
    private ArrayList<Resource> resources;

    private Courier courier = null;
    public boolean isSyn = false;

    private String search_a_keeper() throws  IOException{
        String keeper = null;

        for(int i = 0; i< keepers_ip.size() && keeper == null ; i++){
            try{
                courier.connect(keepers_ip.get(i),Courier.PORT);
                courier.disconnect();
                keeper = keepers_ip.get(i);
             }catch (IOException e) {
                System.out.println("[SAFEZONE] "+keepers_ip.get(i)+" is unreachable");
            }
        }

        if(keeper == null)
            throw new IOException();

        return keeper;
    }

    private void load_online_log_file(){
            try{
                courier.connect(search_a_keeper(),Courier.PORT);

                String log_file_name = safezone_id+"_log.rs";
                byte[] log_file = courier.file_request(safezone_id, password, log_file_name);


                BufferedReader br = new BufferedReader(new StringReader(new String(log_file)));

                String line = "";
                Resource res = null;

                 while( (line = br.readLine() ) != null){

                    if( line.charAt(0) == '#'){
                        //in log file the resources start to count with 1
                        res = getResources( Integer.parseInt(line.substring(1))-1 );

                        continue;
                    }

                    String[] fields = line.split(" ");
                    // the moment in which the change request or report is arrived
                    Date date = new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss").parse(fields[0]);
                    //type of modification
                    String type_modif =fields[1];
                    String peer = fields[2];

                    if(type_modif.equals("RFU") && !peer.equals(NetworkManger.getMyIP())
                            && date.after(res.get_online_log(0).getKey() ) )
                        res.setRemote_modified(true);

                    }

                br.close();

                courier.disconnect();
            }catch (IOException | ParseException e) {
                System.out.println("[SAFEZONE] None of the keepers are reachable");
            }

    }

    /*actually it read the whole log_file  and check if the resources were modified*/
    public void load_local_log_file(){
        String log_file_path = SafezoneManager.SAFEZONES_FOLDER_PATH+ getID()+"\\"+getID()+"_log_local.rs";
        String log_file_path_cloud = SafezoneManager.SAFEZONES_FOLDER_PATH+ getID()+"\\"+getID()+"_log.rs";

        try{
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
    private void add_log(BufferedReader br,boolean is_local_file) throws IOException, ParseException {
        String line = "";
        Resource res = null;


        boolean goes_next_res = false;
        boolean first_update = true;
        /*when it meets the SYN, it means that the rest of message were already read by the peer and updated*/
        while( (line = br.readLine() ) != null){

            if( line.charAt(0) == '#'){
                //in log file the resources start to count with 1
                res = getResources( Integer.parseInt(line.substring(1))-1 );
                first_update = true;
                goes_next_res = false;
                continue;
            }

            String[] fields = line.split(" ");
            // the moment in which the change request or report is arrived
            Date date = new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss").parse(fields[0]);
            //type of modification
            String type_modif =fields[1];


            if(is_local_file && type_modif.equals("CHK") )
                goes_next_res = true;

            if(goes_next_res)
                continue;

            if(is_local_file) {
                if( first_update && type_modif.equals("RFU")){
                    res.setLocal_modified(true);
                    res.setLast_update(date);
                    first_update = false;
                }
                res.addLog_local(new Pair<>(date, type_modif));
            }else {
                if(first_update && type_modif.equals("RFU") ){
                    res.setLast_update(date);
                    first_update = false;
                }

                res.addLog_online(new Pair<>(date, type_modif));
            }
        }
    }

    public void syn() throws IOException {

        System.out.println("[SAFEZONE "+getID()+"]"  +" start syn...");

        load_online_log_file();

        for(int i = 0; i< resources.size(); i++){
            Resource res = resources.get(i);

            // SYNC TYPE = RESTRICTED
            if(res.getSyn_type() == Resource.RESTRICTED && res.isLocal_modified() ){
                if(  res.getOwner_ip().equals(InetAddress.getLocalHost().getAddress()) )
                    update_resource(res);
                else {
                    create_local_copy(res);
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
                }else if(res.isRemote_modified())
                        retrieve_new_version(res);
            }

        }
        System.out.println("SYN DONE");
    }

    private void retrieve_new_version(Resource res) throws IOException {
        String keeper = search_a_keeper();
        courier.connect(keeper,Courier.PORT);
        byte[] file = courier.file_request(safezone_id,password,res.getName());
        courier.disconnect();
        overwrite_file(res.getName(),file);

    }


    /*Replaces a file with a new data file*/
    public void overwrite_file(String file_name, byte[] file_data) throws IOException {
        FileOutputStream fout = new FileOutputStream(getFolderPath()+"\\"+ file_name,false);
        fout.write(file_data);
        fout.close();
    }

    private void create_local_copy(Resource res) throws IOException {
        Path copied = Paths.get(getFolderPath()+"\\local_"+res.getName());
        Path originalPath = Paths.get( getFolderPath()+"\\"+res.getName());
        Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
    }

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

    /*-------------*/
    /* COSTRUCTORS */
    /*-------------*/

    public Safezone(int safezone_id){
        if(safezone_id > 0)
            this.safezone_id = safezone_id;

        keepers_ip = new ArrayList<>();
        resources = new ArrayList<>();
        courier = new Courier();
    }


    /*------------------------------------*/
    /*ACCESSORS METHOD: GETTERS/ SETTERS */
    /*------------------------------------*/
    public boolean isSyn(){
        return isSyn;
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

    public String getFolderPath(){
        return SafezoneManager.SAFEZONES_FOLDER_PATH+"\\"+safezone_id;
    }

    public int getID() {
        return safezone_id;
    }

    public boolean isIDEqual(int id){
        return id == this.safezone_id;
    }

    public Resource getResources(int idx){
        return resources.get(idx);
    }

    public String getKeeper(int idx){
        return keepers_ip.get(idx);
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

    public String getPassword() {
        return password;
    }

    public int getSync_time() {
        return sync_time;
    }
}


class Resource{
    private String name;
    private Date last_update;
    private String owner_ip;

    private boolean local_modified = false;
    private boolean remote_modified = false;

    public static final int RESTRICTED = 1;
    public static final int SHARED = 2;

    /* syn_type: 1 = OVERWRITE/COPY  2 = RESTRICT */
    private int syn_type;

    private ArrayList<Pair<Date,String>> log_file_online;
    private ArrayList<Pair<Date,String>> log_file_local;

    /*GETTERS & SETTERS*/

    public int getSyn_type() {
        return syn_type;
    }

    public String getName() {
        return name;
    }

    public Date getLast_update() {
        return last_update;
    }

    public String getOwner_ip() {
        return owner_ip;
    }

    public boolean isLocal_modified(){
        return local_modified;
    }

    public boolean isRemote_modified(){
        return remote_modified;
    }

    public void setLast_update(Date last_update){
        this.last_update = last_update;
    }

    public void setLocal_modified(boolean local_modified){
        this.local_modified = local_modified;
    }

    public void addLog_online(Pair<Date,String> log){
        log_file_online.add(log);
    }

    public void addLog_local(Pair<Date,String> log){
        log_file_local.add(log);
    }

    private Resource(String name , String owner_ip, int syn_type){
        this.name = name;
        this.owner_ip = owner_ip;
        this.syn_type = syn_type;
        this.log_file_online = new ArrayList<>();
        this.log_file_local = new ArrayList<>();

        try {
            this.last_update = stringToDate("08/01/2000-08:00:00");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Resource(String name) throws UnknownHostException {
        try {

            this.name = name;
            // the new date has the time and date of the moment of creation.
            last_update = stringToDate("08/01/2000-08:00:00");
            owner_ip = InetAddress.getLocalHost().getHostAddress();
            syn_type = 1;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Pair<Date,String> get_online_log(int idx){
        return log_file_online.get(idx);
    }

    public Pair<Date,String> get_local_log(int idx){
        return log_file_local.get(idx);
    }

    public Date stringToDate(String date) throws ParseException {
        return new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss").parse(date);
    }

    public static Resource importResource(String name , String owner_ip, int syn_type){
        return new Resource(name,owner_ip,syn_type);
    }

    public void setRemote_modified(boolean remote_modified) {
        this.remote_modified = remote_modified;
    }

}