package PNPLibrary;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class CourierManager {

    private static CourierManager manager;
    public ArrayList<Courier> couriers;

    public static CourierManager Manager(){
        if(manager == null)
            manager = new CourierManager();

        return manager;
    }

    public CourierManager(){
        couriers = new ArrayList<>();
    }

    public Courier createCourier(){
        Courier courier = new Courier();
        couriers.add(courier);
        return courier;
    }

    public Courier createCourier(Socket socket) throws IOException {
        Courier courier = new Courier(socket);
        couriers.add(courier);
        return courier;
    }

    public TrackerCourier createTrackerCourier(Socket socket) throws IOException {
        TrackerCourier trackerCourier = new TrackerCourier(socket);
        couriers.add(trackerCourier);
        return trackerCourier;
    }


    public void disconnenct_all() throws IOException {
        for(Courier c : couriers){
            c.stopRunning();
        }
    }

}
