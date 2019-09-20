package PNPLibrary;

import javafx.util.Pair;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.synchronizedList;

/*------------------------------------------------------------------------------------*/
/* Tracker is a part of NetworkManger and only some of the peers are trackers, for now
 * the tracker only registers the peers in the swarn and nothing else.*/
/*------------------------------------------------------------------------------------*/
class Tracker implements Runnable{

    private ServerSocket serverSocket;
    private List<String> swarn;
    private String ip;


    public static int PORT = 10121;
    public static boolean STOP_TRACKER;

    public void start() throws IOException {
        /*backlog is the same argument of listen() in the berkley socket*/
        serverSocket =  new ServerSocket(PORT,50, InetAddress.getByName(ip));;
        while ( !STOP_TRACKER )
            new  ClientHandler(serverSocket.accept(),this).start();

    }


    public Tracker(String ip) {
        this.ip = ip;
        swarn = synchronizedList(new ArrayList<>()) ;
        System.out.println("[TRACKER] my ip and port are:"+ ip +":"+ Tracker.PORT);
        STOP_TRACKER = false;
    }

    public void stop() throws Exception {
        serverSocket.close();
    }
    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            System.out.println("[TRACKER] STOP ACCEPTING");
        }
        System.out.println("[TRACKER] SHUT DOWN");
    }



    /*------------------------------------------------------------------------------------*/
    /* HANDLER OF THE REQUESTS */
    /*------------------------------------------------------------------------------------*/
    /* Adds the peer to the swarn  and gives the permission to enter the peer to peer network*/
    private static class ClientHandler extends Thread{

        private TrackerCourier courier;
        private Tracker tracker;

        public ClientHandler(Socket socket, Tracker tracker) throws IOException {
            super("TRACKER HANDLER");
            this.courier = CourierManager.Manager().createTrackerCourier(socket);
            this.tracker = tracker;
        }

        /* When the peer receive a request*/
        public void run(){
            try {
                Pair<String,String> ret = courier.listen();
                String peer = ret.getValue();
                String rqst_type = ret.getKey();

                if(peer != null) {
                    if(rqst_type.equals(new String(Courier.PSPacket.SWJ))) {
                        tracker.swarn.add(peer);
                        System.out.println("[TRACKER] " + peer + " has joined the swarn");
                    }

                    if(rqst_type.equals(new String(Courier.PSPacket.SWE))) {
                        tracker.swarn.remove(peer);
                        System.out.println("[TRACKER] " + peer + " has exited the swarn");
                    }

                    if(rqst_type.equals(new String(Courier.PSPacket.FIN))) {
                        System.out.println("[TRACKER] " + peer + " second the connection");
                    }
                    if(rqst_type.equals(new String(Courier.PSPacket.FIN))) {
                        System.out.println("[TRACKER] " + peer + " first check the connection");
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("[CLIENT HANDLER] SHUT DOWN");


        }


    }



}