package test;
import PNPLibrary.NetworkManger;
import PNPLibrary.Safezone;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Scanner;



public class CRConsole {
    private static char INDICATOR_DEFAULT =  '>';
    private static char INDICATOR_SAFEZONE =  '#';


    public static void main(String[] args) {

        String root = "DefaultUser";
        String cur_idc = ""+INDICATOR_DEFAULT;

        NetworkManger.init(false,true);
        NetworkManger.setSafezonesListPathRoot(root);
        NetworkManger.setSafezoneManagerFolderPathRoot(root);
        NetworkManger manager =NetworkManger.manager();


        Scanner scanner = new Scanner(System.in);

        String input = "";
        String cmd = "";

        Safezone sz = null;

        while(true){
            System.out.print(cur_idc);

            String fields[] = scanner.nextLine().split(" ");
            cmd = fields[0];

            /* safezone id, pass, access_peer*/
            if(cmd.equals("join")){
                if(fields.length != 4){
                    System.out.println("USAGE: join SAFEZONE_ID PASS ACCESS_PEER_IP");
                    continue;
                }
                int sz_id = Integer.parseInt(fields[1]) ;
                String pass = fields[2];
                String access_peer = fields[3];

                try {
                    sz = manager.join_safezone(sz_id,pass,access_peer);
                } catch (IOException e) {
                    System.out.println("UNENABLE TO JOIN THE SAFEZONE: CHECK THE PARAMS");
                }


                cur_idc = INDICATOR_SAFEZONE+""+sz_id+">";
                continue;
            }

            if(cmd.equals("mksz")){
                if(fields.length != 3 ){
                    System.out.println("USAGE: mksz SAFEZONE_ID PASS");
                    continue;
                }
                int sz_id = Integer.parseInt(fields[1]);
                String pass = fields[2];
                sz =  manager.create_safezone(sz_id,pass);

                cur_idc = INDICATOR_SAFEZONE+""+sz_id+">";
                continue;
            }

            /*select*/
            if(cmd.equals("slc")){
                if(fields.length!= 2){
                    System.out.println("USAGE: slc SAFEZONE_ID");
                    continue;
                }

                int sz_id = Integer.parseInt(fields[1]);
                sz = manager.getSafezoneById( sz_id);
                if(sz == null)
                    System.out.println("SAFEZONE ID DOES'T EXIST");
                else{
                    cur_idc = INDICATOR_SAFEZONE+""+sz_id+">";
                }
                continue;
            }



            if(cmd.equals("leave")){
                if(fields.length != 2){
                    System.out.println("USAGE: leave SASFEZONE_ID");
                    continue;
                }
                if(sz != null)sz.leave();
                else System.out.println("CHOOSE A SAFEZONE FIRST");
                continue;
            }

            if(cmd.equals("add")){
                if(fields.length != 2){
                    System.out.println("USAGE: add PATH_TO_THE_RESOURCE");
                    continue;
                }


                if(sz != null)sz.addResource(fields[1]);
                else System.out.println("CHOOSE A SAFEZONE FIRST");
                continue;
            }

            if(cmd.equals("rm")){
                if(fields.length != 2){
                    System.out.println("USAGE: rm RESOURCE_NAME");
                    continue;
                }


                if(sz != null) {
                    if(sz.removeResource(fields[1]))
                        System.out.println("RESOURCE NOT FOUND");

                }else System.out.println("CHOOSE A SAFEZONE FIRST");
                continue;
            }


            if(cmd.equals("ls")){
                if(fields.length != 1){
                    System.out.println("USAGE: ls");
                    continue;
                }

                for(int i = 0; i< manager.szCount(); i++)
                    System.out.println( (i+1)+")"+manager.getSafezone(i).getID() );

                continue;
            }


            if(cmd.equals("list")){
                if(fields.length != 1){
                    System.out.println("USAGE: list");
                    continue;
                }

                if(sz == null) {
                    System.out.println("CHOOSE A SAFEFZONE FIRST");
                    continue;
                }


                for(int i = 0; i< sz.getResourcesCount(); i++)
                    System.out.println( (i+1)+")"+sz.getResource(i).getName() );

                continue;
            }



            if(cmd.equals("exit")){
                if(sz != null){
                    sz = null;
                    cur_idc = INDICATOR_DEFAULT+"";
                    continue;
                }else break;
            }

            System.out.println("command not found!");

        }

        manager.shut_down();
    }
}
