package PNPLibrary;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkManger {

    /*-----------------------------------*/
    /*GENERAL UTILITY CONSTANTS          */
    /*-----------------------------------*/

    private final static String TrackerFile = "Resources\\srepe.lst";
    static String SafezonesFile = "safezones.lst";
    final static String LOCALHOST = "127.0.0.1";
    final static String HOST = "0.0.0.0";
    private static String myIP = null;



    private  static boolean hasConnectivity = false;

    /*connection test*/
    private static final String DNS_HOST = "8.8.8.8";

    /*connection test in localhost*/
    private static final String GOD_TRACKER_IP = "127.0.0.1";

    private static  boolean LO = false;
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

    private Timer timer = null;

    /*NENTWORK MANAGER*/
    private static NetworkManger manager = null;



    /*----------------------------------*/
    /*NETWORK METHODS                   */
    /*----------------------------------*/


    public static void init(boolean isTracker){
        try {
            init(isTracker,InetAddress.getLocalHost().getHostAddress(),false);
        } catch (UnknownHostException e) {
            PeerLogSystem.writeln("WTF, YOU DONT KNOW YOURSELF IP?");
        }
    }


    public static void init(){
        init(false);
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

    public static void init(boolean isTracker, boolean isLO){
        try {
            init(isTracker,InetAddress.getLocalHost().getHostAddress(),isLO);
        } catch (UnknownHostException e) {
            PeerLogSystem.writeln("WTF, YOU DONT KNOW YOURSELF IP?");
        }
    }


    /*Timer manager methods*/

    private void startTimer(){
        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                try {

                    InetAddress addr =  InetAddress.getByName("8.8.8.8");


                    boolean hasCon =  addr.isReachable(500);
                    if(!hasConnectivity && hasCon)
                        NetworkManger.manager().syn();

                    hasConnectivity = hasCon;

                } catch (Exception ignored) {
                }
            }
        };

        timer = new Timer("Connectivity check");

        int delay  = 1000;
        int period = 2000;
        timer.scheduleAtFixedRate(repeatedTask, delay, period);
    }

    private void endTimer(){
        timer.cancel();
    }


    /*before calling the manager the client of this library should first call init() */
    public static NetworkManger manager(){
        if(manager == null) {
            try {
                manager = new NetworkManger(isTracker, myIP);
                manager.syn();
            }catch (IOException e){
                e.printStackTrace();
                PeerLogSystem.writeln("[NETWORK MANAGER] Error init");
            }
        }

        return manager;
    }

    private synchronized static void setConnectivity(boolean hasConnectivity){
        NetworkManger.hasConnectivity = hasConnectivity;
    }

    public synchronized  static boolean getConnectivity(){
        return hasConnectivity;
    }

    /* JOINING THE SWARN and if it's a tracker, starts the Thread of Tracker*/
    /*Searching a tracker to join its swarn*/
    /*the ip attribute is used for debugging*/
    private NetworkManger(boolean isTracker,String ip) throws IOException {

        startTimer();

        safezoneManager = SafezoneManager.Manager();

        PeerLogSystem.write("[NETWORK MANAGER] starting the tracker...");
        /* START THE TRACKER THREAD*/
        try {
            if(isTracker) {
                tracker = new  Tracker(ip);
                new Thread(tracker,"TRACKER").start();
            }
        } catch (Exception e) {
            PeerLogSystem.writeln("ERROR STARTING THE TRACKER");
            e.printStackTrace();
        }
       PeerLogSystem.writeln("DONE");


        PeerLogSystem.write("[NETWORK MANAGER] starting the peer server "+ip+":"+Courier.PORT+"...");
        /* starting the peer receive canal*/
        server = new ServerSocket_n(ip, Courier.PORT);
        server.start();
        PeerLogSystem.writeln("DONE");


        /*GETTING THE TRACKER AND LOADING THE ID OF THE SAFEZONES*/


        PeerLogSystem.write("[NETWORK MANAGER]loading the srepe...");
        loadSREPE();
        PeerLogSystem.writeln("DONE");
        
        PeerLogSystem.writeln("[NETWORK MANGER] Trackers:"+trackers.toString());


        idxMyTracker = newTracker();
        PeerLogSystem.writeln("[NETWORK MANAGER]myTracker:"+getMyTracker());

        /*JOINING THE SWARN */
        /*------------------------------------------------*/
        /*COMMUNICATION ATTRIBUTES                        */
        /*------------------------------------------------*/
        Courier courier = CourierManager.Manager().createCourier();
        courier.join_swarn(getMyTracker());
        courier.stopRunning();

        PeerLogSystem.write("[NETWORK MANAGER]loading the safezones...");
        loadSAFEZONES();
        PeerLogSystem.writeln("DONE");

        PeerLogSystem.writeln("[NETWORK MANGER] Safezones:"+safezoneManager.toString());




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
                PeerLogSystem.writeln("[NETWORK MANAGER] CREATION SAFEZONE LIST FAILED" );
                e.printStackTrace();
            }
        }

        try {
            BufferedReader br = new BufferedReader(
                    new  FileReader(SafezonesFile) );

            String line = "";
            char token = ' ';

            while( (line = br.readLine()) != null) {
                if(line.matches(" *") || line.isEmpty())
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
                if(line.matches(" *") || line.isEmpty())
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
            Courier courier = CourierManager.Manager().createCourier();
            courier.exit_swarn(getMyTracker());

            endTimer();

            CourierManager.Manager().disconnenct_all();
            server.stopRunning();
            tracker.stop();
        } catch (Exception e) {
            PeerLogSystem.writeln("[NETWORK MANAGER] TRACKER SUCCESSFULLY STOPPED");
        }

        PeerLogSystem.close();
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
            PeerLogSystem.writeln("[SAFEZONE] "+ peer_ip +" is unreachable");
        }
        return isOnline;
    }

    /*checks if the host has Internet, NOT TEST FOR LOOPBACK*/
    private boolean hasConnection(String ip){
        boolean ret = false;
        try {
            InetAddress addr = InetAddress.getByName(ip);
            ret = addr.isReachable(500);
        } catch (IOException e) {
            PeerLogSystem.writeln("Can't reach "+ip);
        }

        return ret;
    }

    public boolean hasConnection(){
        if(isLO())
            return hasConnection(GOD_TRACKER_IP);
        else return hasConnection(DNS_HOST);
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
            PeerLogSystem.writeln("[NETWORK MANAGER] Safezone creation failed");
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


