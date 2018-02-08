package com.geometris.wqlib;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Represents vehicle and location data as provided by the Whereqube over bluetooth.
 */
public class GeoData implements Serializable {
    private Integer protocolId;
    private String vin;
    private Double odometer;     // HiResTotalDistance
    private DateTime odometerTimeStamp;
    private Double engTotalHours;
    private DateTime engTotalHoursTimestamp;
    private Double vehicleSpeed; //vehicle Speed
    private DateTime vehicleSpeedTimestamp;
    private Double engineRpm;    // Engine RPM
    private DateTime engineRpmTimestamp;
    private Double latitude;        // Latitude
    private Double longitude;        // Longitude
    private Long gpsTime;
    private DateTime timeStamp;
    private Integer totalUdrvEvents;
    private ArrayList<UnidentifiedEvent> unidentifiedEventArrayList;
    private boolean dataSet =false;


    /**
     * Constructor.
     * By default, all properties begin with null values,
     * and the unidentified events are empty.
     */
    public GeoData() {
        protocolId = null;
        vin = null;
        odometer = null;     // HiResTotalDistance
        odometerTimeStamp = null;
        engTotalHours=null;
        engTotalHoursTimestamp = null;
        vehicleSpeed = null; //vehicle Speed
        vehicleSpeedTimestamp = null;
        engineRpm = null;    // Engine RPM
        engineRpmTimestamp = null;
        latitude = null;        // Latitude
        longitude = null;        // Longitude
        gpsTime = null;
        timeStamp = null;
        totalUdrvEvents = null;
        unidentifiedEventArrayList = new ArrayList<UnidentifiedEvent>();
    }

    /**
     * @return True if the any of the data have been set, false otherwise
     */
    public boolean isDataSet(){ return dataSet;}

    /**
     * the protocol version
     * @return the protocol version
     */
    public Integer getProtocol() {
        return this.protocolId;
    }

    /**
     * Sets the protocol version to the given parameter.
     * @param protocol value for the protocol version.
     */
    public void setProtocol(Integer protocol) {
        this.protocolId = protocol;
        dataSet = true;
    }


    /**
     * Placeholder, not implemented.
     * @param fields fields to set
     * @return this object.
     */
    public GeoData putAllFields(Map<String, String> fields) {
        return this;
    }

    /**
     * The vehicle identification number.
     * @return the vehicle VIN
     */
    public String getVin() {
        return vin;
    }

    /**
     * {@link GeoData#getVin()}
     * @param vin VIN number.
     * @return this object.
     */
    public GeoData setVin(String vin) {
        this.vin = vin;
        dataSet = true;
        return this;
    }

    /**
     *
     * @return GPS latitude of the device.
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * {@link GeoData#getLatitude()}
     * @param latitude value to set latitude.
     */
    public void setLatitude(Double latitude){
        this.latitude = latitude;
        dataSet = true;
    }

    /**
     *
     * @return GPS longitude of the device.
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * {@link GeoData#getLongitude()}
     * @param longitude value to set longitude.
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
        dataSet = true;
    }


    /**
     *
     * @return Age of GPS fix, in minutes
     */
    public Long getGpsTime(){return gpsTime;}

    /**
     * {@link GeoData#getGpsTime()}
     * @param gpsTime value of gpsTime
     */
    public void setGpsTime(Long gpsTime){
        this.gpsTime = gpsTime;
    }


    /**
     *
     * @return Total engine hours.
     */
    public Double getEngTotalHours() {
        return engTotalHours;
    }

    /**
     * {@link GeoData#getEngTotalHours()}
     * @param engTotalHours value for total engine hours.
     */
    public void setEngTotalHours(Double engTotalHours) {
        this.engTotalHours = engTotalHours;
        dataSet = true;
    }

    /**
     *
     * @return Engine hours timestamp.
     */
    public DateTime getEngTotalHoursTimestamp()
    { return engTotalHoursTimestamp;}

    /**
     * {@link GeoData#getEngTotalHoursTimestamp()}
     * @param engTotalHoursTimestamp value to set engine hours timestamp.
     */
    public void setEngTotalHoursTimestamp(DateTime engTotalHoursTimestamp){

        this.engTotalHoursTimestamp = engTotalHoursTimestamp;
    }

    /**
     *
     * @return Engine odometer
     */
    public Double getOdometer()
    {
        return this.odometer;
    }

    /**
     * {@link GeoData#getOdometer()}
     * @param odometer value to set engine odometer.
     * @return returns this object.
     */
    public GeoData setOdometer(Double odometer) {
        this.odometer = odometer;
        dataSet = true;
        return this;
    }

    /**
     *
     * @return Odometer timestamp.
     */
    public DateTime getOdometerTimestamp()
    {
        return this.odometerTimeStamp;
    }

    /**
     * {@link GeoData#getOdometerTimestamp()}
     * @param odometerTimestamp value to set odometer timestamp.
     */
    public void setOdometerTimestamp(DateTime odometerTimestamp)
    {
        this.odometerTimeStamp= odometerTimestamp;
    }

    /**
     *
     * @return engine revolutions per minute
     */
    public Double getEngineRPM(){
        return this.engineRpm;
    }

    /**
     * {@link GeoData#getEngineRPM()}
     * @param engineRPM value to set engine rpms
     * @return returns this object
     */
    public GeoData setEngineRPM(Double engineRPM){
        this.engineRpm = engineRPM;
        dataSet = true;
        return this;
    }

    /**
     *
     * @return gets the Engine RPMS timestamp
     */
    public DateTime getEngineRpmTimestamp(){ return this.engineRpmTimestamp;}

    /**
     * {@link GeoData#getEngineRpmTimestamp()}
     * @param engineRpmTimestamp value to set engine rpms
     */
    public void setEngineRpmTimestamp(DateTime engineRpmTimestamp){
        this.engineRpmTimestamp = engineRpmTimestamp;
    }

    /**
     *
     * @return gets the vehicle speed.
     */
    public Double getVehicleSpeed(){
        return this.vehicleSpeed;
    }

    /**
     * {@link GeoData#getVehicleSpeed()}
     * @param vehicleSpeed value to set vehicle speed
     * @return returns this object.
     */
    public GeoData setVehicleSpeed(Double vehicleSpeed){
        this.vehicleSpeed=vehicleSpeed;
        dataSet = true;
        return this;
    }

    /**
     *
     * @return The vehicle speed timestamp
     */
    public DateTime getVehicleSpeedTimestamp(){ return  this.vehicleSpeedTimestamp;}

    /**
     * {@link GeoData#getVehicleSpeedTimestamp()}
     * @param vehicleSpeedTimestamp value to set vehicle speed timestamp
     */
    public void setVehicleSpeedTimestamp(DateTime vehicleSpeedTimestamp){
        this.vehicleSpeedTimestamp = vehicleSpeedTimestamp;
    }

    /**
     *
     * @return Timestamp of the data
     */
    public DateTime getTimeStamp() {
        return timeStamp;
    }

    /**
     * {@link GeoData#getTimeStamp()}
     * @param timeStamp value to set data timestamp
     * @return this object
     */
    public GeoData setTimeStamp(DateTime timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    /**
     *
     * @return the count of unidentified driver events
     */
    public Integer getTotalUdrvEvents() {
        return totalUdrvEvents;
    }

    /**
     * {@link GeoData#getTotalUdrvEvents()}
     * @param totalUdrvEvents value to set count of unidentified driver events to
     * @return this object.
     */
    public GeoData setTotalUdrvEvents(Integer totalUdrvEvents){
        this.totalUdrvEvents = totalUdrvEvents;
        return this;
    }

    /**
     *
     * @return unidentified driver events.
     */
    public ArrayList<UnidentifiedEvent> getUnidentifiedEventArrayList(){ return unidentifiedEventArrayList;}

    /**
     * {@link GeoData#getUnidentifiedEventArrayList()}
     * @param unidentifiedEventArrayList value to set the unidentified driver events
     */
    public void setUnidentifiedEventArrayList(ArrayList<UnidentifiedEvent> unidentifiedEventArrayList){
        this.unidentifiedEventArrayList = unidentifiedEventArrayList;
    }

    /**
     * Creates a copy of this GeoData instance.
     * @return copy of this instance.
     */
    public GeoData copy() {
        GeoData newGeoData = new GeoData();
        newGeoData.protocolId = this.protocolId;
        newGeoData.vin = this.vin;
        newGeoData.latitude = this.latitude;        // Latitude
        newGeoData.longitude = this.longitude;        // Longitude
        newGeoData.gpsTime = this.gpsTime;
        newGeoData.odometer = this.odometer;     // HiResTotalDistance
        newGeoData.odometerTimeStamp = this.odometerTimeStamp;
        newGeoData.engineRpm = this.engineRpm;    // Engine RPM
        newGeoData.engineRpmTimestamp = this.engineRpmTimestamp;
        newGeoData.vehicleSpeed = this.vehicleSpeed; //vehicle Speed
        newGeoData.vehicleSpeedTimestamp = this.vehicleSpeedTimestamp;
        newGeoData.timeStamp = this.timeStamp;
        newGeoData.engTotalHours=this.engTotalHours;
        newGeoData.engTotalHoursTimestamp = this.engTotalHoursTimestamp;
        newGeoData.totalUdrvEvents = this.totalUdrvEvents;
        for (UnidentifiedEvent item: this.unidentifiedEventArrayList) {
            newGeoData.unidentifiedEventArrayList.add((UnidentifiedEvent)item.copy());
        }
        return newGeoData;
    }

}