package PNPLibrary;

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
    private ArrayList<String> trackers = null;
    private int idxMyTracker;
    private Courier courier;


    /* JOINING THE SWARN and if it's a tracker, starts the Thread of Tracker*/
    /*Searching a tracker to join its swarn*/
    public NetworkManger(boolean isTracker) throws IOException {
        try {
            if(isTracker)
                new Tracker().start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        courier = new Courier();

        trackers = loadTrackers();
        System.out.println("Trackers:");
        System.out.println(trackers.toString());


        idxMyTracker = newTracker();
        System.out.println("myTracker:"+getMyTracker());


        courier.join_swarn(getMyTracker());
    }

    private String getMyTracker(){ return trackers.get(idxMyTracker);}
    /* Choose randomly a tracker from the list*/
    private int newTracker(){
        return new Random().nextInt()%trackers.size();
    }
    /* Opens the "srepe.txt" file and read the trackers on the list*/
    private ArrayList<String> loadTrackers() {
        ArrayList<String> ret = new ArrayList<>() ;

        try {
            BufferedReader br = new BufferedReader(
                    new  FileReader(this.getClass().getResource("srepe.lst").getFile()) );

            int cnt_trackers = Integer.parseInt(br.readLine());
            for(int i = 0; i< cnt_trackers; i++)
                ret.add(br.readLine());

            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static class Tracker  {

        private ServerSocket serverSocket;
        private List<String> swarn;
        public static int PORT = 10121;


        public void start( ) throws IOException {

            /*backlog is the same argument of listen() in the berkley socket*/
            serverSocket =  new ServerSocket(PORT,50, InetAddress.getByName("0.0.0.0"));;
            while (true)
                new  ClientHandler(serverSocket.accept(),this).start();
        }

        public Tracker() {
            swarn = synchronizedList(new ArrayList<>()) ;
        }

        public void stop() throws Exception {
            serverSocket.close();
        }


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
                    if(peer != null)
                        tracker.swarn.add(peer);


                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
