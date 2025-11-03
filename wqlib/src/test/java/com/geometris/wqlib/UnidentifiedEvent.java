package com.geometris.wqlib;


import java.io.Serializable;

/**
 * Specifies data for vehicle events from unidentified drivers.
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
        engTotalHours = null;   //total hours
        vehicleSpeed = null; //vehicle Speed
        odometer = null;     // HiResTotalDistance
        latitude = null;        // Latitude
        longitude = null;        // Longitude
        gpsTimeStamp = null;
    }

    /**
     * Reason code for the event.
     * @return Reason code for the event.
     */
    public Integer getReason(){return reason; }

    /**
     * {@link UnidentifiedEvent#getReason()}
     * @param reason value for the event reason code.
     */
    public void setReason(Integer reason){
        this.reason = reason;
    }


    /**
     * Timestamp for the event, when it happened.
     * @return Timestamp for the event.
     */
    public Long getTimestamp(){return timestamp;}

    /**
     * {@link UnidentifiedEvent#getTimestamp()}
     * @param timestamp value for the timestamp for the event.
     */
    public void setTimestamp(Long timestamp){
        this.timestamp = timestamp;
    }


    /**
     * Total engine hours.
     * @return total engine hours
     */
    public Double getEngTotalHours() {
        return engTotalHours;
    }

    /**
     * {@link UnidentifiedEvent#getEngTotalHours()}
     * @param engTotalHours value for total engine hours
     */
    public void setEngTotalHours(Double engTotalHours) {
        this.engTotalHours = engTotalHours;
    }

    /**
     * Vehicle speed.
     * @return vehicle speed.
     */
    public Double getVehicleSpeed(){
        return this.vehicleSpeed;
    }

    /**
     * {@link UnidentifiedEvent#getVehicleSpeed()}
     * @param vehicleSpeed value for vehicle speed
     * @return returns this object.
     */
    public UnidentifiedEvent setVehicleSpeed(Double vehicleSpeed){
        this.vehicleSpeed=vehicleSpeed;
        return this;
    }

    /**
     * Vehicle odometer.
     * @return vehicle odometer.
     */
    public Double getOdometer()
    {
        return this.odometer;
    }

    /**
     * {@link UnidentifiedEvent#getOdometer()}
     * @param odometer value for the vehicle odometer
     * @return this object.
     */
    public UnidentifiedEvent setOdometer(Double odometer) {
        this.odometer = odometer;
        return this;
    }

    /**
     * GPS Latitude.
     * @return gps latitude.
     */
    public Double getLatitude() {return latitude;}

    /**
     * {@link UnidentifiedEvent#getLatitude()}
     * @param latitude value for latitude.
     */
    public void setLatitude(Double latitude){
        this.latitude = latitude;
    }

    /**
     * GPS longitude.
     * @return gps longitude.
     */
    public Double getLongitude() {return longitude;}

    /**
     * {@link UnidentifiedEvent#getLongitude()}
     * @param longitude value for longitude
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * GPS fix age.
     * @return gps fix age.
     */
    public Long getGPSTimestamp(){return gpsTimeStamp;}

    /**
     * {@link UnidentifiedEvent#getGPSTimestamp()}
     * @param gpsTimeStamp value for gps fix age.
     */
    public void setGPSTimestamp(Long gpsTimeStamp){
        this.gpsTimeStamp = gpsTimeStamp;
    }

    /**
     * Creates a copy of this instance.
     * @return a copy of this instance.
     */
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
