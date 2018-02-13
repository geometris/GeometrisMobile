package com.geometris.wqlib;


import java.io.Serializable;

/**
 * Created by bipin_2 on 1/31/2018.
 */

public class UnidentifiedEvent implements Serializable {
    private Integer reason;
    private Long timestamp;
    private Double engTotalHours;   //total hours
    private Double vehicleSpeed; //vehicle Speed
    private Double odometer;     // HiResTotalDistance
    private Double latitude;        // Latitude
    private Double longitude;        // Longitude
    private Long gpsTimeStamp;

    public UnidentifiedEvent() {
        reason = null;
        timestamp = null;
        engTotalHours = 0.0;   //total hours
        vehicleSpeed = 0.0; //vehicle Speed
        odometer = 0.0;     // HiResTotalDistance
        latitude = 0.0;        // Latitude
        longitude = 0.0;        // Longitude
        gpsTimeStamp = null;
    }
    public Integer getReason(){return reason; }
    public void setReason(Integer reason){
        this.reason = reason;
    }
    public Long getTimestamp(){return timestamp;}
    public void setTimestamp(Long timestamp){
        this.timestamp = timestamp;
    }
    public Double getEngTotalHours() {
        return engTotalHours;
    }

    public void setEngTotalHours(Double engTotalHours) {
        this.engTotalHours = engTotalHours;
    }

    public Double getVehicleSpeed(){
        return this.vehicleSpeed;
    }

    public UnidentifiedEvent setVehicleSpeed(Double vehicleSpeed){
        this.vehicleSpeed=vehicleSpeed;
        return this;
    }

    public Double getOdometer()
    {
        return this.odometer;
    }

    public UnidentifiedEvent setOdometer(Double odometer) {
        this.odometer = odometer;
        return this;
    }

    public Double getLatitude() {return latitude;}
    public void setLatitude(Double latitude){
        this.latitude = latitude;
    }

    public Double getLongitude() {return longitude;}
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    public Long getGPSTimestamp(){return gpsTimeStamp;}
    public void setGPSTimestamp(Long gpsTimeStamp){
        this.gpsTimeStamp = gpsTimeStamp;
    }
    public UnidentifiedEvent copy(){
        UnidentifiedEvent uEvent = new UnidentifiedEvent();
        uEvent.reason = reason;
        uEvent.timestamp = timestamp;
        uEvent.engTotalHours = engTotalHours;   //total hours
        uEvent.vehicleSpeed = vehicleSpeed; //vehicle Speed
        uEvent.odometer = odometer;     // HiResTotalDistance
        uEvent.latitude = latitude;        // Latitude
        uEvent.longitude = longitude;        // Longitude
        uEvent.gpsTimeStamp = gpsTimeStamp;
        return uEvent;
    }
}
