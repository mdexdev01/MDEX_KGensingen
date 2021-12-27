package com.marveldex.MdexDaq;

/**
 * Created by sehjin12-pc on 2016-12-11.
 */


import java.util.Locale;

/**
 *
 * @details 압력세서로부터 전달 받은 데이터를 처리하여 직관적인 값으로 변환하고 사용할 변수에 Setting
 * @author Marveldex
 * @date 2017-03-17
 * @version 0.0.1
 * @li list1
 * @li list2
 *
 */

public class PacketParser {
    //  BLE packet
    public static int def_PACKET_LENGTH = 20; // length 20 is fixed length of BLE packet
    public static final byte [] adcValue_S = new byte[def_PACKET_LENGTH]; // ADC of Shield
    final byte [] packet_data_32bit = new byte[def_PACKET_LENGTH];

    // protocol buffer new - 2021.12.13
    public static int def_PACKET_HEADER_LEN = 1;
    public static int def_PACKET_SENSOR_DATA_LEN = 16;
    public static int def_PACKET_INFO_LEN = 3;

    public static byte packetHeader;
    public static byte [] daqBoardInfo = new byte [def_PACKET_INFO_LEN];

    public static int def_PACKET_BUILD_NUM = 2;
    public static int def_SENSOR_DATA_FULL_LEN = def_PACKET_SENSOR_DATA_LEN * def_PACKET_BUILD_NUM;
    public static int [] sensorDataAll = new int [def_SENSOR_DATA_FULL_LEN];

    public static int def_CUSTOM_SENSOR_NUM = 21;
    public static int [] customSensorDataAll = new int [def_CUSTOM_SENSOR_NUM];



    //  Data header of Marveldex.
    public static byte def_OFFSET_HEADER = 0; // offset of header in Packet : 0
    public static byte def_Header_C = 'C'; // packet header of configuration
    public static byte def_Header_M = 'M'; // packet header of Main
    public static byte def_Header_S = 'S'; // packet header of Sub adc data payload

    //  Wheelopia data
    public static byte def_DATA_OFFSET = 1; // offset of cell data in Packet : 1~10
    public static byte def_CELL_COUNT_WHEELOPIA = 10;
    public static byte [] nDataWheelopia = new byte [def_CELL_COUNT_WHEELOPIA];

    public static byte determiCount = 5;
    public static int determiValues1B[] = new int [determiCount + 1]; // 1 Base index. So index 0 is dummy.

    public static boolean m_isHaveAllData = false;

    private static int def_THRESHOLD_VALID_LOWEST = 5;
    public static int def_THRESHOLD_VALUE_SEAT_OCCUPIED = 80;
    private static int def_THRESHOLD_VALUE_ONE_LEG_EMPTY = 25;

    public static String logPacket = " ";


    public PacketParser(){
    }

    public static void setDataFromCSV(String [] data) {
        for (int i = 0 ; i < def_CUSTOM_SENSOR_NUM ; i++) {
            customSensorDataAll[i] = Integer.parseInt(data[i + 1]);
        }
    }

    public static int getCustomSensorData(int cell_index) {
        return customSensorDataAll[cell_index];
    }

    public static int getCustomCellValue1(int cell_number1) {
        return customSensorDataAll[cell_number1];
    }

    public void setThreshold_Occupy(int value) {

    }


    public void setThreshold_Wheelopia(int i, int v) {
        determiValues1B[i] = v;
    }

    //  0 base cell index. index range : 0 ~ 9
    public static byte getSensorData10ch(int cell_index) {
        return nDataWheelopia[cell_index];
    }

    //  1 base cell index. index range : 1 ~ 10
    public static byte cellValue1(int cell_number1) {
        if(cell_number1 == 0)
            return 0;
        return nDataWheelopia[cell_number1 - 1];
    }

    /**
         *
         * @brief    전달 받은 압력 값을 구분하고 해당 값들을 변수에 Setting
         * @details 1) Convert 8bit to 32bit, 2) Distinguish what if this packet is 'M' or 'S' and store to buffer 3) Reorder sensor sequence after last packet ('S') arrived.
         * @param
         * @return
         * @throws
         */
    public void onReceiveRawPacket(byte [] packet_raw_data){
        //  1) Convert 8bit to 32bit
        for(int index = 0 ; index < def_PACKET_LENGTH ; index++){
            packet_data_32bit[index] = packet_raw_data[index];// & 0xff;
        }

        m_isHaveAllData = false;

        byte header_byte = packet_raw_data[0];
        packetHeader = packet_raw_data[0];

        if(packetHeader == def_Header_C) { // 'C' : Configuration
            //System.arraycopy(packet_raw_data, 1, determiValues1B, 1, determiCount); // 1: offset of source, 1: offset of dest, determiCount: length)
            for(int i = 1 ; i <= 5 ; i++) {
                determiValues1B[i] = packet_data_32bit[i] * 10;
            }
        }
        else if( packetHeader == def_Header_M){ //'M' : Main
//            System.arraycopy(packet_data_32bit, def_PACKET_HEADER_LEN, sensorDataAll, 0, def_PACKET_SENSOR_DATA_LEN);
            for (int i = 0 ; i < def_PACKET_SENSOR_DATA_LEN ; i++) {
                sensorDataAll[i] = packet_data_32bit[def_PACKET_HEADER_LEN + i];
            }
            System.arraycopy(packet_raw_data, (def_PACKET_HEADER_LEN + def_PACKET_SENSOR_DATA_LEN), daqBoardInfo, 0, def_PACKET_INFO_LEN);
        }
        else if( packetHeader == def_Header_S) {// 'S' : Sub
//            System.arraycopy(packet_data_32bit, def_PACKET_HEADER_LEN, sensorDataAll, (def_PACKET_SENSOR_DATA_LEN * 1), def_PACKET_SENSOR_DATA_LEN);
            for (int i = 0 ; i < def_PACKET_SENSOR_DATA_LEN ; i++) {
                sensorDataAll[def_PACKET_SENSOR_DATA_LEN + i] = packet_data_32bit[def_PACKET_HEADER_LEN + i];
            }
            System.arraycopy(packet_raw_data, (def_PACKET_HEADER_LEN + def_PACKET_SENSOR_DATA_LEN), daqBoardInfo, 0, def_PACKET_INFO_LEN);

            System.arraycopy(packet_raw_data, def_PACKET_HEADER_LEN, nDataWheelopia, 0, def_CELL_COUNT_WHEELOPIA); // 1: offset of source, 0: offset of dest, def_CELL_COUNT_WHEELOPIA: length)

            m_isHaveAllData = true;
        }

//        logPacket = String.format(Locale.KOREA, "[%c]: %3d,%3d,%3d,%3d,%3d,%3d,%3d,%3d,%3d,%3d,%3d,%3d,%3d,%3d,%3d,%3d",
//                packet_raw_data[0],
//                packet_data_32bit[1], packet_data_32bit[2], packet_data_32bit[3], packet_data_32bit[4],
//                packet_data_32bit[5], packet_data_32bit[6], packet_data_32bit[7], packet_data_32bit[8],
//                packet_data_32bit[9], packet_data_32bit[10], packet_data_32bit[11], packet_data_32bit[12],
//                packet_data_32bit[13], packet_data_32bit[14], packet_data_32bit[15], packet_data_32bit[16]);

        if(m_isHaveAllData == true) {
            customSensorDataAll[0] = sensorDataAll[1];
            customSensorDataAll[1] = sensorDataAll[4];
            customSensorDataAll[2] = sensorDataAll[3];
            customSensorDataAll[3] = sensorDataAll[6];
            customSensorDataAll[4] = sensorDataAll[5];
            customSensorDataAll[5] = sensorDataAll[8];
            customSensorDataAll[6] = sensorDataAll[7];

            customSensorDataAll[7] = sensorDataAll[10];
            customSensorDataAll[8] = sensorDataAll[9];
            customSensorDataAll[9] = sensorDataAll[12];
            customSensorDataAll[10] = sensorDataAll[11];
            customSensorDataAll[11] = sensorDataAll[14];
            customSensorDataAll[12] = sensorDataAll[13];
            customSensorDataAll[13] = sensorDataAll[16];

            customSensorDataAll[14] = sensorDataAll[15];
            customSensorDataAll[15] = sensorDataAll[18];
            customSensorDataAll[16] = sensorDataAll[17];
            customSensorDataAll[17] = sensorDataAll[20];
            customSensorDataAll[18] = sensorDataAll[19];
            customSensorDataAll[19] = sensorDataAll[22];
            customSensorDataAll[20] = sensorDataAll[21];
//---
            customSensorDataAll[6] = sensorDataAll[15];
            customSensorDataAll[5] = sensorDataAll[18];
            customSensorDataAll[4] = sensorDataAll[17];
            customSensorDataAll[3] = sensorDataAll[20];
            customSensorDataAll[2] = sensorDataAll[19];
            customSensorDataAll[1] = sensorDataAll[22];
            customSensorDataAll[0] = sensorDataAll[21];

            customSensorDataAll[13] = sensorDataAll[10];
            customSensorDataAll[12] = sensorDataAll[9];
            customSensorDataAll[11] = sensorDataAll[12];
            customSensorDataAll[10] = sensorDataAll[11];
            customSensorDataAll[9]  = sensorDataAll[14];
            customSensorDataAll[8]  = sensorDataAll[13];
            customSensorDataAll[7]  = sensorDataAll[16];

            customSensorDataAll[20] = sensorDataAll[1];
            customSensorDataAll[19] = sensorDataAll[4];
            customSensorDataAll[18] = sensorDataAll[3];
            customSensorDataAll[17] = sensorDataAll[6];
            customSensorDataAll[16] = sensorDataAll[5];
            customSensorDataAll[15] = sensorDataAll[8];
            customSensorDataAll[14] = sensorDataAll[7];

        }
    }

    public static boolean isSeatOccupied(){
        int summation = 0;
        for(int i = 0 ; i < def_CUSTOM_SENSOR_NUM ; i++) {
            summation += customSensorDataAll[i];

            if(def_THRESHOLD_VALUE_SEAT_OCCUPIED < summation)
                return true;
        }

        return false;
    }


    public static boolean isHaveAllData(){
        return m_isHaveAllData;
    }

    public static byte getBatteryLevel(){
        return daqBoardInfo[0];//m_BatteryLevel;
    }

    public static boolean getHW_DIPSW_state(int dipsw_index) {
        //  valid dipsw_index value is 0, 1, 2 only.
        if( (dipsw_index < 0) || (2 < dipsw_index) ) {
            return false;
        }

        boolean ret_val = false;
        //  Venus source code...
        // -->  g_ucNUS_PacketBuffer[19] = (g_DIPSWITCH_state[0] << 7) | (g_DIPSWITCH_state[1] << 6) | (g_DIPSWITCH_state[2] << 5);
        switch (dipsw_index) {
            case 0: // 0b1000000 = 0x80
                //ret_val = ( ((m_HWState & 0x80) >> 7) == 0x01);
                break;
            case 1:
                //ret_val = ( ((m_HWState & 0x40) >> 6) == 0x01);
                break;
            case 2:
                //ret_val = ( ((m_HWState & 0x20) >> 5) == 0x01);
                break;
        }

        return ret_val;
    }


    //-------------------------------------------------------------------------
    //  Posture determination
    //-------------------------------------------------------------------------
    public static int getLeftLegPressureSum() {
        int sum_value_left = 0;

        //  left : 0~2 cells of row 0
        for(int cell_index = 0 ; cell_index <= 2 ; cell_index++) {
            //sum_value_left += nPressureValue_Row0 [cell_index];
        }
        return sum_value_left;
    }

    public static int getRightLegPressureSum() {
        int sum_value_right = 0;

        //  right : 3~5 cells of row 0
        for(int cell_index = 3 ; cell_index <= 5 ; cell_index++) {
            //sum_value_right += nPressureValue_Row0 [cell_index];
        }
        return sum_value_right;
    }

    public static boolean isLeftLeg_StuckOnChair() {
        if(isSeatOccupied() == false)
            return false;

        if( def_THRESHOLD_VALUE_ONE_LEG_EMPTY < getLeftLegPressureSum() ){
            return true;
        }

        return false;
    }

    public static boolean isRightLeg_StuckOnChair() {
        if(isSeatOccupied() == false)
            return false;

        if( def_THRESHOLD_VALUE_ONE_LEG_EMPTY < getRightLegPressureSum() ){
            return true;
        }

        return false;
    }

}
