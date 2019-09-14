package PNPLibrary;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class SafezoneManager {
    private ArrayList<Safezone> safezones;
    public static String SAFEZONES_FOLDER_PATH = "safezones\\";
    private static SafezoneManager manager = null;

    public static SafezoneManager Manager(){
        if(manager == null)
            manager = new SafezoneManager();

        return manager;
    }

    private SafezoneManager(){
        safezones = new ArrayList<>();
    }

    public void add(Safezone safezone) {
        safezones.add(safezone);
    }

    public Safezone getSafezoneById(int id){
        for(int i = 0; i < safezones.size(); i++)
            if(safezones.get(i).getID() == id)
                return safezones.get(i);
        return null;
    }

    public void init_safezones() {
        /* loading the basic data from the safezones */
        load_safezones();
        load_log_files();
    }
    private void load_safezones(){
        for(int i = 0; i< safezones.size(); i++){
            Safezone safezone = safezones.get(i);

            String info_file_path = SAFEZONES_FOLDER_PATH+safezones.get(i).getID()+"\\"+safezone.getID()+".sz";
            try (BufferedReader br = new BufferedReader(new FileReader(info_file_path ) )){

                safezone.isIDEqual( Integer.parseInt(br.readLine() ) );

                safezone.setPassword(br.readLine());
                safezone.setSync_time(Integer.parseInt( br.readLine()));


                /* SETTING THE KEEPERS*/
                int n_keepers = Integer.parseInt( br.readLine());
                String keeper = null;
                for(int k = 0; k< n_keepers; k++) {
                    keeper = br.readLine();
                    if(!keeper.equals(NetworkManger.getMyIP()))
                        safezone.addKeepers(keeper);
                }

                int n_resources = Integer.parseInt(br.readLine());
                for(int k = 0; k< n_resources; k++){
                    String[] fields = br.readLine().split(" ");
                    safezone.addResource( Resource.importResource( fields[0],
                            fields[1] , Integer.parseInt(fields[2])  ));
                }

            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void load_log_files() {
        /*the earliest command is at the top*/
        for (int i = 0; i < safezones.size(); i++)
            safezones.get(i).load_local_log_file();

    }

    public void syn() throws IOException {
        for(int i = 0; i< safezones.size(); i++)
            safezones.get(i).syn();

    }
}
