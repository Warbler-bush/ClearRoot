package test;

import PNPLibrary.NetworkManger;
import PNPLibrary.Safezone;

import java.io.IOException;
import java.util.Scanner;

public class Main_4 {

    public static void main(String[] args) {
        String ip = "";

        //Scanner scanner = new Scanner(System.in);

        final String test_res_path= "D:\\TDDOWNLOAD\\projects\\ClearSystem\\ClearRoot\\Resources\\blue.mp3";

        /*
        System.out.print("Insert root path of folder 'safezones' of this process:");
        String root = scanner.nextLine();
        */

        String root = "P4";

        /*
        System.out.print("Insert the ip of the peer in localhost format (127.0.0.x)[x: 1-254]:");
        ip = scanner.nextLine();
        */
        ip = "127.0.0.4";
        NetworkManger.init(false,ip,true);
        NetworkManger.setSafezonesListPathRoot(root);
        NetworkManger.setSafezoneManagerFolderPathRoot(root);
        NetworkManger manager =NetworkManger.manager();

        /*
        System.out.print("Insert the peer to join the safezone:");
        String peer_ip = scanner.nextLine();
        */

        String access_peer = "127.0.0.1";
        try {
            Safezone sz =  manager.join_safezone(2,"42", access_peer);
            sz.addResource(test_res_path);
        } catch (IOException e) {
            System.out.println("Error joining the safezone");
            e.printStackTrace();
        }



        //manager.shut_down();
    }
}
