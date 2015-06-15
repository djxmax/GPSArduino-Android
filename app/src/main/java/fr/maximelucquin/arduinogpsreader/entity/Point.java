package fr.maximelucquin.arduinogpsreader.entity;

import java.util.Date;

/**
 * Created by maxime on 09/06/15.
 */
public class Point {
    private int id;
    private double latitude, longitude, altitude, distPointToPoint, distTotal;
    private Date date;
    private String note;

    public Point(int id, double latitude, double longitude, double altitude, double distPointToPointDate, double distTotal,Date date, String note) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.distPointToPoint=distPointToPointDate;
        this.distTotal=distTotal;
        this.date=date;
        this.note=note;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getDistPointToPoint() {
        return distPointToPoint;
    }

    public void setDistPointToPoint(double distPointToPoint) {
        this.distPointToPoint = distPointToPoint;
    }

    public double getDistTotal() {
        return distTotal;
    }

    public void setDistTotal(double distTotal) {
        this.distTotal = distTotal;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
