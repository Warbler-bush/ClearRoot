package PNPLibrary;

import java.io.*;
import java.util.ArrayList;

class SafezoneManager {

    /*DEFAULT FOLDER FOR VARIOUS SAFEZONES*/
    public static String SAFEZONES_FOLDER_PATH = "safezones\\";


    /*LIST OF SAFEZONES*/
    private ArrayList<Safezone> safezones;

    /*SAFEZONE MANAGER*/
    private static SafezoneManager manager = null;
    public static SafezoneManager Manager(){
        if(manager == null)
            manager = new SafezoneManager();

        return manager;
    }


    /*COSTRUCTOR*/
    private SafezoneManager(){
        safezones = new ArrayList<>();
    }

    public Safezone create_safezone(int safezone_id, String password, int sync_time) throws IOException {
        if(hasSafezone(safezone_id)){
            System.out.println("[SAFEZONE MANAGER] the safezone already exist");
            return null;
        }

        System.out.print("[SAFEZONE MANAGER] CREATING SAFEZONE "+safezone_id+" ...");
        Safezone sz = new Safezone(safezone_id,password,sync_time);
        safezones.add(sz);
        try {
            update_safezone_list(sz);
        } catch (IOException e) {
            System.out.println("[SAFEZONE MANAGER] update safezone list failed");
            throw new IOException();
        }

        try {
            create_safezone_file(sz);
        } catch (IOException e) {
            System.out.println("[SAFEZONE MANAGER] creation of safezone file failed");
            throw new IOException();
        }

        create_safezone_log_file(sz);
        System.out.println("DONE");
        return sz;
    }

    private void create_safezone_log_file(Safezone sz) {
        File file = new File(sz.getCloudLogPath());
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.printf("[SAFEZONE MANAGER] safezone "+sz.getID()+" 's file log hasn't been created successfully");
        }
    }

    private void update_safezone_list(Safezone sz) throws IOException {
        /*if it doesn't exist the network manager will create it in loadsafezones*/
        BufferedWriter bw = new BufferedWriter(new FileWriter(NetworkManger.SafezonesFile,true));
        bw.append("\r\n");
        bw.append(String.valueOf(sz.getID()));
        bw.close();
    }

    public void create_safezone_file(Safezone safezone) throws IOException {


        /*Creating the file and directory*/
        File file = new File( safezone.getFolderPath()+"\\"+safezone.getID()+".sz");

        if(!file.getParentFile().getParentFile().exists())
            file.getParentFile().getParentFile().mkdir();

        if(!file.getParentFile().exists())
            file.getParentFile().mkdir();

        file.createNewFile();

        safezone.update_safezone_file();

    }

    public boolean hasSafezone(int safezone_id){
        for(Safezone sz : safezones){
            if(sz.getID() == safezone_id)
                return true;
        }
        return false;
    }

    public Safezone join_safezone(int safezone_id , String password ,String peer_id) throws IOException {
        if(hasSafezone(safezone_id)) {
            System.out.println("[SAFEZONE MANAGER] safezone "+safezone_id +" already exists");
            return null;
        }


        Courier courier = CourierManager.Manager().createCourier();
        Safezone safezone  = new Safezone(safezone_id,password);
        safezones.add(safezone);

        courier.connect(peer_id);
        byte[] safezone_info_file = courier.safezone_join(safezone_id,password);
        courier.disconnect();


        safezone.load_info_file(safezone_info_file);
        System.out.println("[SAFEZONE MANAGER] safezone "+safezone.getID()+" joined succefully the safezone");


        create_safezone_file(safezone);
        create_safezone_log_file(safezone);


        update_safezone_list(safezone);
        safezone.report_safezone_join();

        for(int i = 0; i< safezone.getResourcesCount(); i++)
            safezone.getResource(i).setRemote_modified(true);

        safezone.syn();


        return safezone;
    }

    /* -------------------------------------------*/
    /* INITIALIZATION OF THE SAFEZONESE           */
    /* -------------------------------------------*/

    /* loading the basic data from the safezones                 load_safezones()*/
    /* loading the log files of the resources of every safezone  load_log_files()*/
    public void init_safezones() {
        load_safezones();
        load_log_files();
    }

    private void load_safezones(){
        for(int i = 0; i< safezones.size(); i++){
            Safezone safezone = safezones.get(i);
            safezone.load_info_file();
        }
    }

    private void load_log_files() {
        /*the earliest command is at the top*/
        for (int i = 0; i < safezones.size(); i++)
            safezones.get(i).load_local_log_file();

    }

    /* --------------------------------*/
    /* GETTERS AND SETTERS             */
    /* --------------------------------*/

    public void add(Safezone safezone) {
        safezones.add(safezone);
    }

    public Safezone getSafezoneById(int id){
        for(int i = 0; i < safezones.size(); i++)
            if(safezones.get(i).getID() == id)
                return safezones.get(i);
        return null;
    }

    public String toString(){
        String ret = "[";
        for(int i = 0; i< safezones.size()-1; i++){
            ret+= safezones.get(i).getID()+" ";
        }
        if(safezones.size()> 0)
            ret+=safezones.get(safezones.size()-1).getID();
        ret+="]";
        return ret;
    }
    /*--------------------------*/
    /*WRAPPERS                  */
    /*--------------------------*/

    /*SYNCHRONIZES ALL THE SAFEZONES*/
    public void syn() throws IOException {
        for(int i = 0; i< safezones.size(); i++)
            safezones.get(i).syn();

        update_log_files();
        System.out.println("[SAFEZONE MANAGER] SYN DONE");
    }

    public int szCount(){
        return safezones.size();
    }

    public Safezone getSafezone(int idx){
        if(idx >= safezones.size())
            return null;
        return safezones.get(idx);
    }

    /*UPDATES THE LOG FILES OF ALL THE SAFEZONES*/
    public void update_log_files() {
        for(Safezone s : safezones){
            s.update_log_file();
        }

    }

}
