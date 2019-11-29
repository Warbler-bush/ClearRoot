package PNPLibrary;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

class ServerSocket_n extends Thread {
    private ServerSocket serverSocket;
    private String ip;
    private int port;
    public static boolean STOP_SERVER;

    public ServerSocket_n(String ip,int port){
        super("SERVER SOCKET");
        this.ip = ip;
        this.port = port;
    }

    public ServerSocket_n(boolean isLocalhost){
        if(isLocalhost)
            ip = NetworkManger.LOCALHOST;
        else ip = NetworkManger.HOST;

        port = Courier.PORT;
        STOP_SERVER = false;
    }

    public ServerSocket_n(){
        /*this!!!!!!!! -> recalls the constructor with these parameters*/
        this(false);
    }



    public void startServer() throws IOException {

        /* https://stackoverflow.com/questions/14976867/how-can-i-bind-serversocket-to-specific-ip */
        /*backlog is the same argument of listen() in the berkley socket*/
        serverSocket =  new ServerSocket(port,50, InetAddress.getByName(ip) );
        

        while (!STOP_SERVER)
            new ClientHandler(serverSocket.accept()).start();

        PeerLogSystem.writeln("[SERVER] SHUT DOWN");
    }

    public void stopRunning() throws Exception {
        serverSocket.close();
    }

    @Override
    public void run(){
        try {
            startServer();
        } catch (IOException e) {
            PeerLogSystem.writeln("[SERVER] STOP ACCEPPTING CONNECTION");
        }

        PeerLogSystem.writeln("[SERVER] SHUT DOWN");
    }


    private static class ClientHandler extends Thread{
        private Courier courier = null;

        public ClientHandler(Socket socket) throws IOException {
            super("SERVERE CLIENT HANDLER");
            courier = CourierManager.Manager().createCourier(socket);
        }

        /* When the peer receive a request*/
        public void run(){
            try {
                courier.listen_message();
            } catch (IOException e) {
                e.printStackTrace();
                PeerLogSystem.writeln("[CLIENT HANDLER] "+courier.getHostIp()+" has disconnected");
            }
            PeerLogSystem.writeln("[CLIENT HANDLER] SHUT DOWN");
        }


    }
}