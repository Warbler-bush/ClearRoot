package PNPLibrary;

import java.io.IOException;

public class SwarnJoinExitTest {

    @org.junit.Test
    public void join_exit_swarn() {
        try {
            NetworkManger networkManger = new NetworkManger(false);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}