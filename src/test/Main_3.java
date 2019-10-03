package test;

import PNPLibrary.NetworkManger;
import PNPLibrary.Safezone;

public class Main_3 {

    public static void main(String[] args) {
        final String test_resource_path = "D:\\TDDOWNLOAD\\projects\\ClearSystem\\ClearRoot\\Resources\\arctic_monkey_arabella.mp3";

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
            sz.addResource(test_resource_path);


        //manager.shut_down();
    }
}
