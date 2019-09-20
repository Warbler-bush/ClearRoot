package PNPLibrary;

import java.util.Date;
import java.util.logging.SimpleFormatter;

public class BaseLog {
    private Date date;
    private String type_modif;
    private String who;

    public String toLog(){
        return Resource.DateToString(date)+" "+type_modif+" "+who;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType_modif() {
        return type_modif;
    }

    public void setType_modif(String type_modif) {
        this.type_modif = type_modif;
    }

    public String getWho() {
        return who;
    }

    public void setWho(String who) {
        this.who = who;
    }

    public BaseLog(Date date, String type_modif, String who){
        this.date = date;
        this.type_modif = type_modif;
        this.who = who;
    }

}
