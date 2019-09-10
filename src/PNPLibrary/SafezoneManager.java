package PNPLibrary;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.SimpleFormatter;

public class SafezoneManager {
    private ArrayList<Safezone> safezones;
    private static final String SAFEZONE_FOLDER_PATH = "safezones\\";

    public SafezoneManager(){
        safezones = new ArrayList<>();
    }

    public void add(Safezone safezone) {
        safezones.add(safezone);
    }

    public void init_safezones() throws FileNotFoundException {

        for(int i = 0; i< safezones.size(); i++){
            Safezone safezone = safezones.get(i);

            String info_file_path = SAFEZONE_FOLDER_PATH+safezones.get(i).getID()+"\\"+safezone.getID()+".sz";
            try (BufferedReader br = new BufferedReader(new FileReader(info_file_path ) )){
                  safezone.isIDEqual( Integer.parseInt(br.readLine()) );
                  safezone.setPassword(br.readLine().toCharArray());
                  safezone.setSync_time(Integer.parseInt( br.readLine()));

                  int n_keepers = Integer.parseInt( br.readLine());
                  for(int k = 0; k< n_keepers; k++)
                      safezone.addKeepers(br.readLine());


                  int n_resources = Integer.parseInt(br.readLine());
                  for(int k = 0; k< n_resources; k++){
                      String[] fields = br.readLine().split(" ");
                      safezone.addResource( Resource.importResource( fields[0], new SimpleDateFormat("dd/MM/yyyy/HH:mm:ss").parse(fields[1]),
                                            fields[2] , Integer.parseInt(fields[3])  ));
                  }

            }catch (IOException e){

            } catch (ParseException e) {
                e.printStackTrace();
            }


        }
    }
}
