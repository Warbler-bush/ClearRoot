package test;

import PNPLibrary.NetworkManger;
import PNPLibrary.Safezone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class NetworkMangerTest {
    public final static String peer_one = "127.0.0.1";
    public final static String peer_two = "127.0.0.2";
    public final static String peer_three = "127.0.0.3";



    @Before
    public void init_peer_one(){
        NetworkManger.init(true,peer_one);
        NetworkManger.setSafezonesListPathRoot("Test");
        NetworkManger.setSafezoneManagerFolderPathRoot("Test");
        NetworkManger manger = NetworkManger.manager();
    }



}