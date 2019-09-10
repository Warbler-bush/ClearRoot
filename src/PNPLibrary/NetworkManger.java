package PNPLibrary;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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


    /* JOINING THE SWARN and if it's a tracker, starts the Thread of Tracker*/
    /*Searching a tracker to join its swarn*/
    public NetworkManger(boolean isTracker) throws IOException {

        /* START THE TRACKER THREAD*/
        try {
            if(isTracker);
               new Thread(new Tracker()).start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*GETTING THE TRACKER AND LOADING THE ID OF THE SAFEZONES*/
        courier = new Courier();

        loadSREPE();
        System.out.println("Trackers:");
        System.out.println(trackers.toString());


        idxMyTracker = newTracker();
        System.out.println("myTracker:"+getMyTracker());

        /*JOINING THE SWARN*/
        courier.join_swarn(getMyTracker());
        /*LOADING THE SAFEZONES INFORMATION AND SYNCHRONIZE THE FILES OF SAFEZONES WITH OTHER PEERS*/
        safezoneManager.init_safezones();

    }

    private String getMyTracker(){ return trackers.get(idxMyTracker);}
    /* Choose randomly a tracker from the list*/
    private int newTracker(){
        return new Random().nextInt()%trackers.size();
    }

    /* Opens the "srepe.txt" file and read the trackers on the list*/
    private void loadSREPE() {
        trackers = new ArrayList<>();
        safezoneManager = new SafezoneManager();

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
    public static int PORT = 10121;

    public void start() throws IOException {

        /*backlog is the same argument of listen() in the berkley socket*/
        serverSocket =  new ServerSocket(PORT);;
        System.out.println("my ip and port are:"+ serverSocket.getLocalSocketAddress().toString());
        while (true)
            new  ClientHandler(serverSocket.accept(),this).start();
    }



    public Tracker() {
        swarn = synchronizedList(new ArrayList<>()) ;
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
                    System.out.println("peer " +peer +" has joined the swarn");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }



}
