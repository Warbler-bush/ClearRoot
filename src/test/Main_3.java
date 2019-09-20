package test;

import PNPLibrary.NetworkManger;
import PNPLibrary.Safezone;

import java.io.IOException;
import java.util.Scanner;

public class Main_3 {

    public static void main(String[] args) {


        String ip = "";


        /*
        Scanner scanner = new Scanner(System.in);


        System.out.print("Insert root path of folder 'safezones' of this process:");
        String root = scanner.nextLine();
        */
        String root = "P3";

        /*
        System.out.print("Insert the ip of the peer in localhost format (127.0.0.x)[x: 1-254]:");
        ip = scanner.nextLine();
        */

        ip="127.0.0.3";

        NetworkManger.init(false, ip,true);
        NetworkManger.setSafezonesListPathRoot(root);
        NetworkManger.setSafezoneManagerFolderPathRoot(root);
        NetworkManger manager =NetworkManger.manager();

        int safezone_id = 2;
        String password = "42";

        Safezone sz  = null;

        sz = NetworkManger.manager().create_safezone(safezone_id,password);
        if(sz != null)
            sz.addResource("C:\\Users\\Wang Wei\\Desktop\\test_music.mp3");


        //manager.shut_down();
    }
}
