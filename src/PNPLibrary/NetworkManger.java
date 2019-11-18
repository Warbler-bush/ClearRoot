package PNPLibrary;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class NetworkManger {

    /*-----------------------------------*/
    /*GENERAL UTILITY CONSTANTS          */
    /*-----------------------------------*/

    private final static String TrackerFile = "Resources\\srepe.lst";
    public static String SafezonesFile = "safezones.lst";
    public final static String LOCALHOST = "127.0.0.1";
    public final static String HOST = "0.0.0.0";
    private static String myIP = null;


    /*connection test*/
    private static final String DNS_HOST = "8.8.8.8";
    private static final int DNS_PORT = 53;

    /*connection test in localhost*/
    private static final String GOD_TRACKER_IP = "127.0.0.2";
    private static final int GOD_TRACKER_PORT = Tracker.PORT;

    private static  boolean LO = false;
    /*------------------------------------------------*/
    /*COMMUNICATION ATTRIBUTES                        */
    /*------------------------------------------------*/
    private Courier courier;
    private ServerSocket_n server;

    /*------------------------------------------------*/
    /*     TRACKER  ATTRIBUTES                        */
    /*------------------------------------------------*/

    /*list of trackers*/
    private ArrayList<String> trackers = null;
    private static boolean isTracker = false;
    private int idxMyTracker;

    /*tracker server*/
    private Tracker tracker = null;

    /*SAFEZONE MANAGER*/
    private SafezoneManager safezoneManager = null;

    /*NENTWORK MANAGER*/
    private static NetworkManger manager = null;



    /*----------------------------------*/
    /*NETWORK METHODS                   */
    /*----------------------------------*/


    public static void init(){
            init(false);

    }

    public static void init(boolean isTracker){
        try {
            init(isTracker,InetAddress.getLocalHost().getHostAddress(),false);
        } catch (UnknownHostException e) {
            System.out.println("WTF, YOU DONT KNOW YOURSELF IP?");
        }
    }


    /*sets the "ip" and the flag "is Tracker" */
    public static void init(boolean isTracker,String ip,boolean isLO){
        NetworkManger.isTracker = isTracker;
        NetworkManger.myIP = ip;
        NetworkManger.LO = isLO;
    }

    public static void init(boolean isTracker,String ip){
        init(isTracker,ip,false);
    }

    /*before calling the manager the client of this library should first call init() */
    public static NetworkManger manager(){
        if(manager == null) {
            try {
                manager = new NetworkManger(isTracker, myIP);
                manager.syn();
            }catch (IOException e){
                e.printStackTrace();
                System.out.println("[NETWORK MANAGER] Error init");
            }
        }

        return manager;
    }

    /* JOINING THE SWARN and if it's a tracker, starts the Thread of Tracker*/
    /*Searching a tracker to join its swarn*/
    /*the ip attribute is used for debugging*/
    private NetworkManger(boolean isTracker,String ip) throws IOException {

        safezoneManager = SafezoneManager.Manager();

        System.out.print("[NETWORK MANAGER] starting the tracker...");
        /* START THE TRACKER THREAD*/
        try {
            if(isTracker) {
                tracker = new  Tracker(ip);
                new Thread(tracker,"TRACKER").start();
            }
        } catch (Exception e) {
            System.out.println("ERROR STARTING THE TRACKER");
            e.printStackTrace();
        }
        System.out.println("DONE");


        System.out.print("[NETWORK MANAGER] starting the peer server "+ip+":"+Courier.PORT+"...");
        /* starting the peer receive canal*/
        server = new ServerSocket_n(ip, Courier.PORT);
        server.start();
        System.out.println("DONE");




        /*GETTING THE TRACKER AND LOADING THE ID OF THE SAFEZONES*/


        System.out.print("[NETWORK MANAGER]loading the srepe...");
        loadSREPE();
        System.out.println("DONE");
        
        System.out.println("[NETWORK MANGER] Trackers:"+trackers.toString());


        idxMyTracker = newTracker();
        System.out.println("[NETWORK MANAGER]myTracker:"+getMyTracker());

        /*JOINING THE SWARN */
        courier = CourierManager.Manager().createCourier();
        courier.join_swarn(getMyTracker());
        courier.stopRunning();

        System.out.print("[NETWORK MANAGER]loading the safezones...");
        loadSAFEZONES();
        System.out.println("DONE");

        System.out.println("[NETWORK MANGER] Safezones:"+safezoneManager.toString());




        /*LOADING THE SAFEZONES INFORMATION*/
        safezoneManager.init_safezones();
        /* SYNCHRONIZE THE FILES OF SAFEZONES WITH OTHER PEERS */

    }

    public void syn(){
        try {
            safezoneManager.syn();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSAFEZONES() {
        safezoneManager = SafezoneManager.Manager();

        File file = new File(SafezonesFile);
        if(!file.exists()) {
            try {
                file.createNewFile();
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                bw.write("/safezones");
                bw.close();
            } catch (IOException e) {
                System.out.println("[NETWORK MANAGER] CREATION SAFEZONE LIST FAILED" );
                e.printStackTrace();
            }
        }

        try {
            BufferedReader br = new BufferedReader(
                    new  FileReader(SafezonesFile) );

            String line = "";
            char token = ' ';

            while( (line = br.readLine()) != null) {
                if(line.isBlank() || line.isEmpty())
                    continue;

                if(line.equals( "/trackers") ) {
                    token = 'T';
                    continue;
                }

                if(line.equals("/safezones") ) {
                    token = 'S';
                    continue;
                }

                if(token == 'T')
                    if(!line.equals(InetAddress.getLocalHost().getHostAddress()))
                        trackers.add(line);


                // the safezone password will be updated later in the reading of safezone_id.sz
                if(token == 'S')
                    safezoneManager.add(new Safezone(Integer.parseInt(line),""));

            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Opens the "srepe.txt" file and read the trackers on the list*/
    private synchronized void loadSREPE() {
        trackers = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(
                    new  FileReader( TrackerFile) );

            String line = "";
            char token = ' ';

            while( (line = br.readLine()) != null) {
                if(line.isBlank() || line.isEmpty())
                    continue;

                if(line.equals( "/trackers") ) {
                    token = 'T';
                    continue;
                }

                if(line.equals("/safezones") ) {
                    token = 'S';
                    continue;
                }

                if(token == 'T')
                    //if(!line.equals(InetAddress.getLocalHost().getHostAddress()))
                        trackers.add(line);


                // the safezone password will be updated later in the reading of safezone_id.sz
                if(token == 'S')
                    safezoneManager.add(new Safezone(Integer.parseInt(line),""));

            }

            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*It's used when you don't want a localhost PEER*/
    public NetworkManger(boolean isTracker) throws  IOException{
        this(isTracker,HOST);
    }

    /*stops all the SERVERS*/
    public void shut_down(){

        try {
            /*still need to fix*/
            Courier courier = CourierManager.Manager().createCourier();
            courier.exit_swarn(getMyTracker());

            CourierManager.Manager().disconnenct_all();
            server.stopRunning();
            tracker.stop();
        } catch (Exception e) {
            System.out.println("[NETWORK MANAGER] TRACKER SUCCESSFULLY STOPPED");
        }

        Tracker.STOP_TRACKER = true;
        ServerSocket_n.STOP_SERVER = true;
    }

    /*returns the ip of this host*/
    public static String getMyIP(){
        return NetworkManger.myIP;
    }

    /*check if a peer is online*/
    public static boolean isOnlinePeer(String peer_ip){
        Courier courier = CourierManager.Manager().createCourier();
        boolean isOnline = false;
        try{
            courier.connect(peer_ip);
            courier.disconnect();
            isOnline = true;
        }catch (IOException e) {
            System.out.println("[SAFEZONE] "+ peer_ip +" is unreachable");
        }
        return isOnline;
    }

    /*checks if the host has Internet, NOT TEST FOR LOOPBACK*/
    private boolean hasConnection(String ip,int port){
        Courier courier = CourierManager.Manager().createCourier();
        boolean ret = false;
        try {
            courier.connect(ip,port);
            courier.disconnect();
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean hasConnection(){
        if(isLO())
            return hasConnection(GOD_TRACKER_IP,GOD_TRACKER_PORT);
        else return hasConnection(DNS_HOST,DNS_PORT);
    }



    /*----------------------------------*/
    /*TRACKER METHODS                   */
    /*----------------------------------*/

    /*get the tracker that's bind with this host*/
    private String getMyTracker(){ return trackers.get(idxMyTracker);}

    /* Choose randomly a tracker from the list*/
    private int newTracker(){
        return new Random().nextInt()%trackers.size();
    }

    /*checks if this peer is also a tracker*/
    public static Boolean isTracker(){
        return isTracker;
    }


    /*--------------------------------------------*/
    /*SAFEZONE MANAGERS METHODS                   */
    /*--------------------------------------------*/

    /*the creation of safezone misses the check for unique safezone_id*/
    public Safezone create_safezone(int safezone_id, String password, int sync_time)   {
        try {
            return safezoneManager.create_safezone(safezone_id,password,sync_time);
        } catch (IOException e) {
            System.out.println("[NETWORK MANAGER] Safezone creation failed");
        }
        return null;
    }

    public Safezone create_safezone(int safezone_id, String password) {
        return create_safezone(safezone_id,password,1);
    }

    public Safezone join_safezone(int safezone_id , String password ,String peer_id) throws IOException {
        return safezoneManager.join_safezone(safezone_id,password,peer_id);
    }

    public static void setSafezoneManagerFolderPathRoot(String root){
        SafezoneManager.SAFEZONES_FOLDER_PATH = root +"\\"+SafezoneManager.SAFEZONES_FOLDER_PATH;
    }

    public static void setSafezonesListPathRoot(String root){
        SafezonesFile = root+"\\"+SafezonesFile;
    }

    public int szCount(){
        return SafezoneManager.Manager().szCount();
    }

    public Safezone getSafezone(int idx){
        return SafezoneManager.Manager().getSafezone(idx);
    }

    public Safezone getSafezoneById(int id){
        return SafezoneManager.Manager().getSafezoneById(id);
    }

    public boolean isLO() {
        return LO;
    }

}


