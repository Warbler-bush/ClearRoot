import PNPLibrary.NetworkManger;
import PNPLibrary.SafezoneManager;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String ip = "";
        Scanner scanner = new Scanner(System.in);
        System.out.print("Insert root path of folder 'safezones' of this process:");
        String root = scanner.nextLine();
        SafezoneManager.SAFEZONES_FOLDER_PATH = root +"\\"+SafezoneManager.SAFEZONES_FOLDER_PATH;

        System.out.print("Insert the ip of the peer in localhost format (127.0.0.x)[x: 1-254]:");
        ip = scanner.nextLine();

        NetworkManger.init(true,ip);
        NetworkManger manager =NetworkManger.manager();

        System.out.println("press a key plus enter to finish... ");
        String ret =  scanner.nextLine();
        return;
    }
}