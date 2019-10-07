package test;

import PNPLibrary.NetworkManger;

import java.util.Scanner;


/*the only tracker for now*/

public class Main_2 {
    public static void main(String[] args) {
        String ip = "";
        /*
        Scanner scanner = new Scanner(System.in);

        System.out.print("Insert root path of folder 'safezones' of this process:");
        String root = scanner.nextLine();
        */

        String root = "P2";

        //System.out.print("Insert the ip of the peer in localhost format (127.0.0.x)[x: 1-254]:");
        //ip = scanner.nextLine();

        ip = "127.0.0.2";

        NetworkManger.init(true,ip,true);
        NetworkManger.setSafezonesListPathRoot(root);
        NetworkManger.setSafezoneManagerFolderPathRoot(root);
        NetworkManger manager =NetworkManger.manager();

        //manager.shut_down();
    }


}
