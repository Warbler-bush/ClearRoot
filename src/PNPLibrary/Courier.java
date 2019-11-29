package PNPLibrary;

import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

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

        //PeerLogSystem.writeln("[COURIER] SENDING CON...");
        client.connect( ip,port);
        client.send(PSPacket.CON_MSG_C().toBinary());
        PSPacket packet = PSPacket.toPacket(client.receive());

        if(!packet.isTypeEqual(PSPacket.RQA))
            throw new IOException();
        //PeerLogSystem.writeln("[COURIER] RQA ARRIVED");
    }

    public void connect(String ip) throws  IOException{
        connect(ip,Courier.PORT);
    }

    public byte[] file_request(int safezone_id,String pass,String filename) throws IOException {

        PeerLogSystem.writeln("[COURIER] SENDING FRQ "+ filename+" ...");
        client.send(PSPacket.FRQ_MSG_C(safezone_id, pass ,filename).toBinary() );
        PSPacket packet  = PSPacket.toPacket( client.receive() );

        if(!packet.isTypeEqual(PSPacket.FAW) ){
            throw new IOException();
        }

        PeerLogSystem.writeln("[COURIER] FAW ARRIVED");
        return packet.getData();
    }

    public void report_file_update(int safezone_id, String pass, String filename) throws IOException {
        PeerLogSystem.writeln("[COURIER] SENDING FILE UPDATE "+filename+" ...");
        Safezone safezone = SafezoneManager.Manager().getSafezoneById(safezone_id);
        byte[] file_data = file_to_binary(safezone.getFolderPath()+"\\"+  filename);

        PSPacket packet = PSPacket.RFU_MSG_C(safezone_id, pass, filename,file_data);

        packet.setFlags((byte) safezone.getResource(filename).getSyn_type());

        Resource res = safezone.getResource(filename);
        packet.setFlags((byte) res.getSyn_type());

        String fields = NetworkManger.getMyIP()+"&"+ Resource.DateToString(res.getLast_update());
        packet.setOptions(fields.getBytes());

        client.send(packet.toBinary());
        packet = PSPacket.toPacket(client.receive());
        if(!packet.isTypeEqual(PSPacket.OKS))
            throw new IOException();
    }

    public void report_file_add(int safezone_id, String pass, String filename) throws IOException {
        PeerLogSystem.writeln("[COURIER] SENDING FILE ADD "+filename+ " ...");
        Safezone safezone = SafezoneManager.Manager().getSafezoneById(safezone_id);
        byte[] file_data = file_to_binary(safezone.getFolderPath()+"\\"+  filename);

        PSPacket packet = PSPacket.RFA_MSG_C(safezone_id, pass, filename,file_data);

        Resource res = safezone.getResource(filename);
        packet.setFlags((byte) res.getSyn_type());

        String fields = NetworkManger.getMyIP()+"&"+ Resource.DateToString(res.getLast_update()) ;
        packet.setOptions(fields.getBytes());

        client.send(packet.toBinary());
        packet = PSPacket.toPacket(client.receive());
        if(!packet.isTypeEqual(PSPacket.OKS))
            throw new IOException();
    }

    public void report_file_remove(int safezone_id,String pass, String filename) throws IOException {
        PeerLogSystem.writeln("[COURIER] SENDING FILE REMOVE "+ filename +" ...");
        PSPacket packet = PSPacket.RFR_MSG_C(safezone_id,pass,filename);

        Safezone safezone = SafezoneManager.Manager().getSafezoneById(safezone_id);

        Resource res = safezone.getResource(filename);
        packet.setFlags((byte) res.getSyn_type());

        String fields = NetworkManger.getMyIP()+"&"+ Resource.DateToString(res.getLast_update()) ;
        packet.setOptions(fields.getBytes());

        client.send(packet.toBinary());
        packet =  PSPacket.toPacket( client.receive() );
        if(!packet.isTypeEqual(PSPacket.OKS) )
            throw new IOException();
    }

    public void report_sasfezone_join(int safezone_id,String pass) throws IOException {
        PSPacket packet = PSPacket.RSJ_MSG_C(safezone_id,pass);
        client.send(packet.toBinary());

        packet = PSPacket.toPacket(client.receive());
        if(!packet.isTypeEqual(PSPacket.OKS))
            throw  new IOException();
    }

    public void file_answer(int safezone_id, String pass, String filename, byte[] data) throws IOException{
        client.send(PSPacket.FAW_MSG_C(safezone_id, pass, filename, data).toBinary() );
    }

    private byte[] file_to_binary(String file_path) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(file_path));;
        return data;
    }

    public void listen_message() throws IOException {

        PeerLogSystem.writeln("[COURIER S.] CON REQUEST");
        PSPacket packet = PSPacket.toPacket( client.receive());
        if(packet.isTypeEqual(PSPacket.CON))
            client.send(PSPacket.RQA_MSG_C().toBinary());
        else{
            PeerLogSystem.writeln("[COURIER S.] the peer is not asking to connect");
        };

        while(!ServerSocket_n.STOP_SERVER) {
            packet = PSPacket.toPacket(client.receive());


            if(packet.isTypeEqual(PSPacket.FIN)){
                PeerLogSystem.writeln("[COURIER S.] "+client.getHostIp()+" DISCONNECT");
                client.disconnect();
                return;
            }

            if (packet.isTypeEqual(PSPacket.FRQ)) {
                PeerLogSystem.writeln("[COURIER S.]["+client.getHostIp()+ "]FILE REQUEST "+packet.getFilename());

                try{
                    Safezone sz = SafezoneManager.Manager().getSafezoneById(packet.getSafezone_id());

                    file_answer( packet.getSafezone_id(), new String(packet.getPassword()) , packet.getFilename(),
                        file_to_binary( sz.getFolderPath()+"\\"+ packet.getFilename()  ));
                }catch (FileNotFoundException e){
                    PeerLogSystem.writeln("[COURIER S.] safezone "+packet.getSafezone_id()+" hasn't a log file yet");

                }
            }

            if (packet.isTypeEqual(PSPacket.RFU)) {
                PeerLogSystem.writeln("[COURIER S.] FILE UPDATE "+packet.getFilename());
                Safezone safezone = SafezoneManager.Manager().getSafezoneById(packet.getSafezone_id());

                String path =safezone.getFolderPath()+"\\"+  packet.getFilename();
                PeerLogSystem.writeln("[COURIER S.] path:"+path);


                String options = new String( packet.getOptions());
                String[] fields = options.split("&");

                try {
                    safezone.getResource(packet.getFilename()).addLog(new BaseLog(Resource.StringToDate(fields[1]),new String(PSPacket.RFU),fields[0]) );
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                safezone.update_log_file();
                safezone.overwrite_file(path , packet.getData() );
                client.send(PSPacket.OKS_MSG_C().toBinary());
            }

            if(packet.isTypeEqual(PSPacket.RFA)){

                PeerLogSystem.writeln("[COURIER S.] FILE ADD"+packet.getFilename());
                Safezone safezone = SafezoneManager.Manager().getSafezoneById(packet.getSafezone_id());
                String options = new String( packet.getOptions());
                String[] fields = options.split("&");

                try {
                    safezone.addResource(Resource.importResource(packet.filename,fields[0],packet.getFlags(),Resource.StringToDate(fields[1])  ))
                            .addLog(new BaseLog(Resource.StringToDate(fields[1]) , new String(PSPacket.RFA), fields[0]));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                String path =safezone.getFolderPath()+"\\"+  packet.getFilename();
                PeerLogSystem.writeln("[COURIER S.] path:"+path);

                safezone.update_safezone_file();
                safezone.update_log_file();
                safezone.overwrite_file(path , packet.getData() );
                client.send(PSPacket.OKS_MSG_C().toBinary());
            }

            if(packet.isTypeEqual(PSPacket.RFR)){
                PeerLogSystem.writeln("[COURIER S.] REPORT FILE DELETE:"+packet.getFilename());
                Safezone safezone = SafezoneManager.Manager().getSafezoneById(packet.getSafezone_id());

                String options = new String( packet.getOptions());
                String[] fields = options.split("&");
                try {
                    safezone.getResource(packet.filename).addLog(new BaseLog(Resource.StringToDate(fields[1]) , new String(PSPacket.RFR), fields[0]) );
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                safezone.update_log_file();

                safezone.removeResource(packet.getFilename());
                client.send(PSPacket.OKS_MSG_C().toBinary());
            }

            if(packet.isTypeEqual(PSPacket.SZJ)){
                String asker_ip = new String(packet.getData());

                PeerLogSystem.writeln("[COURIER S.] REQUEST SAFEZONE JOIN");
                PeerLogSystem.writeln("[COURIER S.] "+asker_ip+" has joined the safezone");
                Safezone safezone = SafezoneManager.Manager().getSafezoneById(packet.getSafezone_id());

                accept_safezone_join(safezone.getID(),safezone.getPassword(),safezone.get_info_file());

                safezone.update_safezone_file();
            }

            if(packet.isTypeEqual(PSPacket.RSJ)){
                String asker_ip = new String(packet.getOptions());
                Safezone safezone = SafezoneManager.Manager().getSafezoneById(packet.getSafezone_id());
                safezone.addKeeper(asker_ip);

                client.send(PSPacket.OKS_MSG_C().toBinary());
            }

            if(packet.isTypeEqual(PSPacket.SZE)){
                String asker_ip = new String(packet.getData());
                PeerLogSystem.writeln("[COURIER S.] " + asker_ip +" REQUEST SAFEZONE EXIT");
                SafezoneManager.Manager().getSafezoneById(packet.getSafezone_id()).removeKeeper(asker_ip  );
                client.send(PSPacket.OKS_MSG_C().toBinary());
            }



        }


    }


    /*STill uncomplete, all keepers need to be online for receive all the OK.*/
    public void leave_safezone(int safezone_id, String password) throws IOException {
        client.send(PSPacket.SZE_MSG_C(safezone_id,password).toBinary());
        PSPacket packet = PSPacket.toPacket(client.receive());

        if (!packet.isTypeEqual(PSPacket.OKS))
            throw new IOException("[Courier] Exit from safezone unsuccessfull");

    }

    public byte[] safezone_join(int safezone_id, String pass) throws IOException {
        client.send(PSPacket.SZJ_MSG_C(safezone_id,pass ).toBinary());
        PSPacket packet = PSPacket.toPacket(client.receive()) ;
        if(!packet.isTypeEqual(PSPacket.RQA))
            throw new IOException("Request denied");

        packet= PSPacket.toPacket(client.receive()) ;
        if(!packet.isTypeEqual(PSPacket.FAW))
            throw new IOException("Keeper didn't send the info file");

        return packet.getData();
    }

    public void accept_safezone_join(int safezone_id, String pass,byte[] safezone_file) throws IOException {
       client.send(PSPacket.RQA_MSG_C().toBinary());

       Safezone safezone = SafezoneManager.Manager().getSafezoneById(safezone_id);
       PSPacket packet1 = PSPacket.FAW_MSG_C( safezone_id,pass,safezone.getID()+".sz",safezone_file);
       client.send(packet1.toBinary());
    }



    public String getHostIp(){
        return client.getHostIp();
    }

    public void disconnect() throws IOException {
        PeerLogSystem.writeln("[COURIER] SEND FIN");
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
        PeerLogSystem.writeln("[COURIER]Connected to the swarn");

    }

    public void exit_swarn(String ip_peer) throws Exception {
        client.connect(ip_peer,Tracker.PORT);
        client.send(PSPacket.SWE_MSG_C().toBinary());

        PSPacket packet = PSPacket.toPacket( client.receive() );
        if(!packet.isTypeEqual(PSPacket.RQA))
            throw new IOException();

        client.disconnect();
        PeerLogSystem.writeln("[COURIER]Exited to the swarn");
    }


    public void stopRunning() throws IOException {
        client.shut_down();
    }



    protected static class  PSPacket {

        private static String CHARSET = "ISO-8859-1";
        private static int PASS_DIM = 32;

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

        private byte[] options;

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

        /* REPORT SAFEZONE REPORT */
        /* RSJ is used for notifying the other peers
         that you have joined the safezone*/
        protected static final char[] RSJ = "RSJ".toCharArray();


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
        protected static final char[] OKS = "OKS".toCharArray();

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
            this.options = new byte[0];
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
            this.options = new byte[0];
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

        private PSPacket setPassword(String pass){
            byte[] sha = new byte[PASS_DIM];
            try {
                sha = MessageDigest.getInstance("SHA-256").digest( pass.getBytes());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }


            /*contains the allocated size*/
            this.password = sha;
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
            if(data != null)
                this.data = data;
            else return null;

            return this;
        }

        private PSPacket setOptions(byte[] options){
            if(options != null)
                this.options = options;
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

        public byte[] getOptions(){return options;}
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
            /* id:int , safezone_id:int, type:char[3], password:byte[PASS_DIM](256), filename_length:int , filename:string, data:bytes[]+ flags:byte,
            *           options_length:int, options:byte[]*/
            int total_size = Integer.BYTES*4 + 3* Character.BYTES +
                    password.length + filename_length+ data.length+ Byte.BYTES +
                    options.length;

            ByteBuffer buffer = ByteBuffer.allocate(total_size);
            buffer.putInt(id);

            buffer.putChar(type[0]);
            buffer.putChar(type[1]);
            buffer.putChar(type[2]);

            buffer.putInt(safezone_id);
            buffer.put(flags);
            buffer.put(password);

            buffer.putInt(filename_length);
            buffer.put(filename.getBytes());

            buffer.putInt(options.length);
            buffer.put(options);
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
            packet.flags = buffer.get();
            buffer.get(packet.password, 0, packet.password.length);

            packet.filename_length = buffer.getInt();

            byte[] tmp = new byte[packet.filename_length];
            buffer.get(tmp,0,packet.filename_length);
            packet.filename = new String(tmp);


            int option_length = buffer.getInt();
            packet.options = new byte[option_length];
            buffer.get(packet.options,0,option_length);

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
        public static PSPacket OKS_MSG_C(){
            PSPacket packet = new PSPacket(OKS);
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
            packet.setPassword(password);
            packet.setData(NetworkManger.getMyIP().getBytes());
            return packet;
        }

        /* CREATING SAFEZONE EXIT MESSAGE*/
        public static PSPacket SZE_MSG_C(int safezone_id, String password){
            PSPacket packet = new PSPacket(SZE);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password);
            return packet;
        }


        /* REPORTING THE JOINING OF A SAFEZONE*/
        public static PSPacket RSJ_MSG_C(int safezone_id, String password){
            PSPacket packet = new PSPacket(RSJ);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password);
            packet.setOptions(NetworkManger.getMyIP().getBytes());
            return packet;
        }

        /* KICK OUT AN INACTIVE PEER*/
        public static PSPacket KOS_MSG_C(int safezone_id, String password, String ip_peer){
            PSPacket packet = new PSPacket(KOS);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password);
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
            packet.setPassword(password);
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);

            return packet;
        }

        /* CREATING FILE ANSWER MESSAGE*/
        public static PSPacket FAW_MSG_C(int safezone_id, String password,String file_name, byte[] data){
            PSPacket packet = new PSPacket(FAW);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password);
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);
            packet.data = data;

            return packet;
        }

        /* CREATING OWNERSHIP CHANGE MESSAGE*/
        public static PSPacket OWC_MSG_C(int safezone_id, String password, String file_name,byte[] new_owner){
            PSPacket packet = new PSPacket(OWC);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password);
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);
            packet.data = new_owner;

            return packet;
        }


        /* CREATING REPORT FILE UPDATE MESSAGE*/
        public static PSPacket RFU_MSG_C(int safezone_id, String password, String file_name, byte[] file){
            PSPacket packet = new PSPacket(RFU);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password);
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);
            packet.setData(file);

            return packet;
        }

        /* CREATING REPORT FILE ADD MESSAGE*/
        public static PSPacket RFA_MSG_C(int safezone_id, String password, String file_name, byte[] file){
            PSPacket packet = new PSPacket(RFA);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password);
            packet.setFilenameLength(file_name.length());
            packet.setFileName(file_name);
            packet.setData(file);
            return packet;
        }

        /* CREATING REPORT FILE REMOVE MESSAGE*/
        public static PSPacket RFR_MSG_C(int safezone_id, String password, String file_name){
            PSPacket packet = new PSPacket(RFR);
            packet.setSafezoneID(safezone_id);
            packet.setPassword(password);
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


    public Pair<String,String> listen() throws IOException{
        PSPacket packet = PSPacket.toPacket(client.receive());

        if(packet.isTypeEqual(PSPacket.SWJ) ) {
            client.send(PSPacket.RQA_MSG_C().toBinary());
            return new Pair<>(new String(PSPacket.SWJ), client.getHostAddress());
        }

        if(packet.isTypeEqual(PSPacket.SWE)){
            client.send(PSPacket.RQA_MSG_C().toBinary());
            return new Pair<>(new String(PSPacket.SWE), client.getHostAddress());
        }

        if(packet.isTypeEqual(PSPacket.CON)){
            PeerLogSystem.writeln("[COURIER S.] CONNECTED");
            client.send(PSPacket.RQA_MSG_C().toBinary());
            return new Pair<>(new String(PSPacket.CON),client.getHostAddress());
        }

        if(packet.isTypeEqual(PSPacket.FIN)){
            PeerLogSystem.writeln("[COURIER S.] DISCONNECT");
            client.disconnect();
            return new Pair<>(new String(PSPacket.FIN),client.getHostAddress());
        }

        // not sending request decline because it's a error of sender, it's not even a request.
        client.send(PSPacket.ERR_MSG_C().toBinary());
        return null;
    }

}



