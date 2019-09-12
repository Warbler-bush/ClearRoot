package PNPLibrary;

import javafx.util.Pair;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private void check_if_resources_were_remotely_modified(){
            try{
                courier.connect(search_a_keeper(),Courier.PORT);

                String log_path = getFolderPath() + "\\"+safezone_id+"_log.rs";
                byte[] log_file = courier.file_request(safezone_id, password, log_path);


                BufferedReader br = new BufferedReader(new StringReader(new String(log_file)));
                add_log(br,false);
                br.close();

                courier.disconnect();
            }catch (IOException | ParseException e) {
                System.out.println("[SAFEZONE] None of the keepers are reachable");
            }

    }

    /*actually it read the whole log_file  and check if the resources were modified*/
    public void check_if_resources_were_localy_modified(){
        String log_file_path = SafezoneManager.SAFEZONES_FOLDER_PATH+ getID()+"\\"+getID()+"_log.rs";

        try(BufferedReader br = new BufferedReader(new FileReader(log_file_path))){
            add_log(br,true);
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


        boolean met_syn = false;
        /*when it meets the SYN, it means that the rest of message were already read by the peer and updated*/
        while( (line = br.readLine() ) != null){

            if( line.charAt(0) == '#'){
                //in log file the resources start to count with 1

                res = getResources( Integer.parseInt(line.substring(1))-1 );
                met_syn = false;
                continue;
            }

            String[] fields = line.split(" ");

            if(fields[1].equals("SYN"))
                met_syn = true;

            if(met_syn)
                continue;

            // the moment in which the change request or report is arrived
            Date date = new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss").parse(fields[0]);

            //type of modification
            String type_modif =fields[1];
            if(type_modif.equals("RFU") ){
                met_syn = true;
                if(is_local_file) {
                    res.setLocal_modified(true);
                    System.out.println("[SAFEFZONE N^"+getID()+"][RES:"+res.getName()+"]"+"is localy modified the day"+fields[0]);
                }
                else {
                    res.setRemote_modified(true);
                    System.out.println("[SAFEFZONE N^"+getID()+"][RES:"+res.getName()+"]"+"is remotely modified the day"+fields[0]);
                }

            }


            res.addLog( new Pair<>(date,type_modif));
        }
    }

    public void syn() throws IOException {
        System.out.println("[SAFEZONE "+getID()+"]"  +" start syn...");

        check_if_resources_were_remotely_modified();

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
        byte[] file = courier.file_request(safezone_id,password,res.getName());
        overwrite_file(res.getName(),file);
    }


    /*Replaces a file with a new data file*/
    public void overwrite_file(String file_name, byte[] file_data) throws IOException {
        FileOutputStream fout = new FileOutputStream(getFolderPath()+ file_name,false);
        fout.write(file_data);
        fout.close();
    }

    private void create_local_copy(Resource res) throws IOException {
        Path copied = Paths.get(getFolderPath()+"\\local_"+res.getName());
        Path originalPath = Paths.get( getFolderPath()+"\\"+res.getName());
        Files.copy(originalPath, copied);
    }

    private void update_resource(Resource res) {
        for (String keeper_ip: keepers_ip ) {
            try {
                courier.connect(keeper_ip,Courier.PORT);
                courier.report_file_update(safezone_id, password, res.getName());
                courier.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
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

    private Resource getResourceByName(String name){
        for(int i = 0; i< resources.size(); i++)
            if(resources.get(i).getName().equals(name))
                return resources.get(i);
        return null;
    }

    private Resource getResourceByIndex(int idx ){
        return resources.get(idx);
    }

    private String getFolderPath(){
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

    private ArrayList<Pair<Date,String>> log_file;

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

    public void addLog(Pair<Date,String> log){

        if(log.getValue().equals("RFU"))
            last_update =  (Date) log.getKey().clone();

        log_file.add(log);
    }

    private Resource(String name , String owner_ip, int syn_type){
        this.name = name;
        this.owner_ip = owner_ip;
        this.syn_type = syn_type;
        this.log_file = new ArrayList<>();
    }

    public Resource(String name) throws UnknownHostException {
        this.name = name;
        // the new date has the time and date of the moment of creation.
        last_update =  new Date();
        owner_ip = InetAddress.getLocalHost().getHostAddress();
        syn_type = 1;
    }

    public static Resource importResource(String name , String owner_ip, int syn_type){
        return new Resource(name,owner_ip,syn_type);
    }

    public void setRemote_modified(boolean remote_modified) {
        this.remote_modified = remote_modified;
    }

}