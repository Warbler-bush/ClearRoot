package PNPLibrary;

import javax.sound.midi.Track;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.Collections.synchronizedList;

public class NetworkManger {

    private final static String TrackerFile = "srepe.lst";

    private ArrayList<String> trackers = null;
    private int idxMyTracker;
    private Courier courier;
    private SafezoneManager safezoneManager = null;
    private ServerSocket_n server;



    private static String myIP = null;
    private static boolean isTracker = false;
    private static NetworkManger manager = null;

    public static String getMyIP(){
        return NetworkManger.myIP;
    }

    public static Boolean isTracker(){
        return isTracker;
    }

    public static void init(boolean isTracker,String ip){
        NetworkManger.isTracker = isTracker;
        NetworkManger.myIP = ip;
    }

    public static NetworkManger manager(){
        if(manager == null) {
            try {
                manager = new NetworkManger(isTracker, myIP);
            }catch (IOException e){
                System.out.println("[NETWORK MANAGER] Error init");
            }
        }
        return manager;
    }



    /* JOINING THE SWARN and if it's a tracker, starts the Thread of Tracker*/
    /*Searching a tracker to join its swarn*/
    /*the ip attribute is used for debugging*/
    private NetworkManger(boolean isTracker,String ip) throws IOException {

        System.out.print("[NETWORK MANAGER] starting the tracker...");
        /* START THE TRACKER THREAD*/
        try {
            if(isTracker);
               new Thread(new Tracker(ip)).start();
        } catch (Exception e) {
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
        courier = new Courier();
        courier.join_swarn(getMyTracker());
        /*LOADING THE SAFEZONES INFORMATION*/
        safezoneManager.init_safezones();
        /* SYNCHRONIZE THE FILES OF SAFEZONES WITH OTHER PEERS */
        safezoneManager.syn();


    }

    public NetworkManger(boolean isTracker) throws  IOException{
        this(isTracker,"0.0.0.0");
    }




    private String getMyTracker(){ return trackers.get(idxMyTracker);}
    /* Choose randomly a tracker from the list*/
    private int newTracker(){
        return new Random().nextInt()%trackers.size();
    }
    /* Opens the "srepe.txt" file and read the trackers on the list*/
    private void loadSREPE() {
        trackers = new ArrayList<>();
        safezoneManager = SafezoneManager.Manager();

        try {
            BufferedReader br = new BufferedReader(
                    new  FileReader(this.getClass().getResource(TrackerFile).getFile()) );

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

                if(token == 'S')
                    safezoneManager.add(new Safezone(Integer.parseInt(line)));

            }

            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

/*------------------------------------------------------------------------------------*/
/* Tracker is a part of NetworkManger and only some of the peers are trackers, for now
 * the tracker only registers the peers in the swarn and nothing else.*/
/*------------------------------------------------------------------------------------*/
class Tracker implements Runnable{

    private ServerSocket serverSocket;
    private List<String> swarn;
    private String ip;
    public static int PORT = 10121;

    public void start() throws IOException {
        /*backlog is the same argument of listen() in the berkley socket*/
        serverSocket =  new ServerSocket(PORT,50, InetAddress.getByName(ip));;
        while (true)
            new  ClientHandler(serverSocket.accept(),this).start();
    }



    public Tracker(String ip) {
        this.ip = ip;
        swarn = synchronizedList(new ArrayList<>()) ;
        System.out.println("[TRACKER] my ip and port are:"+ ip +":"+ Tracker.PORT);
    }

    public void stop() throws Exception {
        serverSocket.close();
    }

    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /*------------------------------------------------------------------------------------*/
    /* HANDLER OF THE REQUESTS */
    /*------------------------------------------------------------------------------------*/
    /* Adds the peer to the swarn  and gives the permission to enter the peer to peer network*/
    private static class ClientHandler extends Thread{

        private TrackerCourier courier;
        private Tracker tracker;

        public ClientHandler(Socket socket, Tracker tracker) throws IOException {
            this.courier = new TrackerCourier(socket);
            this.tracker = tracker;
        }

        /* When the peer receive a request*/
        public void run(){
            try {
                String peer = courier.accept_swarn_join();
                if(peer != null) {
                    tracker.swarn.add(peer);
                    System.out.println("[TRACCKER] "+peer +" has joined the swarn");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }



}
