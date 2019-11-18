package PNPLibrary;

/*------------------------------------------------------------------------------------*/
/* simple client and server socket                                                    */
/*------------------------------------------------------------------------------------*/



import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

class ClientSocket_n  {

    private Socket socket ;
    private DataOutputStream dOut ;
    private DataInputStream dIn;

    public ClientSocket_n(Socket sock) throws IOException {
        this.socket = sock;
        dOut = new DataOutputStream(socket.getOutputStream());
        dIn = new DataInputStream(socket.getInputStream());
    }

    public ClientSocket_n(){ }

    public void connect(String ip, int port) throws IOException{
        socket = new Socket(ip, port);
        //UPnP.openPortTCP(port);

        dOut = new DataOutputStream(socket.getOutputStream());
        dIn = new DataInputStream(socket.getInputStream());
    }

    public void send(byte[] msg) throws IOException {
        dOut.writeInt(msg.length); // write length of the message
        dOut.write(msg);           // write the message
    }

    public byte[] receive() throws IOException {

        int length = dIn.readInt();                    // read length of incoming message
        if(length > 0) {
            byte[] message = new byte[length];
            dIn.readFully(message, 0, message.length); // read the message
            return message;
        }
        return null;
    }

    public void disconnect() throws IOException {
        if(dOut != null)
            dOut.close();
        if(dIn != null)
            dIn.close();
        socket.close();
    }

    public void shut_down(){
        socket = null;
    }

    public void sendFile(String file_path) throws IOException {
        FileInputStream in = new FileInputStream(file_path);
        int count;
        byte[] buffer = new byte[8192]; // or 4096, or more
        while ((count = in.read(buffer)) > 0)
        {
            dOut.write(buffer, 0, count);
        }
        in.close();
    }
    /* return the host address*/
    public String getHostAddress(){
        return socket.getRemoteSocketAddress().toString();
    }

    public String getHostIp(){return socket.getRemoteSocketAddress().toString().split(":")[0].substring(1);}
}

