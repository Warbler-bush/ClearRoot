package PNPLibrary;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/*------------------------------------------------------------------------------------*/
/* COURIER                                                                           */
/*------------------------------------------------------------------------------------*/


class Courier {
    protected ClientSocket_n client;
    // Peer port
    public static final int PORT = 8440;

    public Courier() {
        client = new ClientSocket_n();
    }

    public Courier(Socket sock) throws IOException {
        client = new ClientSocket_n(sock);
    }


    public void connect(String ip, int port) throws IOException {

        System.out.println("[COURIER] SENDING CON...");
        client.connect( ip,port);
        client.send(PSPacket.CON_MSG_C().toBinary());
        PSPacket packet = PSPacket.toPacket(client.receive());

        if(!packet.isTypeEqual(PSPacket.RQA))
            throw new IOException();
        System.out.println("[COURIER] RQA ARRIVED");
    }

    public byte[] file_request(int safezone_id,String pass,String filename) throws IOException {

        System.out.println("[COURIER] SENDING FRQ "+ filename+" ...");
        client.send(PSPacket.FRQ_MSG_C(safezone_id, pass ,filename).toBinary() );
        PSPacket packet  = PSPacket.toPacket( client.receive() );

        if(!packet.isTypeEqual(PSPacket.FAW) ){
            throw new IOException();
        }

        System.out.println("[COURIER] FAW ARRIVED");
        return packet.getData();
    }

    public void report_file_update(int safezone_id, String pass, String filename) throws IOException {
        System.out.println("[COURIER] SENDING FILE UPDATE "+filename+" ...");
        byte[] file_data = file_to_binary(filename);
        client.send(PSPacket.RFU_MSG_C(safezone_id, pass, filename,file_data).toBinary());
        PSPacket packet = PSPacket.toPacket(client.receive());
        if(!packet.isTypeEqual(PSPacket.OK))
            throw new IOException();
    }

    public void file_answer(int safezone_id, String pass, String filename, byte[] data) throws IOException{
        client.send(PSPacket.FAW_MSG_C(safezone_id, pass, filename, data).toBinary() );
    }

    private byte[] file_to_binary(String file_path) throws IOException {
        FileInputStream in = new FileInputStream(file_path);
        byte[] data = in.readAllBytes();
        in.close();
        return data;
    }




    public void disconnect() throws IOException {
        System.out.println("[COURIER] SEND FIN");
        client.send(PSPacket.FIN_MSG_C().toBinary());
        client.disconnect();
    }


    /* Asking the tracker if the peer can enter the p2p network,
    * after having the permission the peer start to listen to the 8440 port*/

    public void join_swarn(String ip_peer) throws IOException {

        client.connect(ip_peer, Tracker.PORT);
        client.send(PSPacket.SWJ_MSG_C().toBinary());
        PSPacket packet = PSPacket.toPacket( client.receive() );

        if(!packet.isTypeEqual(PSPacket.RQA))
            throw new IOException();

        client.disconnect();
        System.out.println("[COURIER]Connected to the swarn");

    }


    public void exit_swarn(String ip_peer) throws Exception {
        client.connect(ip_peer,Tracker.PORT);
        client.send(PSPacket.SWE_MSG_C().toBinary());
        client.disconnect();
    }

    public void listen_message() throws IOException {

        System.out.println("[COURIER S.] CON REQUEST");
        PSPacket packet = PSPacket.toPacket( client.receive());
        client.send(PSPacket.RQA_MSG_C().toBinary());

        while(true) {
            packet = PSPacket.toPacket(client.receive());

            if(packet.isTypeEqual(PSPacket.FIN)){
                System.out.println("[COURIER S.] DISCONNECT");
                client.disconnect();
                return;
            }


            if (packet.isTypeEqual(PSPacket.FRQ)) {
                System.out.println("[COURIER S.] FILE REQUEST "+packet.getFilename());

                String path = SafezoneManager.SAFEZONES_FOLDER_PATH+"\\"+packet.getSafezone_id();

                file_answer( packet.getSafezone_id(), new String(packet.getPassword()) , packet.getFilename(),
                        file_to_binary(path+"\\"+packet.getFilename()  ));

            }

            if (packet.isTypeEqual(PSPacket.RFU)) {
                System.out.print("[COURIER S.] FILE UPDATE "+packet.getFilename());
                Safezone safezone = SafezoneManager.Manager().getSafezoneById(packet.getSafezone_id());
                safezone.overwrite_file(packet.getFilename(), packet.getData());
                client.send(PSPacket.OK_MSG_C().toBinary());
            }
        }


    }

    protected static class  PSPacket {

        private static String CHARSET = "ISO-8859-1";
        private static int PASS_DIM = 256;

        /*--------------------------------------------------*/
        /* FIELDS OF PNPLibrary.PSPacket                               */
        /*--------------------------------------------------*/


        /* Unique counter that count the number of packet sent by this peer */
        private static int ID_COUNTER = 0;

        /* Identifier of the packet */
        private int id;

        /* type of packet*/
        private char[] type;

        /* identifier of the safezone */
        private int safezone_id;

        /* various flags */
        private byte flags;

        /* CURRENT VERSION DOESN'T IMPLEMENT IT*/
        /* it's memorized as a digest of sha3-256*/
        private byte[] password;

        /* filename length*/
        private int filename_length;
        /* used in the messages related to file management*/
        private String filename;

        /* general data for send files and other messages*/
        private byte[] data;


        /*--------------------------------------------------*/
        /* TYPE OF MESSAGES                                 */
        /*--------------------------------------------------*/

        /* CONNECTION REQUEST*/
        /* CON used for joining into the swarn */
        protected static final char[] CON = "CON".toCharArray();

        /* DISCONNECTION REQUEST*/
        /* FIN used for exiting onto the swarn */
        protected static final char[] FIN = "FIN".toCharArray();

        /*----------------------------------------------------*/

        /* JOIN SAFEZONE REQUEST*/
        /* SZJ used for joining a Safezone */
        protected static final char[] SZJ = "SZJ".toCharArray();

        /* EXIT SAFEZONE REQUEST*/
        /* SZE used for exiting a Safezone */
        protected static final char[] SZE = "SZE".toCharArray();

        /* KICK OUT SAFEZONE*/
        /* KOS used for kicking out an inactive peer */
        protected static final char[] KOS = "KOS".toCharArray();

        /*----------------------------------------------------*/

        /* SWARN JOIN REQUEST*/
        /* SWJ used for joining a swarn*/
        protected static final char[] SWJ = "SWJ".toCharArray();


        /* SWARN EXIT REQUEST*/
        /* SWE used for exiting a swarn*/
        protected static final char[] SWE = "SWE".toCharArray();

        /*----------------------------------------------------*/

        /* FILE REQUEST*/
        /* FRQ used for asking a file to a peer of safezone */
        protected static final char[] FRQ = "FRQ".toCharArray();

        /* FILE ANSWER */
        /* FAW answers to a request of a peer */
        protected static final char[] FAW = "FAW".toCharArray();

        /* OWNERSHIP CHANGE */
        /* OWC used for notifying the change of ownership of a file */
        protected static final char[] OWC = "OWC".toCharArray();

        /*----------------------------------------------------*/

        /*REPORT FILE UPDATE*/
        /*RFU used for notifying the update of a file*/
        protected static final char[] RFU = "RFU".toCharArray();

        /*REPORT FILE ADD*/
        /*RFA used for notifying the adding of a file*/
        protected static final char[] RFA = "RFA".toCharArray();

        /*REPORT FILE REMOVE*/
        /*RFA used for notifying the removing of a file*/
        protected static final char[] RFR = "RFR".toCharArray();

        /*----------------------------------------------------*/

        /*REQUEST ACCEPTED*/
        /*RQA used for general acceptance of a request*/
        protected static final char[] RQA = "RQA".toCharArray();

        /*REQUEST DECLINE*/
        /*RQA used for general declining of a request*/
        protected static final char[] RQD = "RQD".toCharArray();

        /*OK ALL GOOD*/
        /*OK*/
        protected static final char[] OK = "OK".toCharArray();

        /*ERROR*/
        /*ERR used for error occur when something went wrong*/
        protected static final char[] ERR = "ERR".toCharArray();


        /*-----------------------------------------------------*/
        /* FLAGS                                        */
        /*-----------------------------------------------------*/

        private static final byte NONE = 0b00000000;
        private static final byte SYN = 0b00000001;

        /*-----------------------------------------------------*/
        /* CONSTRUCTORS                                        */
        /*-----------------------------------------------------*/

        private PSPacket(char[] type){
            id = ID_COUNTER++;

            //clone creates a new Pointer and copy the elements of the old pointer, so if the
            //elements are ereferences it copies the references, if they are value it simply copies the value
            this.type = type.clone();
            this.safezone_id = 0;
            this.flags = NONE;
            //initialize all automatically with 0
            this.password = new byte[PASS_DIM];
            this.filename_length = 0;
            this.filename = "";
            this.data = new byte[0];
        }

        private PSPacket(){
            id = ID_COUNTER++;

            //clone creates a new Pointer and copy the elements of the old pointer, so if the
            //elements are ereferences it copies the references, if they are value it simply copies the value
            this.type = new char[3];
            this.safezone_id = 0;
            this.flags = NONE;
            //initialize all automatically with 0
            this.password = new byte[PASS_DIM];
            this.filename_length = 0;
            this.filename = "";
            this.data = new byte[0];

        }

        /*-----------------------------------------------------*/
        /* GETTERS AND SETTERS                                 */
        /*-----------------------------------------------------*/

        private PSPacket setSafezoneID(int safezone_id){
            if(safezone_id >= 0)
                this.safezone_id = safezone_id;
            else return null;

            return this;
        }

        private PSPacket setFlags(byte flags){
            this.flags = flags;
            return this;
        }

        private PSPacket setPassword(byte[] password){

            /*contains the allocated size*/
            if(password.length == PASS_DIM)
                this.password = password;
            else return null;

            return this;
        }

        private PSPacket setFilenameLength(int filename_length){
            if(filename_length >= 0)
                this.filename_length = filename_length;
            else return null;

            return this;
        }

        private PSPacket setFileName(String filename){
            if(filename != null)
                this.filename = filename;
            else return null;

            return this;
        }

        private PSPacket setData(byte[] data){
            if( data != null && data.length >= 0)
                this.data = data;
            else return null;

            return this;
        }


        public int getId() {
            return id;
        }

        public char[] getType() {
            return type;
        }

        public int getSafezone_id() {
            return safezone_id;
        }

        public byte getFlags() {
            return flags;
        }

        public byte[] getPassword() {
            return password;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] getData(){return data;}
        /*-----------------------------------------------------*/
        /* CONVERTS INTEGER TO BINARY AND VICE VERSA           */
        /*-----------------------------------------------------*/
        private static byte[] Int2Binary(int i){
            return String.valueOf(i).getBytes(Charset.forName(CHARSET));
        }
        private static int Binary2Int(byte[] bin){
            return Integer.valueOf( new String(bin) );
        }

        /*-----------------------------------------------------*/
        /* CONVERTS CHAR ARRY TO BINARY AND VICE VERSA         */
        /*-----------------------------------------------------*/
        private static byte[] charArray2Binary(char[] array){
            return new String(array).getBytes( Charset.forName(CHARSET) );
        }
        private static char[] Binary2CharArray(byte[] bin){
            return new String(bin).toCharArray();
        }



        /*--------------------------------------------------------------*/
        /*CONVERSION INTO BINARY INFORMATION FOR SENDING THROUGH SOCKET */
        /*--------------------------------------------------------------*/
        protected byte[] toBinary(){
            /* id:int , safezone_id:int, type:char[3], password:byte[PASS_DIM](256), filename_length:int , filename:string, data:bytes[] */
            int total_size = Integer.BYTES*3 + 3* Character.BYTES +
                    password.length + filename_length*Character.BYTES+ data.length;

            ByteBuffer buffer = ByteBuffer.allocate(total_size);
            buffer.putInt(id);

            buffer.putChar(type[0]);
            buffer.putChar(type[1]);
            buffer.putChar(type[2]);

            buffer.putInt(safezone_id);
            buffer.put(password);

            buffer.putInt(filename_length);
            buffer.put(filename.getBytes());
            buffer.put(data);

            return buffer.array();
        }

        /*------------------------------------------------------------------------*/
        /*READING THE DATA FROM THE BINARY INFORMATION RECEIVE THROUGH THE SOCKET */
        /*returns True if the format is wrong                                     */
        /*------------------------------------------------------------------------*/

        protected static PSPacket toPacket(byte[] binary){
            PSPacket packet = new PSPacket();

            ByteBuffer buffer = ByteBuffer.wrap(binary);
            packet.id = buffer.getInt();

            packet.type[0] = buffer.getChar();
            packet.type[1] = buffer.getChar();
            packet.type[2] = buffer.getChar();


            packet.safezone_id = buffer.getInt();
            buffer.get(packet.password, 0, PASS_DIM);
            packet.filename_length = buffer.getInt();

            byte[] tmp = new byte[packet.filename_length];
            buffer.get(tmp,0,packet.filename_length);
            packet.filename = new String(tmp);

            packet.data = new byte[buffer.remaining()];
            buffer.get(packet.data,0, buffer.remaining());
            return packet;
        }


        /*------------------------------------------------------------------------*/
        /*CREATING PACKET FOR THE PROTOCOL     _C stands for create               */
        /*------------------------------------------------------------------------*/


        /* CREATING CONNECTION MESSAGE  */
        public static PSPacket CON_MSG_C(){
            PSPacket packet = new PSPacket(CON);
            return packet;
        }


        /* DISCNNECTION MESSAGE MESSAGE CREATION*/
        public static PSPacket FIN_MSG_C(){
            PSPacket packet = new PSPacket(FIN);
            return packet;
        }

        /* REQUEST ACCEPTED MESSAGE CREATION*/
        public static PSPacket RQA_MSG_C(){
            PSPacket packet = new PSPacket(RQA);
            return packet;
        }

        /* OK ALL GOOD, AFFERMATIVE*/
        public static PSPacket OK_MSG_C(){
            PSPacket packet = new PSPacket(OK);
            return packet;
        }

        /* CREATING REQUEST DECLINED MESSAGE
        the future implementation may contain also the reason of rejection  */
        public static PSPacket RQD_MSG_C(/*String INFO*/){
            PSPacket packet = new PSPacket(RQD) ;
            return packet;
        }

        /* CREATING SAFEZONE JOIN MESSAGE*/
        public static PSPacket SZJ_MSG_C(int safezone_id,String password){
            PSPacket packet = new PSPacket(SZJ);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password.getBytes());
            return packet;
        }

        /* CREATING SAFEZONE EXIT MESSAGE*/
        public static PSPacket SZE_MSG_C(int safezone_id, String password){
            PSPacket packet = new PSPacket(SZE);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password.getBytes());
            return packet;
        }

        /* KICK OUT AN INACTIVE PEER*/
        public static PSPacket KOS_MSG_C(int safezone_id, String password, String ip_peer){
            PSPacket packet = new PSPacket(KOS);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password.getBytes());
            packet.setData(ip_peer.getBytes());
            return packet;
        }

        /* JOIN SWARN REQUEST TO A TRACKER*/
        public static PSPacket SWJ_MSG_C(){
            PSPacket packet = new PSPacket(SWJ);
            return packet;
        }

        /* EXIT SWARN REQUEST TO A TRACKER*/
        public static PSPacket SWE_MSG_C(){
            PSPacket packet = new PSPacket(SWE);
            return packet;
        }

        /* CREATING FILE REQUEST MESSAGE*/
        public static PSPacket FRQ_MSG_C(int safezone_id, String password,String file_name){
            PSPacket packet = new PSPacket(FRQ);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password.getBytes());
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);

            return packet;
        }

        /* CREATING FILE ANSWER MESSAGE*/
        public static PSPacket FAW_MSG_C(int safezone_id, String password,String file_name, byte[] data){
            PSPacket packet = new PSPacket(FAW);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password.getBytes());
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);
            packet.data = data;

            return packet;
        }

        /* CREATING OWNERSHIP CHANGE MESSAGE*/
        public static PSPacket OWC_MSG_C(int safezone_id, String password, String file_name,byte[] new_owner){
            PSPacket packet = new PSPacket(OWC);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password.getBytes());
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);
            packet.data = new_owner;

            return packet;
        }


        /* CREATING REPORT FILE UPDATE MESSAGE*/
        public static PSPacket RFU_MSG_C(int safezone_id, String password, String file_name, byte[] file){
            PSPacket packet = new PSPacket(RFU);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password.getBytes());
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);

            return packet;
        }

        /* CREATING REPORT FILE ADD MESSAGE*/
        public static PSPacket RFA_MSG_C(int safezone_id, String password, String file_name, byte[] file){
            PSPacket packet = new PSPacket(RFA);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password.getBytes());
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);

            return packet;
        }

        /* CREATING REPORT FILE REMOVE MESSAGE*/
        public static PSPacket RFR_MSG_C(int safezone_id, String password, String file_name, byte[] file){
            PSPacket packet = new PSPacket(RFR);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password.getBytes());
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);

            return packet;
        }



        /* CREATING ERROR MESSAGE*/
        /* EXIT SWARN REQUEST TO A TRACKER*/
        /* future implementation contains also the error info*/
        public static PSPacket ERR_MSG_C(){
            PSPacket packet = new PSPacket(ERR);
            return packet;
        }

        /* used for compare different types of packet*/
        public boolean isTypeEqual(char[] type){
            return this.type[0] == type[0] && this.type[1] == type[1] && this.type[2] == type[2];
        }
    }

}

/*------------------------------------------------------------------------------------*/
/* TRACKERCOURIER*/
/**/
/*------------------------------------------------------------------------------------*/

class TrackerCourier extends Courier{

    public TrackerCourier(Socket client) throws IOException{
        this.client = new ClientSocket_n(client);
    }

    /* return false if it's all ok*/
    public String accept_swarn_join() throws IOException {
        PSPacket packet = PSPacket.toPacket(client.receive());
        if(packet.isTypeEqual(PSPacket.SWJ) ) {
            client.send(PSPacket.RQA_MSG_C().toBinary());
            return client.getHostAddress();
        }

        // not sending request decline because it's a error of sender, it's not even a request.
        client.send(PSPacket.ERR_MSG_C().toBinary());
        return null;
    }

}

/*------------------------------------------------------------------------------------*/
/* simple client and server socket                                                    */
/*------------------------------------------------------------------------------------*/


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
        dOut.close();
        dIn.close();
        socket.close();
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

}

class ServerSocket_n extends Thread {
    private ServerSocket serverSocket;
    private String ip;
    private int port;

    public ServerSocket_n(String ip,int port){
        this.ip = ip;
        this.port = port;
    }

    public ServerSocket_n(boolean isLocalhost){
        if(isLocalhost)
            ip = "127.0.0.1";
        else ip = "0.0.0.0";

        port = Courier.PORT;
    }

    public ServerSocket_n(){
        /*this!!!!!!!! -> recalls the constructor with these parameters*/
        this(false);
    }



    public void startServer() throws IOException {

        /* https://stackoverflow.com/questions/14976867/how-can-i-bind-serversocket-to-specific-ip */
        /*backlog is the same argument of listen() in the berkley socket*/
        serverSocket =  new ServerSocket(port,50,InetAddress.getByName(ip) );

        while (true)
            new ClientHandler(serverSocket.accept()).start();

    }

    public void stopRunning() throws Exception {
        serverSocket.close();
    }

    @Override
    public void run(){
        try {
            startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class ClientHandler extends Thread{
        private Courier courier = null;

        public ClientHandler(Socket socket) throws IOException {
            courier = new Courier(socket);
        }

        /* When the peer receive a request*/
        public void run(){
            try {
                courier.listen_message();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}

