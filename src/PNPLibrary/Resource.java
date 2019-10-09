package PNPLibrary;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

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

    private ArrayList<BaseLog> log_file_online;
    private ArrayList<BaseLog> log_file_local;


    /*constructors*/

    Resource(String name, String owner_ip, int syn_type){
        this.name = name;
        this.owner_ip = owner_ip;
        this.syn_type = syn_type;
        this.log_file_online = new ArrayList<>();
        this.log_file_local = new ArrayList<>();

        try {
            this.last_update =new Date();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Resource(String name) {
        this(name, NetworkManger.getMyIP(),1);
        last_update = new Date();
    }


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

    public void addLog(BaseLog log){
        if(NetworkManger.manager().hasConnection())
            addLog_online(log);
        else addLog_local(log);
    }

    void addLog_online(BaseLog log){
        log_file_online.add(log);
    }

    void addLog_local(BaseLog log){
        log_file_local.add(log);
    }


    public String getFullOnlineLog(){
        String ret = "";
        for(int i = 0; i< log_file_online.size()-1; i++) {
            BaseLog log = log_file_online.get(i);
            ret += log.toLog() + "\r\n";
        }
        if(log_file_online.size()>0) {
            BaseLog log = log_file_online.get(log_file_online.size() - 1);
            ret += log.toLog();
        }

        return ret;
    }

    public void clear_online_log(){
        log_file_online.clear();
    }

    public BaseLog get_online_log(int idx){
        return log_file_online.get(idx);
    }

    public BaseLog get_local_log(int idx){
        return log_file_local.get(idx);
    }

    public static Date StringToDate(String date) throws ParseException {
        return new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss").parse(date);
    }

    public static String DateToString(Date date){
        return new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss").format(date);
    }

    public static Resource importResource(String name , String owner_ip, int syn_type){
        return new Resource(name,owner_ip,syn_type);
    }

    public void setRemote_modified(boolean remote_modified) {
        this.remote_modified = remote_modified;
    }

    public void clear_local_log() {
        log_file_local.clear();
    }
}