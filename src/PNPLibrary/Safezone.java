package PNPLibrary;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

public class Safezone {
    private int safezone_id;
    private char[] password ;

    private int sync_time ;

    private ArrayList<String> keepers_ip;
    private ArrayList<Resource> resources;

    public Safezone(int safezone_id){
        if(safezone_id > 0)
            this.safezone_id = safezone_id;
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

    public void setPassword(char[] password) {
        this.password = password;
    }

    public void setSync_time(int sync_time) {
        this.sync_time = sync_time;
    }


    public char[] getPassword() {
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

    /* syn_type: 1 = OVERWRITE/COPY  2 = RESTRICT */
    private int syn_type;

    /*GETTERS*/



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

    private Resource(String name, Date last_update, String owner_ip, int syn_type){
        this.name = name;
        this.last_update = last_update;
        this.owner_ip = owner_ip;
        this.syn_type = syn_type;
    }

    public Resource(String name) throws UnknownHostException {
        this.name = name;
        // the new date has the time and date of the moment of creation.
        last_update =  new Date();
        owner_ip = InetAddress.getLocalHost().getHostAddress();
        syn_type = 1;
    }

    public static Resource importResource(String name, Date last_update, String owner_ip, int syn_type){
        return new Resource(name,last_update,owner_ip,syn_type);
    }
}