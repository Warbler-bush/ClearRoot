import PNPLibrary.NetworkManger;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            NetworkManger manager = new NetworkManger(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
