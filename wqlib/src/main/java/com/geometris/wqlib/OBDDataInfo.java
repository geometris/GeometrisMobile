package com.geometris.wqlib;


import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by bipin_2 on 1/25/2018.
 */

public class OBDDataInfo {
    public static final String TAG ="Geometris";
    public static final int PACKET_COUNT_OFFSET = 0, PACKET_IDENTIFIER=1,PROTOCOL_IDENTIFIER = 2,TOTAL_PACKET_INDEX=3;

    public GeoData geoData = null;
    private static final long RPM_MAX_AGE_IN_MILLIS= 30000; //30 seconds
    private static final Double RPM_THRESHOLD= 200.00; //30 seconds
    private static Double prevEngineRpm=0.0;
    private static DateTime prevEngineRpmTimestamp = DateTime.now();
    private Byte protocolId;
    private Byte totalPacket;
    private Set<Byte> pi;
    private HashMap<Byte, byte[]> packetList;
    private boolean complete;
    StringBuilder VINsb = new StringBuilder();

    public OBDDataInfo() {
        this.pi = new HashSet<>();
        this.protocolId = -1;
        this.totalPacket=0;
        this.packetList = new HashMap<>();
        this.geoData = new GeoData();
        this.complete = false;
    }
    private void setProtocolId(Byte protocolId){
        this.protocolId = protocolId;
        if(protocolId==0){
            this.totalPacket=7;
        }

    }
    private boolean is_RPM_Active(){
        if(prevEngineRpm < RPM_THRESHOLD || ((DateTime.now().getMillis()-prevEngineRpmTimestamp.getMillis())>RPM_MAX_AGE_IN_MILLIS)){
            return false;
        }
        return true;
    }

    public Byte getProtocolId(){
        return protocolId;
    }
    private void setTotalPacket(Byte totalPacket){
        this.totalPacket = totalPacket;
    }
    public Byte getTotalPacket(){ return this.totalPacket; }

    public boolean isFull() {
        if(totalPacket>0) {
            if (protocolId == 0)
                return pi.size() >= 7;
            else if (protocolId >= 1)
                return pi.size() >= totalPacket;
        }
        return false;
    }

    public void insertPacket(byte[] value)
    {
        if(value.length<=0) return;

        Byte packet_count = value[PACKET_COUNT_OFFSET];
        Log.d(TAG, "OBD Raw Data:");
        String logString="";
        StringBuilder sb = new StringBuilder();
        for(byte c : value) {
            sb.append(String.format("%02x, ",c));
        }
        logString = new String(sb);
        Log.d(TAG, logString + "\r\n");

        if(packet_count> 0 && pi.isEmpty())
            return;
        if(packet_count ==0 )
        {
            if(value.length>1 && value[PACKET_IDENTIFIER] == (byte) 0xCB)
            {
                Byte protocol_id = value[PROTOCOL_IDENTIFIER];
                Byte totalPacket = value[TOTAL_PACKET_INDEX];
                setProtocolId(protocol_id);
                setTotalPacket( totalPacket);
            }
            else{
                setProtocolId( (byte) 0);
            }
        }
        insertIndex(packet_count);
        insertPacket(packet_count, value);

    }
    private void insertIndex(Byte index) {
        pi.add(index);
    }
    private void insertPacket(Byte index, byte[] packet){
        packetList.put(index, packet);
   }
    private HashMap<Byte, byte[]> getPacketList(){ return packetList;};

    private void insertVIN(Byte index, String data) {
        if (index == 0) {
            if (data.length() > 0)
                VINsb.insert(0, data);
        } else if (index == 1) {
            if (VINsb != null && VINsb.length() > 0) {
                VINsb.append(data);
                if (VINsb.charAt(0) != '-') {
                    if (this.geoData != null)
                        this.geoData.setVin(VINsb.toString());
                }
            }
            VINsb = new StringBuilder(35);
        }
    }

    /**
     * For Compatibility with older version of firmware.
     * @param RPM
     * @param rpmTime
     */
    private void setRPM(double RPM, DateTime rpmTime){
        if(RPM !=-1) {
            geoData.setEngineRPM(RPM);
            prevEngineRpm = RPM;
            prevEngineRpmTimestamp=rpmTime;
        }
        else
        {
            if(is_RPM_Active())
                geoData.setEngineRPM(prevEngineRpm);
        }
    }
    public GeoData getGeoData()
    {
        if(!isFull())
            return null;
        DateTime now = DateTime.now();
        geoData.setTimeStamp(now);
        DateTimeFormatter formatter = DateTimeFormat.forPattern("HH:mm:ss");
        HashMap<Byte, byte[]> packets = getPacketList();
        geoData.setProtocol( getProtocolId().intValue());
        StringBuilder sb = new StringBuilder();

        if(getProtocolId()==0){

            Byte ind = 0, totalPacket = getTotalPacket();

            while(ind<totalPacket){
                byte[] packet = packets.get(ind);
                switch(ind) {
                    case 0:
                    case 1:
                        String VIN = WQData.getStringValue(1, packet);
                        insertVIN(ind, VIN);
                        break;
                    case 2:

                        double odometer =
                                (double) WQData.getIntValue(WQData.FORMAT_UINT32,
                                        1, packet);
                        if(odometer != -1) {
                            geoData.setOdometer(odometer);
                        }

                        geoData.setOdometerTimestamp(now);
                        double RPM =
                                (double) WQData.getIntValue(WQData.FORMAT_UINT32,
                                        5,packet);

                        setRPM(RPM, now);
                        geoData.setEngineRpmTimestamp(now);
                        Log.d(TAG, "rpm:"+RPM);

                        double speed =
                                (double) WQData.getIntValue(WQData.FORMAT_UINT32,
                                        13, packet);
                        if(speed != -1) {
                            geoData.setVehicleSpeed(speed);
                        }
                        geoData.setVehicleSpeedTimestamp(now);
                        break;
                    case 3:
                        break;

                    case 4:
                        break;
                    case 5:
                        break;
                    case 6:
                        double engine_hours = WQData.getIntValue(WQData.FORMAT_UINT32, 5, packet);
                        if (engine_hours != -1) {
                            geoData.setEngTotalHours(engine_hours / 10); //its divided by 10 because device is sending (hours times 10)
                        }
                        geoData.setEngTotalHoursTimestamp(now);
                        break;
                }
                ind++;
            }

        }
        else if(getProtocolId()==1) {
            byte[] geobuff = new byte[500];
            Byte pi = 0,totalPacket = getTotalPacket();
            int totaldata=0,  index_count=0;

            while(pi<totalPacket) {
                byte[] packetData = packets.get(pi);
                for (int k = 1; k < packetData.length; k++) {
                    if(pi==0 && k <=(TOTAL_PACKET_INDEX+1)) continue;
                    geobuff[totaldata++] = packetData[k];
                }
                pi++;
            }

            index_count=2;
            boolean parseexit =false;
            byte[] tbytes;
            UnidentifiedEvent unidentifiedEvent = new UnidentifiedEvent();
            Integer totalUdrvEvents = 0;
            while(index_count < totaldata){

                switch(geobuff[index_count])
                {
                    case 0x01:  //VIN
                        index_count+=2;
                        int vinlength  = geobuff[index_count];
                        index_count+=2;
                        if(vinlength>0) {
                            byte[] strBytes = new byte[vinlength];
                            for (int i = 0; i < vinlength; i++) {
                                strBytes[i] = geobuff[index_count];
                                index_count+=2;
                            }
                            String vin = new String(strBytes);
                            geoData.setVin(vin);
                        }

                        break;

                    case 0x02:
                        index_count+=2;
                        tbytes= WQData.fixUint32Endian(geobuff, index_count);
                        double odometer = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                        if (odometer != -1) {
                            geoData.setOdometer(odometer);
                        }
                        geoData.setOdometerTimestamp(now);
                        index_count = index_count+4;
                        break;

                    case 0x03: //RPM
                        index_count+=2;
                        tbytes= WQData.fixUint32Endian(geobuff, index_count);
                        double RPM = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                        setRPM(RPM, now);
                        geoData.setEngineRpmTimestamp(now);
                        Log.d(TAG, "rpm:"+RPM);
                        index_count = index_count+4;

                        break;

                    case 0x04: //COOLANT
                        index_count+=2;
                        index_count = index_count+4;

                        break;
                    case 0x05: //SPEED
                        index_count+=2;
                        tbytes= WQData.fixUint32Endian(geobuff, index_count);
                        double speed = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                        if (speed != -1) {
                            geoData.setVehicleSpeed(speed);
                        }
                        geoData.setVehicleSpeedTimestamp(now);
                        index_count = index_count+4;
                        break;

                    case 0x06: //FUEL LEVEL
                        index_count+=2;
                        index_count = index_count+4;

                        break;
                    case 0x07: //ECU_VOLTAGE
                        index_count+=2;
                        index_count = index_count+4;


                        break;
                    case 0x08: //THROTTLE
                        index_count+=2;
                        index_count = index_count+4;

                        break;
                    case 0x09: //Ambient Temperature
                        index_count+=2;
                        index_count = index_count+4;
                        break;

                    case 0x0A: //OBD_MPG
                        index_count+=2;
                        index_count = index_count+4;
                        break;

                    case 0x0B: //OBD_TRIP_MPG
                        index_count+=2;
                        index_count = index_count+4;
                        break;

                    case 0x0C: //OBD_INSTANT_MPG
                        index_count+=2;
                        index_count = index_count+4;
                        break;

                    case 0x0D: //MIL_STATUS
                        index_count+=2;
                        index_count = index_count+4;
                        break;

                    case 0x0E: //DTC_COUNT
                        index_count+=2;
                        index_count = index_count+4;
                        break  ;
                    case 0x0F:
                        index_count+=2;
                        int i= geobuff[index_count];
                        index_count+=2;
                        index_count = index_count +i*2 ;
                        break;
                    case 0x10: //REGEN_SWITCH_STATUS
                        index_count+=2;
                        index_count = index_count+4;
                        break;

                    case 0x11: //Engine Hours
                        index_count+=2;
                        tbytes= WQData.fixUint32Endian(geobuff, index_count);
                        double engine_hours = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                        if (engine_hours != -1) {
                            geoData.setEngTotalHours(engine_hours / 10); //its divided by 10 because device is sending (hours times 10)
                        }
                        geoData.setEngTotalHoursTimestamp(now);
                        index_count = index_count+4;
                        break;
                    case 0x12:
                        index_count+=2;
                        index_count = index_count+4;
                        break;
                    case 0x13: //Longitude
                        index_count+=2;
                        tbytes= WQData.fixUint32Endian(geobuff, index_count);
                        double latitude = WQData.getIntValue(WQData.FORMAT_UINT32, 0,tbytes);
                        if (latitude != -1)
                            geoData.setLatitude(latitude / 100000);
                        index_count = index_count+4;
                        break;
                    case 0x14: //Latitude
                        index_count+=2;
                        tbytes= WQData.fixUint32Endian(geobuff, index_count);
                        double longitude = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                        if (longitude != -1)
                            geoData.setLongitude(longitude / 100000);
                        index_count = index_count+4;
                        break;
                    case 0x15: //Location Time Stamp
                        index_count+=2;
                        tbytes= WQData.fixUint32Endian(geobuff, index_count);
                        long timestamp = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                        geoData.setGpsTime(timestamp*1000);
                        long _startTS = (long) (timestamp * 1000);
                        DateTime _startDate = new DateTime( _startTS );
                        DateTimeFormatter formatter1 = DateTimeFormat.forPattern("yyyy/M/d h:m:s a");
                        Log.d(TAG, "LATLON TIMESTAMP: " + formatter1.print(_startDate));
                        index_count = index_count+4;
                        break;
                    case 0x16:
                    case 0x17:
                    case 0x18:
                    case 0x19:
                    case 0x1A:
                    case 0x1B:
                    case 0x1C:
                    case 0x1D:
                    case 0x1E:
                        byte index_value =  geobuff[index_count];
                        index_count+=2;
                        tbytes= WQData.fixUint32Endian(geobuff, index_count);
                        index_count = index_count+4;
                        switch(index_value) {
                            case 0x1E:
                                totalUdrvEvents = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                                Log.d(TAG, "Total unidentified Event:" + totalUdrvEvents       +", ");
                                geoData.setTotalUdrvEvents(totalUdrvEvents);
                                break;
                            case 0x17:
                                long tstamp = WQData.getIntValue(WQData.FORMAT_UINT32,0, tbytes);
                                Log.d(TAG, "timestamp:" + tstamp       +", ");
                                unidentifiedEvent.setTimestamp(tstamp);
                                break;
                            case 0x16:
                                Integer reason = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                                Log.d(TAG, "Unidentified Data:\r\n");
                                unidentifiedEvent.setReason(reason);
                                break;
                            case 0x18:
                                double eHrs = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                                unidentifiedEvent.setEngTotalHours(eHrs);
                                break;
                            case 0x19:
                                double vSpeed = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                                unidentifiedEvent.setVehicleSpeed(vSpeed);
                                break;
                            case 0x1A:
                                double odom = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                                unidentifiedEvent.setOdometer(odom);
                                break;
                            case 0x1B:
                                double lat = WQData.getIntValue(WQData.FORMAT_UINT32,  0,tbytes);
                                if (lat != -1)
                                    unidentifiedEvent.setLatitude(lat / 100000);
                                break;
                            case 0x1C:
                                double lon = WQData.getIntValue(WQData.FORMAT_UINT32,  0,tbytes);
                                if (lon != -1)
                                    unidentifiedEvent.setLongitude(lon / 100000);
                                break;
                            case 0x1D:
                                long gpsTime = WQData.getIntValue(WQData.FORMAT_UINT32, 0, tbytes);
                                unidentifiedEvent.setGPSTimestamp(gpsTime);
                                break;
                        }
                        break;
                    default:
                        parseexit=true;
                        break;
                }
                if(parseexit)
                    break;
            }
            if(unidentifiedEvent.getTimestamp()!=null ){
                geoData.getUnidentifiedEventArrayList().add(unidentifiedEvent);
            }

    }

    sb.append(new String("vi:"+geoData.getVin()+"("+now+")"));
    sb.append(new String("od:"+geoData.getOdometer()+"("+now+")"));
    sb.append(new String("r:"+geoData.getEngineRPM()+"("+now+")"));
    sb.append(new String("sp:"+geoData.getVehicleSpeed()+"("+now+")"));
    sb.append(new String("enhr:"+geoData.getEngTotalHours()+"("+now+")"));
    sb.append(new String("lt:"+geoData.getLatitude()));
    sb.append(new String("ln:"+geoData.getLongitude()));
    DateTime _startDate = new DateTime( geoData.getGpsTime() );
    DateTimeFormatter formatter1 = DateTimeFormat.forPattern("yyyy/M/d h:m:s a");
    sb.append(new String("gt:"+formatter1.print(_startDate)));
    String logString = new String(sb);
    Log.e(TAG, logString);
    Log.d(TAG, "New Data Updated: RPM:  " + geoData.getEngineRPM() + ", Time: " + formatter.print(now));

    return geoData;
    }
}
