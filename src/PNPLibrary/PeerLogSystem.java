package PNPLibrary;

import java.io.*;

public class PeerLogSystem {
    /*PEER ACTIVITY*/
    private final static String LOG_FILE = "pacti.log";
    private final static String TMP_FILE = "log";
    private static BufferedWriter bw = null;
    private static RandomAccessFile file;

    static {
        try {
            file = new RandomAccessFile(TMP_FILE,"rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static {
        try {
            bw = new BufferedWriter(new FileWriter(new File(LOG_FILE)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static void write(String log){
        try {
            file.write( (log+"\n").getBytes());
            file.seek(0);

            bw.write(log);
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void writeln(String log){
        write(log+"\n");
    }

    static void  close(){
        try {
            file.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
