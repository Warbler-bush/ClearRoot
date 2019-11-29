package PNPLibrary;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class PeerLogSystem {
    /*PEER ACTIVITY*/
    private final static String LOG_FILE = "pacti.log";

    public static void write(String log){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE));
            bw.append("["+new Date().toString() +"]"+log);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeln(String log){
        write(log+"\n");
    }



}
