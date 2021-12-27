
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.marveldex.MdexDaq;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import androidx.annotation.AnyThread;
import androidx.collection.ArrayMap;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import ca.hss.heatmaplib.HeatMap;
import ca.hss.heatmaplib.HeatMapMarkerCallback;
import com.opencsv.CSVReader;

import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.elapsedRealtime;

/**
 *
 * @mainpage SCMS( Sitting Cushion Management System) - Marveldex
 * @brief 스마트 방석을 이용한 Application :
 * @details 방석의 각 압력 센서의 값을 VENUS 보드를 통해 전달 받아 데이터를 처리하여 Application을 통해 여러 형태의 값으로 보여주는 기능을 하는 Source
 *
 * @brief 2번째 설명 :
 * @details 방석의 각 압력 센서의 값을 VENUS 보드를 통해 전달 받아 데이터를 처리하여 Application을 통해 여러 형태의 값으로 보여주는 기능을 하는 Source
 *
 * TODO - 211224
 * Battery 잔량 Icon화
 * Replay
 * 변수 일관화
 * 클래스화
 * Material UI 도입
 * 센서 개수 32개로 변경하여.... Git Hub 에 올리기
 * 센서 셀 레이아웃 분리. 센서 번호와 프로토콜 매칭 파일 외장화
 *
 */

/*
        === CONTENTS  ===
        <><> MAIN ACTIVITY LIFE CYCLE DEFINITION
        onActivityResult
        SERVICE AND BROADCAST RECEIVER BLOCK

        <><> UI_onCreate
        BLE CHECK BLIND AND AUTO RECONNECT BLOCK
        onDataProc
        SET SENSOR VALUE AND CALL PRSSURE MAP BLOCK
        HEATMAP BLOCK
        UPDATE STATE, COM, NOTI BLOCK
        CALC COM COORD BLOCK
        NOTIFY MANAGER BLOCK
        STATE TABLE VIEW BLOCK
        SAVE CSV BLOCK
        REPLAY BLOCK
*/

/**
 *
 * @file MainActivity.java
 * @brief 메인 기능을 수행하는 파일
 *
 */

/**
 *
 * @brief this is main function for run this app
 * @details show values of sence and save to CSV File
 * @author Marveldex
 * @date 2017-03-17
 * @version 0.0.1
 * @li list1
 * @li list2
 *
 */



public class   MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_FILE_OPEN = 3;

    public static final String TAG = "nRFUART";
    private static final int MDEX_BLE_STATE_CONNECTED = 20;
    private static final int MDEX_BLE_STATE_DISCONNECTED = 21;

    private int m_BLE_State = MDEX_BLE_STATE_DISCONNECTED;
    private UartService m_UartService = null;
    private BluetoothDevice m_objBLE_Device = null;
    private BluetoothAdapter m_BtAdapter = null;
    //private ListView messageListView;
    //private ArrayAdapter<String> listAdapter;
    private Button m_btnBLE_ConnDisconn, m_btnBLE_TxData;

    private boolean m_isSavingOn = false;
    private boolean m_isReplayOn = false;

    ArrayList<String> m_DetailList;
    ArrayAdapter<String> m_DetailAdapter;
    ListView m_lvDetailStateList;
    
    private float m_LateralVector;

    private TextView m_tvDeviceSWMode;

    Button m_btnDevceOnLeft;
    Button m_btnDevceOnRight;
    SharedPreferences m_prefDeviceHistory;

    //  <><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>
    //
    //        MAIN ACTIVITY LIFE CYCLE DEFINITION BLOCK
    //
    //  <><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        checkPermissionSDWrite();

        m_BtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (m_BtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        m_prefDeviceHistory = getSharedPreferences("MacAddr", Activity.MODE_PRIVATE);
/*
        messageListView = (ListView) findViewById(R.id.listMessage);
        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);
*/

        UI_onCreate();

        Notify_onCreate();

        service_init();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
    	 super.onDestroy();
        Log.d(TAG, "onDestroy()");
        
        try {
        	LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        } 
        unbindService(mServiceConnection);
        m_UartService.stopSelf();
        m_UartService= null;
       
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!m_BtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
 
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (m_BLE_State == MDEX_BLE_STATE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.popup_title)
            .setMessage(R.string.popup_message)
            .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
   	                finish();
                }
            })
            .setNegativeButton(R.string.popup_no, null)
            .show();
        }
    }


    /**
     *
     * @brief To Handler
     * @details  To Create Handler
     * @param
     * @return
     * @throws
     */
    private Handler mHandler = new Handler() {
        @Override

        //Handler events that received from UART service
        public void handleMessage(Message msg) {
            Log.i(TAG, "Uart service handleMessage message= " + msg);
        }
    };

    public void checkPermissionSDWrite(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ){//Can add more as per requirement
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1203);
        }
    }

    public void checkPermission(String permission_name, int req_code){
        if (ContextCompat.checkSelfPermission(this, permission_name) != PackageManager.PERMISSION_GRANTED ){//Can add more as per requirement
            ActivityCompat.requestPermissions(this, new String[]{permission_name} , req_code);
        }
    }
    //          <><><>                                          <><><>
    //          MAIN ACTIVITY LIFE CYCLE DEFINITION BLOCK ENDS HERE
    //          <><><>                                          <><><>


    /////////////////////////////////////////////////////////////////////////
    //
    //        FUNCTION : onActivityResult
    //
    //        << class message >>
    //          REQUEST_SELECT_DEVICE : Select popup view
    //          REQUEST_ENABLE_BT : receive return of permission. Deprecated
    //          REQUEST_FILE_OPEN : receive return of File open popup view
    /////////////////////////////////////////////////////////////////////////
    Uri m_UriReplayFile = null;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    m_objBLE_Device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + m_objBLE_Device + "mserviceValue" + m_UartService);
                    ((TextView) findViewById(R.id.deviceName)).setText(m_objBLE_Device.getName()+ " - " + getResources().getString(R.string.ui_btn_ble_try_connect));

                    m_btnBLE_ConnDisconn.setText(getResources().getString(R.string.ui_btn_ble_try_connect));
                    //m_btnBLE_ConnDisconn.setText("Connecting...");

                    m_UartService.connect(deviceAddress);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_FILE_OPEN:
                if (resultCode == Activity.RESULT_OK && data != null) {

                    Uri uri = data.getData();
                    String uriRawPath = uri.getPath();

                    String strPathName = uriRawPath.substring(uriRawPath.indexOf("/storage/"));

                    String strFolderPath = strPathName.substring(0, strPathName.lastIndexOf("/") + 1);
                    Log.d("uri", "strFolderPath : " + strFolderPath);

                    String strFileName = strPathName.substring(strPathName.lastIndexOf("/") + 1);
                    Log.d("uri", "strFileName : " + strFileName);

                    mtv_PostureState.setText(strFileName);

                    //  Reference : https://howtodoinjava.com/java/library/parse-read-write-csv-opencsv/
                    try {
                        File csvfile = new File(strPathName);
                        CSVReader reader = new CSVReader(new FileReader(csvfile.getAbsolutePath()));

                        //Read all rows at once
                        m_Replay_RowsAll = reader.readAll();
//                        for(String[] row : allRows){
//                            System.out.println(Arrays.toString(row));
//                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "The specified file was not found", Toast.LENGTH_SHORT).show();
                    }

                    Log.d("csv", "lines = " + m_Replay_RowsAll.size() + ", columns = " + Arrays.stream(m_Replay_RowsAll.get(0)).count() + ", [0, 2] =" + m_Replay_RowsAll.get(0)[2]);

                    m_Replay_PacketNum = m_Replay_RowsAll.size();
                    // 만일 플레이중이라면 스톱.
                    msb_Replay_RandomAccess.setMax(m_Replay_PacketNum);
                }
                break;

            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }
    //        FUNCTION : onActivityResult ENDS HERE


    /////////////////////////////////////////////////////////////////////////
    //
    //  SERVICE AND BROADCAST RECEIVER BLOCK
    //
    /////////////////////////////////////////////////////////////////////////
    //UART service connected/disconnected
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(com.marveldex.MdexDaq.UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(com.marveldex.MdexDaq.UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(com.marveldex.MdexDaq.UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(com.marveldex.MdexDaq.UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(com.marveldex.MdexDaq.UartService.DEVICE_DOES_NOT_SUPPORT_UART);

        //      gap messages
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        //intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        return intentFilter;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            m_UartService = ((com.marveldex.MdexDaq.UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected m_UartService= " + m_UartService);
            if (!m_UartService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            if(m_UartService != null) {
                //m_UartService.disconnect(m_objBLE_Device);
                m_UartService.disconnect();
                //m_UartService = null;
            }
            //m_UartService = null;
        }
    };


    private void service_init() {
        Intent bindIntent = new Intent(this, com.marveldex.MdexDaq.UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    /**
     *
     * @brief Receive Broadcast and Check and Action each Function
     * @details  It receives data from the UartService.java file as a broadcast and performs its function through its value. Manage Bluetooth and device connection status.
     * @param
     * @return
     * @throws
     */
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(com.marveldex.MdexDaq.UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
                        Calendar cal = Calendar.getInstance();
                        String time_str = dateFormat.format(cal.getTime());

                        Log.d(TAG, "ACTION_GATT_CONNECTED " + time_str);

                        m_btnBLE_ConnDisconn.setText(getResources().getString(R.string.ui_btn_ble_disconnect));
                        //m_btnBLE_ConnDisconn.setText("Disconnect");
                        m_edtBLE_TxView.setEnabled(true);
                        m_btnBLE_TxData.setEnabled(true);

                        ((TextView) findViewById(R.id.deviceName)).setText(m_objBLE_Device.getName()+ " - " + getResources().getString(R.string.ui_btn_ble_connected));
/*
                         listAdapter.add("["+currentDateTimeString+"] Connected to: "+ m_objBLE_Device.getName());
                         messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
*/

                        //연결 완료  - 맥어드레스 저장
                        //업데이트
                        SharedPreferences.Editor editor = m_prefDeviceHistory.edit();
                        editor.putString("MacAddr",m_objBLE_Device.getAddress());
                        editor.commit();
                        m_BLE_State = MDEX_BLE_STATE_CONNECTED;
                    }
                });
            }

            //*********************//
            if (action.equals(com.marveldex.MdexDaq.UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "ACTION_GATT_DISCONNECTED");

                        m_btnBLE_ConnDisconn.setText(getResources().getString(R.string.ui_btn_ble_connect));
                        //m_btnBLE_ConnDisconn.setText("Connect");
                        m_edtBLE_TxView.setEnabled(false);
                        m_btnBLE_TxData.setEnabled(false);
                        ((TextView) findViewById(R.id.deviceName)).setText(getResources().getString(R.string.ui_btn_ble_disconnect));
/*
                             listAdapter.add("["+currentDateTimeString+"] Disconnected to: "+ m_objBLE_Device.getName());
*/
                        m_BLE_State = MDEX_BLE_STATE_DISCONNECTED;
                        m_UartService.close();
                    }
                });
            }


            //*********************//
            if (action.equals(com.marveldex.MdexDaq.UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                m_UartService.enableTXNotification();
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
            }

            //-----------------------------------------------------
            // HERE RECEIVE RAW BLE DATA AND PARSE
            //-----------------------------------------------------

            if (action.equals(com.marveldex.MdexDaq.UartService.ACTION_DATA_AVAILABLE)) {
                final byte[] packetVenus2Phone = intent.getByteArrayExtra(com.marveldex.MdexDaq.UartService.EXTRA_DATA);

                runOnUiThread(new Runnable() {
                    public void run() {
//                         try {
//
//                         	String text = new String(packetVenus2Phone, "UTF-8");
//                         	String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
//                             listAdapter.add("["+currentDateTimeString+"] RX: "+text);
//                             messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
//
//                         } catch (Exception e) {
//                             Log.e(TAG, e.toString());
//                         }

                        if(packetVenus2Phone.length < m_PacketParser.def_PACKET_LENGTH){
                            Log.d(TAG, packetVenus2Phone.toString());
                        }
                        else {
                            // receive data and parse
                            m_PacketParser.onReceiveRawPacket(packetVenus2Phone);

                            onDataProc();
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(com.marveldex.MdexDaq.UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                m_UartService.disconnect();
            }
        }
    };
    ////////    SERVICE AND BROADCAST RECEIVER BLOCK ENDS HERE   ///////////



    //  <><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>
    //
    //              USER VARIABLES, FUNCTIONS
    //
    //  <><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>


    /////////////////////////////////////////////////////////////////////////
    //
    //  FUNCTION : UI_onCreate
    //
    /////////////////////////////////////////////////////////////////////////

    //  Variables - UI Vies
    private HeatMap pressureMap;
    //ImageView miv_bgi;
    RelativeLayout rl_canvasCells;
    private EditText m_edtBLE_TxView;
    private TextView mtv_BatteryLevel;
    private TextView mtv_LastPacketTime;
    private TextView mtv_PostureState;
    
    Space msp_COM_sero;
    ImageView miv_COM_bar;
    
    TextView mTxtSavePath;
    private TextView mtv_BlindState, mtv_BlindStartTime, mtv_BlindElapsedTime;
    private TextView mtv_LeftLegCrossed, mtv_RightLegCrossed, mtv_LongitudinalVector, mtv_LateralVector;

    //  Variables - Raw data
    private TextView [] mtv_Cells = new TextView[PacketParser.def_CUSTOM_SENSOR_NUM];

    float [] cellPos_X = new float[PacketParser.def_CUSTOM_SENSOR_NUM];
    float [] cellPos_Y = new float[PacketParser.def_CUSTOM_SENSOR_NUM];

    PacketParser m_PacketParser = new PacketParser();


    //  Variables - Posture tag and time
    enum POSTURE_tag{
        POSTURE_NO_LOG,
        POSTURE_LEFT_BAD,
        POSTURE_LEFT,
        POSTURE_CENTER,
        POSTURE_RIGHT,
        POSTURE_RIGHT_BAD
    }
    POSTURE_tag m_PostureState;
    long m_PostureOriginTimeMS = elapsedRealtime();


    //  Variables - Save CSV
    Button mSave_Start;
    private boolean m_SaveFlag = false;
    Button mbt_PlayPause;
    SeekBar msb_Replay_RandomAccess;

    private String m_PositionCsv = null;
    Map<String, Object> m_HashMap = null;

    /**
     *
     * @brief App의 화면의 컨트롤들 선언
     * @details App의 화면을 구성하는 컨트롤들을 불러와 각 변수에 선언하여 사용할수 있게 셌팅한다.
     * @param
     * @return void
     * @throws
     */
    private void UI_onCreate(){
        initHeatmap();
        m_PacketParser.setThreshold_Wheelopia(1, 100); // 100 100 170 180
        m_PacketParser.setThreshold_Wheelopia(2, 100);
        m_PacketParser.setThreshold_Wheelopia(3, 170);
        m_PacketParser.setThreshold_Wheelopia(4, 180);
        m_PacketParser.setThreshold_Wheelopia(5, 100);


        rl_canvasCells = (RelativeLayout)findViewById(R.id.cellLayer);
        //miv_bgi = (ImageView)findViewById(R.id.iv_backimage);
        mtv_BatteryLevel = (TextView) findViewById(R.id.TV_BatteryLevel);
        mtv_LastPacketTime = (TextView)findViewById(R.id.TV_CurTime);

        mtv_BlindState = (TextView)findViewById(R.id.TV_BLIND_STATE);
        mtv_BlindStartTime = (TextView)findViewById(R.id.TV_BLIND_TIME_START);
        mtv_BlindElapsedTime = (TextView)findViewById(R.id.TV_BLIND_TIME_ELAPSED);
        mtv_LeftLegCrossed = (TextView)findViewById(R.id.TV_LEFT_LEG_CROSSED);
        mtv_RightLegCrossed = (TextView)findViewById(R.id.TV_RIGHT_LEG_CROSSED);
        mtv_LongitudinalVector = (TextView)findViewById(R.id.TV_LONGITUDINAL_VECTOR);
        mtv_LateralVector = (TextView)findViewById(R.id.TV_LATERAL_VECTOR);

        m_btnBLE_ConnDisconn=(Button) findViewById(R.id.btn_select);

        m_btnBLE_ConnDisconn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!m_BtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    //Connect button pressed
                    if (m_btnBLE_ConnDisconn.getText().equals(getResources().getString(R.string.ui_btn_ble_connect))){
                        //if (m_btnBLE_ConnDisconn.getText().equals("Connect")){

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(MainActivity.this, com.marveldex.MdexDaq.DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);

                        if(m_isReplayOn == true)
                            Replay_ForceCancel();
                    }
                    // Disconnect button pressed
                    else {
                        if (m_objBLE_Device!=null){
                            m_UartService.disconnect();
                            {
                                m_btnBLE_ConnDisconn.setText(getResources().getString(R.string.ui_btn_ble_connect));
                                //m_btnBLE_ConnDisconn.setText("Connect");

                                m_edtBLE_TxView.setEnabled(false);
                                m_btnBLE_TxData.setEnabled(false);
                                ((TextView) findViewById(R.id.deviceName)).setText(getResources().getString(R.string.ui_device_name_none));
                                m_BLE_State = MDEX_BLE_STATE_DISCONNECTED;
                                m_UartService.close();

                                mtv_BlindState.setText(getResources().getString(R.string.ui_device_name_none));
                            }
                        }
                    }
                }
            } // end of onClick
        });


        m_edtBLE_TxView = (EditText) findViewById(R.id.sendText);
        m_btnBLE_TxData=(Button) findViewById(R.id.sendButton);

        m_btnDevceOnLeft = (Button)findViewById(R.id.Left_On_Off);
        m_btnDevceOnRight = (Button)findViewById(R.id.Right_On_Off);

        // Handle Send button
        m_btnBLE_TxData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.sendText);
                String message = editText.getText().toString();
                byte[] value;
                try {
                    //send data to service
                    value = message.getBytes("UTF-8");
                    m_UartService.writeRXCharacteristic(value);
/*
					//Update the log with time stamp
					String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
					listAdapter.add("["+currentDateTimeString+"] TX: "+ message);
		               	 	messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
*/
                    m_edtBLE_TxView.setText("");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // Set initial UI state
        m_btnDevceOnLeft.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String message;
                message = "1";
                //message = "turn off left";

                byte[] value;
                try {
                    //send data to service
                    value = message.getBytes("UTF-8");
                    m_UartService.writeRXCharacteristic(value);
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });

        m_btnDevceOnRight.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String message;
                message = "2";

//                String msg_led[] = new String[2];
//                msg_led[0] = "turn on left";
//                msg_led[0] = "turn off left";
//                msg_led[1] = "turn on right";
//                msg_led[1] = "turn off right";
//                String send_msg = msg_led[0];
//                value = send_msg.getBytes("UTF-8");

                byte[] value;
                try {
                    //send data to service
                    value = message.getBytes("UTF-8");
                    m_UartService.writeRXCharacteristic(value);
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });

        int cell_index = 0;
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_00);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_01);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_02);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_03);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_04);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_05);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_06);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_07);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_08);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_09);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_10);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_11);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_12);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_13);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_14);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_15);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_16);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_17);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_18);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_19);
        mtv_Cells[cell_index++] =(TextView)findViewById(R.id.TV_CELL_VAL_20);



        mtv_BatteryLevel.setText(String.format(getString(R.string.fmt_battery_level), 0));

        mtv_PostureState = (TextView)findViewById(R.id.TV_POSTURE_LATERAL );
        miv_COM_bar = (ImageView)findViewById(R.id.iv_COM_TARGET);

        m_PostureState = POSTURE_tag.POSTURE_NO_LOG;

        mSave_Start = (Button)findViewById(R.id.Save_Start);
        mTxtSavePath = (TextView)findViewById(R.id.TV_FILEPATH);
        m_tvDeviceSWMode = (TextView)findViewById(R.id.MODE_INFO);



        mbt_PlayPause = (Button)findViewById(R.id.REPLAY_PLAY_PAUSE);
        msb_Replay_RandomAccess = (SeekBar)findViewById(R.id.REPLAY_SEEK_BAR);
        msb_Replay_RandomAccess.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                mtv_PostureState.setText("onStop TrackingTouch");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                mtv_PostureState.setText("onStart TrackingTouch");
            }

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mtv_PostureState.setText("onProgressChanged : " + progress);
            }
        });
        msb_Replay_RandomAccess.setProgress(1);
        msb_Replay_RandomAccess.setMax(100);


        //출처: https://bitsoul.tistory.com/29 [Happy Programmer~]

        m_ConnectionMonitorTimer.schedule(new com.marveldex.MdexDaq.MainActivity.TimerTask_ConnectionMonitor(), 500,TIMER_PERIOD_MONITOR);
    }
    ////////    UI_onCreate ENDS HERE   /////////////////////////////////////


    /////////////////////////////////////////////////////////////////////////
    //
    //  BLE CHECK BLIND AND AUTO RECONNECT BLOCK
    //
    /////////////////////////////////////////////////////////////////////////
    private Timer m_ConnectionMonitorTimer  = new Timer();
    private final int TIMER_PERIOD_MONITOR  = 1000; // 1000 : 1 sec
    int m_Blind_ElapsedTime = 0;
    int m_Blind_ElapsedTime_Last= 0;
    private static final int THRESHOLD_BLIND_SEC = 3;  // THRESHOLD_BLIND_SEC : threshold to decide blind state
    private static final int CONN_STATE_DISCONNECT = 0;
    private static final int CONN_STATE_CONNECT_OK = 1;
    private static final int CONN_STATE_CONNECT_BLIND = 2;
    int m_AutoConn_State = CONN_STATE_DISCONNECT;

    private class TimerTask_ConnectionMonitor extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(m_BLE_State == MDEX_BLE_STATE_DISCONNECTED) {
                        m_AutoConn_State = CONN_STATE_DISCONNECT;
                        if(com.marveldex.MdexDaq.UartService.getIsDisconnIntentional() == false) {
                            //  This disconnect message is...
                            //  not occurred by device disconnect request. (Device disconnect doesn't make disconnect message in phone)
                            //  not occurred by phone disconnect request. (Phone disconnect request make UartService.m_is_Disconnect_Intentional flag be true)
                            //  occurred only when this application is just launched. So reconnection needs device mac info to connect.
                        }
                        return;
                    }

                    //  case (m_BLE_State == MDEX_BLE_STATE_CONNECTED)
                    m_Blind_ElapsedTime++;
                    if(THRESHOLD_BLIND_SEC <= m_Blind_ElapsedTime) {
                        m_Blind_ElapsedTime_Last = m_Blind_ElapsedTime;
                        //  first time
                        if(m_Blind_ElapsedTime_Last == THRESHOLD_BLIND_SEC) {
                            m_AutoConn_State = CONN_STATE_CONNECT_BLIND;

                            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
                            Calendar cal = Calendar.getInstance();
                            String time_str = dateFormat.format(cal.getTime());
                            mtv_BlindStartTime.setText("Last blind : " + time_str);

                            mtv_BlindState.setText(getResources().getString(R.string.ui_ble_state_blind));
                            //mtv_BlindState.setText("State : Blind");
                        }
                        mtv_BlindElapsedTime.setText(m_Blind_ElapsedTime_Last + " sec");
                    }
                    else {
                        return;
                    }
                }
            });
        }
    }
    ////////    BLE CHECK BLIND AND AUTO RECONNECT BLOCK ENDS HERE   ////////



    //-------------------------------------------------------------------------
    //  Data display
    //-------------------------------------------------------------------------
    private void onDataProc() {
        if(m_PacketParser.isHaveAllData() == false)
            return;

        // update sensor data to TextView
        UI_updateSensorValue();

        // draw center of mass image
        UI_updateStateCOMNoti();

        // save CSV file
        UI_saveCSVProc();

    };

    /////////////////////////////////////////////////////////////////////////
    //
    //  SET SENSOR VALUE AND CALL PRSSURE MAP BLOCK
    //
    /////////////////////////////////////////////////////////////////////////
    private void UI_updateSensorValue(){

        //mode check  M,S --> Venus   L,R --> Seat
        //Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.ui_dip_switch_toast), Toast.LENGTH_SHORT);
        //toast.show();
        //toast.cancel();
        //m_tvDeviceSWMode.setText(getString(R.string.ui_dip_switch_txt));

        // last time packet received
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            String time_str = dateFormat.format(cal.getTime());
            String last_packet_timestamp = getString(R.string.fmt_time_last_packet) + time_str;
            //mtv_LastPacketTime.setText(getString(R.string.fmt_time_last_packet) + time_str);

            m_AutoConn_State = CONN_STATE_CONNECT_OK;
            m_Blind_ElapsedTime = 0;

            String sRecvMsg = getResources().getString(R.string.ui_ble_state_recv);
            sRecvMsg += ("...   " + last_packet_timestamp);
            mtv_BlindState.setText(sRecvMsg);
//            mtv_BlindState.setText(getResources().getString(R.string.ui_ble_state_recv));
            //mtv_BlindState.setText("State : Receiving");
        }

        //  UI - battery level
        mtv_BatteryLevel.setText(String.format(getString(R.string.fmt_battery_level), PacketParser.getBatteryLevel()));

        boolean is_draw_text_value = true;
        boolean is_draw_text_color = false;
        boolean is_draw_pressure_map = true;
        //  UI - cell data and color
        {
            int nSensorValue = 0;
            int cell_index = 0;
            int row_index = 0;

            for (cell_index = 0 ; cell_index < PacketParser.def_CUSTOM_SENSOR_NUM ; cell_index++){
                nSensorValue = m_PacketParser.getCustomSensorData(cell_index);

                if(is_draw_text_value)
                    mtv_Cells[cell_index].setText(String.format("%d", nSensorValue));

                if(is_draw_text_color)
                    mtv_Cells[cell_index].setBackgroundColor(0x00FF0000 | (nSensorValue << 24) );
            }
        }

        //  UI - update pressureMap
        if(is_draw_pressure_map) {
            drawNewMap();
            pressureMap.forceRefresh();
        }

    }
    ////////    SET SENSOR VALUE AND CALL PRSSURE MAP BLOCK ENDS HERE   /////


    /////////////////////////////////////////////////////////////////////////
    //
    //  HEATMAP BLOCK
    //
    /////////////////////////////////////////////////////////////////////////

    private void initHeatmap() {

        pressureMap = findViewById(R.id.heatmapCushion);
        pressureMap.setMinimum(0.0);
        pressureMap.setMaximum(100.0);
        pressureMap.setLeftPadding(100);
        pressureMap.setRightPadding(100);
        pressureMap.setTopPadding(100);
        pressureMap.setBottomPadding(100);
        pressureMap.setMarkerCallback(new HeatMapMarkerCallback.CircleHeatMapMarker(0xff9400D3));
        pressureMap.setRadius(80.0);
        Map<Float, Integer> colors = new ArrayMap<>();
        //build a color gradient in HSV from red at the center to green at the outside
/*
        for (int i = 0; i < 21; i++) {
            float stop = ((float)i) / 20.0f;
            int color = doGradient(i * 5, 0, 100, 0xff00ff00, 0xffff0000); // FFRRGGBB
            colors.put(stop, color);
        }
        pressureMap.setColorStops(colors);
*/

        final int color_step = 100;
        final int area_width = 25;
        final int [] area_value = new int [] {0, 20, 40, 60, 100};
        int area_min, area_max;

        //  area  0 ~ 20 : Blue (0000FF) ~ Cyan (00FFFF)
        //  area 20 ~ 40 : Cyan (00FFFF) ~ Green (00FF00)
        //  area 40 ~ 60 : Green (00FF00) ~ Yellow (FFFF00)
        //  area 60 ~ 100: Yellow (FFFF00) ~ Red (FF0000)

        for (int i = 0; i < color_step + 1; i++) {
            int area = i / area_width;

            int color_head = 0;
            int color_tail = 0;

            if(area == 0) {
                area_min = area_value[area];
                area_max = area_value[area + 1];

                color_head = 0xFF0000FF; // Blue
                color_tail = 0xFF00FFFF; // Cyan
            }
            else if(area == 1) {
                area_min = 0;//area_value[area];
                area_max = 20;//area_value[area + 1];

                color_head = 0xFF00FFFF; // Cyan
                color_tail = 0xFF00FF00; // Green
            }
            else if(area == 2) {
                area_min = 0;//area_value[area];
                area_max = 20;//area_value[area + 1];

                color_head = 0xFF00FF00; // Green
                color_tail = 0xFFFFFF00; // Yellow
            }
            else {
                area_min = area_value[3];
                area_max = area_value[4];

                color_head = 0xFFFFFF00; // Yellow
                color_tail = 0xFFFF0000; // Red
            }

            if(i == 0) {
                area_min = 0;
                area_max = 1;

                color_head = 0xFFFFFFFF; // white
                color_tail = 0xFFFFFFFF; // white
            }


            float stop = ((float)i) / (float)color_step;
            int color = doGradient(i , area_min, area_max, color_head, color_tail); // FFRRGGBB
            colors.put(stop, color);
        }

        pressureMap.setColorStops(colors);
        //make the minimum opacity completely transparent
        //pressureMap.setMinimumOpactity(0);
        //make the maximum opacity 50% transparent
        pressureMap.setMaximumOpacity(125);
        //pressureMap.setMaximumOpacity(250);
        pressureMap.setRadius(1800);

        pressureMap.setMarkerCallback(null);
    }

    @SuppressWarnings("SameParameterValue")
    private static int doGradient(double value, double min, double max, int min_color, int max_color) {
        if (max <= value) {
            return max_color;
        }
        if (value <= min) {
            return min_color;
        }
        float[] hsvmin = new float[3];
        float[] hsvmax = new float[3];
        Color.RGBToHSV(Color.red(min_color), Color.green(min_color), Color.blue(min_color), hsvmin);
        Color.RGBToHSV(Color.red(max_color), Color.green(max_color), Color.blue(max_color), hsvmax);

        float frac = (float)((value - min) / (max - min));
        float[] retval = new float[3];
        for (int i = 0; i < 3; i++) {
            retval[i] = interpolate(hsvmin[i], hsvmax[i], frac);
        }
        return Color.HSVToColor(retval);
    }


    @AnyThread
    private void drawNewMap() {
        pressureMap.clearData();
//        Random rand = new Random();

//                                   //  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12
//        float [] pos_x = new float[] {17, 30, 83, 70, 8F, 92, 30, 50, 70, 50, 17, 83};
//        float [] pos_y = new float[] {42, 52, 42, 52, 74, 74, 98, 98, 98, 74, 15, 15};

        // 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12
        float [] pos_x = new float[] { 0, 17, 34, 50, 67, 84, 100, 0, 17, 34, 50, 67, 84, 100, 0, 17, 34, 50, 67, 84, 100};
        float [] pos_y = new float[] { 5, 5, 5, 5, 5, 5, 5, 52, 52, 52, 52, 52, 52, 52, 105, 105, 105, 105, 105, 105, 105};

//  8 ~ 92, 2~98 div 7 = (98 -2)/7  X: { 2, 9, 16, 23, 30, 37, 44, 2, 9, 16, 23, 30, 37, 44, 2, 9, 16, 23, 30, 37, 44 }
// 42 ~ 98,42~98 div 3 = (98-52)/3  Y: { 52, 52, 52, 52, 52, 52, 52, 74, 74, 74, 74, 74, 74, 74, 98, 98, 98, 98, 98, 98, 98}

        int fake_value10, fake_value11;

        //add 20 random points of random intensity
        for (int i = 0; i < PacketParser.def_CUSTOM_SENSOR_NUM; i++) { // 20 : number of points
            //  HeatMap.DataPoint(x, y, value);
            int value = PacketParser.getCustomCellValue1(i);
            if ( i == 0 || i == 20) {
                //value = 200;
            }
            HeatMap.DataPoint point = new HeatMap.DataPoint(pos_x[i]/100, pos_y[i]/100, value);

            pressureMap.addData(point);
        }

//        fake_value10 = PacketParser.nDataWheelopia[0];
//        HeatMap.DataPoint fake_point10 = new HeatMap.DataPoint(pos_x[10]/100, pos_y[10]/100, fake_value10);
//        pressureMap.addData(fake_point10);
//
//        fake_value11 = PacketParser.nDataWheelopia[2];
//        HeatMap.DataPoint fake_point11 = new HeatMap.DataPoint(pos_x[11]/100, pos_y[11]/100, fake_value11);
//        pressureMap.addData(fake_point11);
    }



    @SuppressWarnings("SameParameterValue")
    private float clamp(float value, float min, float max) {
        return value * (max - min) + min;
    }

    @SuppressWarnings("SameParameterValue")
    private double clamp(double value, double min, double max) {
        return value * (max - min) + min;
    }

    private static float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }
    //////////////   HEATMAP BLOCK ENDS HERE   ////////////////////


    /////////////////////////////////////////////////////////////////////////
    //
    //  UPDATE STATE, COM, NOTI BLOCK
    //
    /////////////////////////////////////////////////////////////////////////

    /**
     * @brief   현재의 자세를 태그로 저장한다.
     * @detail  추가로 현재의 자세를 취한 시점의 시간을 저장한다. 이 값은 현재의 자세를 유지할 경우, 몇초간 유지하고 있는 지를 UI에서 보여줄 때 사용된다.
     * @param
     * @return  void
     */
    private boolean updateDetermiState(Determi_Wheelopia_tag determi_state) {
        boolean is_changed = false;
        if (m_DetermiState != determi_state) {
            m_DetermiOccurTimeMS = elapsedRealtime();
            is_changed = true;
        }
        m_DetermiState = determi_state;

        return is_changed;
    }

    /**
     * @brief   현재의 자세 태그를 반환한다.
     * @param
     * @return  자세 태그 (POSTURE_tag enum)
     */
    private Determi_Wheelopia_tag getDetermiState(){
        return m_DetermiState;
    }

    /**
     * @brief   현재의 자세를 취한 시간 길이를 측정하여 반환한다.
     * @param
     * @return  시간 (초 단위)
     */
    private long getDetermiElapsedSecond(){
        return (elapsedRealtime() - m_DetermiOccurTimeMS) / 1000;
    }




    //  Variables - Posture tag and time
    enum Determi_Wheelopia_tag{
        DETERMI_NO_DATA,
        DETERMI_NO_PROBLEM, // NOT DETERMI_1 ~ 5
        DETERMI_1, // if ( (#6 + #9 + 40) < (#5 + #7) ) , then determination "왼쪽 쏠림"
        DETERMI_2, // if ( (#5 + #7 + 40) < (#6 + #9) ) , then determination "오른쪽 쏠림"
        DETERMI_3, // if ( (#10 + 50) < (#2 + #4) ) , then determination "부적절한 자세"
        DETERMI_4, // if ( (#10 + 50) < (#1 + #3) ) , then determination "부적절한 자세"
        DETERMI_5  // if ( (#10 + 50) < (#7 + #8 + #9) ) , then determination "부적절한 자세"
    };
    Determi_Wheelopia_tag m_DetermiState;
    private String m_txtDetermi;
    long m_DetermiOccurTimeMS = elapsedRealtime();



    private Determi_Wheelopia_tag Determination_Wheelopia() {
        if(PacketParser.isSeatOccupied() == false) {
            m_txtDetermi = "착석 상태가 아닙니다.";
            return Determi_Wheelopia_tag.DETERMI_NO_DATA;
        }

        int [] threshold = new int [PacketParser.determiCount + 1]; // 1 based array. 0 : not used, 1 ~ 5 : used, threshold number
        int [] cell_value1 = new int [PacketParser.def_CELL_COUNT_WHEELOPIA + 1]; // 1 based array. 0 : not used, 1 ~ 10 : used, sensor number.

        //  get threshold 1 ~ 5 (0 is empty)
        for(int i = 1 ; i < PacketParser.determiCount + 1 ; i++) {
            threshold[i] = PacketParser.determiValues1B[i];
        }

        //  get cell values 1 ~ 10 (0 is empty)
        for(int i = 1 ; i < PacketParser.def_CELL_COUNT_WHEELOPIA + 1 ; i++) {
            cell_value1[i] = PacketParser.cellValue1(i);
        }

        // 판별식 1, if ( (#3 + #4 + #6 + 100) < (#1 + #2 + #5) ) ,
        if( (cell_value1[3] + cell_value1[4] + cell_value1[6] + 100) < ( cell_value1[1] + cell_value1[2] + cell_value1[5] ) ) {
            m_txtDetermi = getResources().getString(R.string.ui_det_msg_1);
            return Determi_Wheelopia_tag.DETERMI_1;
        }

        // 판별식 2, if ( (#1 + #2 + #5 + 100) < (#3 + #4 + #6) ) ,
        if( (cell_value1[1] + cell_value1[2] + cell_value1[5] + 100) < ( cell_value1[3] + cell_value1[4] + cell_value1[6] ) ) {
            m_txtDetermi = getResources().getString(R.string.ui_det_msg_2);
            return Determi_Wheelopia_tag.DETERMI_2;
        }

        // 판별식 3, if ( (#10 + 170) < (#7 + #8 + #9) ) ,
        if( (cell_value1[10] + 170) < ( cell_value1[7] + cell_value1[8] + cell_value1[9] ) ) {
            m_txtDetermi = getResources().getString(R.string.ui_det_msg_3);
            return Determi_Wheelopia_tag.DETERMI_3;
        }

        // 판별식 4, if ( (#10 + 200) < (#1 + #2 + #3 + #4) ) ,
        if( (cell_value1[10] + 180) < ( cell_value1[1] + cell_value1[2] + cell_value1[3] + cell_value1[4] ) ) {
            m_txtDetermi = getResources().getString(R.string.ui_det_msg_4);
            return Determi_Wheelopia_tag.DETERMI_4;
        }

/*
        // 판별식 1, if ( (#6 + #9 + 40) < (#5 + #7) ) , then determination "왼쪽 쏠림"
        if( (cell_value1[6] + cell_value1[9] + threshold[1]) < ( cell_value1[5] + cell_value1[7] ) ) {
            m_txtDetermi = "왼쪽 쏠림";
            return Determi_Wheelopia_tag.DETERMI_1;
        }

        // 판별식 2, if ( (#5 + #7 + 40) < (#6 + #9) ) , then determination "오른쪽 쏠림"
        if( (cell_value1[5] + cell_value1[7] + threshold[2]) < ( cell_value1[6] + cell_value1[9] ) ) {
            m_txtDetermi = "오른쪽 쏠림";
            return Determi_Wheelopia_tag.DETERMI_2;
        }

        // 판별식 3, if ( (#10 + 50) < (#2 + #4) ) , then determination "부적절한 자세"
        if( (cell_value1[10] + threshold[3]) < ( cell_value1[2] + cell_value1[4] ) ) {
            m_txtDetermi = "부적절한 자세(3).";
            return Determi_Wheelopia_tag.DETERMI_3;
        }

        // 판별식 4, if ( (#10 + 50) < (#1 + #3) ) , then determination "부적절한 자세"
        if( (cell_value1[10] + threshold[4]) < ( cell_value1[1] + cell_value1[3] ) ) {
            m_txtDetermi = "부적절한 자세(4)";
            return Determi_Wheelopia_tag.DETERMI_4;
        }

        // 판별식 5, if ( (#10 + 50) < (#7 + #8 + #9) ) , then determination "부적절한 자세"
        if( (cell_value1[10] + threshold[5]) < ( cell_value1[7] + cell_value1[8] + cell_value1[9] ) ) {
            m_txtDetermi = "부적절한 자세(5)";
            return Determi_Wheelopia_tag.DETERMI_5;
        }
*/
        m_txtDetermi = "  ";
        return Determi_Wheelopia_tag.DETERMI_NO_PROBLEM;
    }


    private void updateCellPos() {

    }


    /**
     * @brief UI 화면에 무게 중심을 표시해주는 함수(COM : Center of Mess)
     * @details 센서에서 측정한 압력 값으로 COM 값을 계산한다. 이를 이미지로 화면에 뿌려준다.
     * @param
     * @return
     * @throws
     */
    private void UI_updateStateCOMNoti(){
        setCOM_Img();

        Determi_Wheelopia_tag   determi_state = Determi_Wheelopia_tag.DETERMI_NO_DATA;//Determination_Wheelopia();

        //  set text
        //String strMsg = String.format(getString(R.string.fmt_determi_msg), m_txtDetermi, getDetermiElapsedSecond());
        //mtv_PostureState.setText(strMsg);
        mtv_PostureState.setText(m_txtDetermi);

        //  make notification
//        boolean is_sent = UI_updateNotiManager(determi_state);
//        if(is_sent) {
//            //Toast toast = Toast.makeText(getApplicationContext(), "sent notification...", Toast.LENGTH_SHORT);
//            //toast.show();
//        }

        if(m_PacketParser.isSeatOccupied() == false){
            //UI_LEDWORK(0, 1);

            //  reset list image
            m_LateralVector = 0.0f;
            m_DetailList = new ArrayList<String>();

            //m_DetailList.add(getString(R.string.ui_state_vector) + "0");
            m_DetailList.add(getString(R.string.ui_state_com) + "0");
            //m_DetailList.add(getString(R.string.ui_state_coc) + "0");
            m_DetailAdapter = new ResultListAdapter(this, 0, m_DetailList);

            m_lvDetailStateList = (ListView)findViewById(R.id.TV_SEAT_LOG);
            m_lvDetailStateList.setAdapter(m_DetailAdapter);

            return;
        }


        // UI_updateStateTable(proportion_com_x, proportion_coc_left,proportion_coc_right, m_LateralVector );

    }
    //////////////   UPDATE STATE, COM, NOTI ENDS HERE   ////////////////////


    /////////////////////////////////////////////////////////////////////////
    //
    //  CALC COM COORD BLOCK
    //
    /////////////////////////////////////////////////////////////////////////

    private Point m_COM_Coord = new Point(0,0);

    private void setCOM_Img() {
        int summation_mass = 0;
        float mean_x_position = 0;
        float mean_y_position = 0;
        float summation_mass_x_position = 0;
        float summation_mass_y_position = 0;
        float com_pos_x, com_pos_y;

        int [] cell_pos = new int[2];


        //  get the position of all cells in phone screen.
        for(int cell_index = 0 ; cell_index < PacketParser.def_CUSTOM_SENSOR_NUM ; cell_index++) {
            mtv_Cells[cell_index].getLocationOnScreen(cell_pos);

            cellPos_X[cell_index] = cell_pos[0]; // x
            cellPos_Y[cell_index] = cell_pos[1]; // y
        }


        //  get center position of all cells
        for(int cell_index = 0 ; cell_index < PacketParser.def_CUSTOM_SENSOR_NUM ; cell_index++) {
            mean_x_position += cellPos_X[cell_index];
            mean_y_position += cellPos_Y[cell_index];
        }
        mean_x_position /= PacketParser.def_CUSTOM_SENSOR_NUM;
        mean_y_position /= PacketParser.def_CUSTOM_SENSOR_NUM;


        //  calc summation of mass with weighting of position
        for(int i = 0 ; i < PacketParser.def_CUSTOM_SENSOR_NUM ; i++) {
            summation_mass_x_position += (PacketParser.customSensorDataAll[i] * cellPos_X[i]);
            summation_mass_y_position += (PacketParser.customSensorDataAll[i] * cellPos_Y[i]);

            summation_mass += PacketParser.customSensorDataAll[i];
        }

        //  if not occupied
        if(summation_mass < PacketParser.def_THRESHOLD_VALUE_SEAT_OCCUPIED) {
            com_pos_x = mean_x_position;
            com_pos_y = mean_y_position;
        }
        //  if occupied
        else {
            //  calc center of mass
            com_pos_x = summation_mass_x_position / summation_mass;
            com_pos_y = summation_mass_y_position / summation_mass;
        }

        rl_canvasCells.getLocationOnScreen(cell_pos);
        m_COM_Coord.x = (int)com_pos_x;
        m_COM_Coord.y = (int)com_pos_y - cell_pos[1];

        miv_COM_bar.setX(m_COM_Coord.x);
        miv_COM_bar.setY(m_COM_Coord.y);

        //Log.d("COM", "COM : " + m_COM_Coord.x + ", " + m_COM_Coord.y);

    }
    //////////////   CALC COM COORD BLOCK ENDS HERE   ///////////////////////



    /////////////////////////////////////////////////////////////////////////
    //
    //  NOTIFY MANAGER BLOCK
    //
    /////////////////////////////////////////////////////////////////////////

    NotificationManager m_NotiManager;
    public static int m_NotiID = 123888;
    NotificationCompat.Builder m_NotiBuilder;
    public static final String m_NotiChannelId = "WheelopiaCh0";
    public static final String m_NotiChannelName = "Wheelopia NotiCh";

    private void Notify_onCreate() {
        m_NotiManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            String channelDesc = "User posture";

            NotificationChannel channel = new NotificationChannel(m_NotiChannelId, m_NotiChannelName, NotificationManager.IMPORTANCE_DEFAULT);

            channel.setDescription(channelDesc);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[] {100, 200, 300});

            m_NotiManager.createNotificationChannel(channel);
            m_NotiBuilder = new NotificationCompat.Builder(this, m_NotiChannelId);
        }
        else{
            m_NotiBuilder = new NotificationCompat.Builder(this);
        }

        Bitmap largeIcon= BitmapFactory.decodeResource(getResources(), R.drawable.vector_big_left);
        m_NotiBuilder.setSmallIcon(R.drawable.noti_small_icon)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getString(R.string.noti_title))
                .setContentText("Posture monitoring...")
                .setLargeIcon(largeIcon)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);


        Intent intent=new Intent( getApplicationContext(), MainActivity.class);
        //PendingIntent pendingIntent=PendingIntent.getActivity(this, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent0 = PendingIntent.getActivity(MainActivity.this, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        m_NotiBuilder.setContentIntent(pendingIntent0);
/*
        Intent intent2 =  new Intent(this, NotiReceiver.class);
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
        m_NotiBuilder.addAction(new NotificationCompat.Action.Builder(R.drawable.noti_small_icon, "ACTION2", pendingIntent2).build()  );
*/
    }

    public class NotiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast toast = Toast.makeText(context, "I'm Noti receiver", Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    Determi_Wheelopia_tag m_lastNotiState = Determi_Wheelopia_tag.DETERMI_NO_DATA;
    long defNotiIntervalMin = (180 * 1000); // 노티 발생 이후 180초간은 노티 발생시키지 않음. 31초째부터 노티를 발생
    long m_LastNotiTimeMS = 0;

    private Determi_Wheelopia_tag getLastNotiState() {
        return m_lastNotiState;
    }

    private void setLastNotiTimeMS(long timeStampMS) {
        m_LastNotiTimeMS = timeStampMS;
    }

    private long getLastNotiElapsedMS() {
        return (elapsedRealtime() - m_LastNotiTimeMS) ;
    }

    private boolean UI_updateNotiManager(Determi_Wheelopia_tag state) {
        //  1. if state is changed? ==> YES
        //  2. if this state is same to the last state? ==> NO
        //  3. check minimum time interval ==> Over defNotiIntervalMin

        boolean is_state_changed = updateDetermiState(state);
        if(is_state_changed == false){
            return false;
        }

        Bitmap largeIcon= BitmapFactory.decodeResource(getResources(), R.drawable.center_of_contuor); // center_of_contuor : 'X'

        switch (state) {
            case DETERMI_NO_PROBLEM:
            case DETERMI_NO_DATA:
                return false;
            case DETERMI_1:
                largeIcon= BitmapFactory.decodeResource(getResources(), R.drawable.vector_big_left);
                break;
            case DETERMI_2:
                largeIcon= BitmapFactory.decodeResource(getResources(), R.drawable.vector_big_right);
                break;
            case DETERMI_3:
                largeIcon= BitmapFactory.decodeResource(getResources(), R.drawable.vector_big_back);
                break;
            case DETERMI_4:
                largeIcon= BitmapFactory.decodeResource(getResources(), R.drawable.vector_big_front);
                break;
            case DETERMI_5:
                largeIcon= BitmapFactory.decodeResource(getResources(), R.drawable.nrfuart_hdpi_icon);
                break;
        }

        Log.d("NotiManager", "Noti : " + m_txtDetermi + " ==> " + getLastNotiElapsedMS()/1000 + "sec");

        //if((2 * 1000) < getLastNotiElapsedMS()) {
        if(defNotiIntervalMin < getLastNotiElapsedMS()) {
            //String strMsg = String.format(getString(R.string.fmt_notify_msg), m_txtDetermi);
            String strMsg = m_txtDetermi;
            m_NotiBuilder.setContentText(strMsg).setLargeIcon(largeIcon);

            m_NotiManager.notify(m_NotiID, m_NotiBuilder.build());
            m_lastNotiState = state;

            setLastNotiTimeMS(elapsedRealtime());

            Log.d("NotiManager", "Noti Fire : " + m_txtDetermi + " ==> " + getLastNotiElapsedMS()/1000 + "sec");

            return true;
        }

        return false;
    }
    //////////////   NOTIFY MANAGER BLOCK ENDS HERE   ///////////////////////


    /////////////////////////////////////////////////////////////////////////
    //
    //  STATE TABLE VIEW BLOCK
    //
    /////////////////////////////////////////////////////////////////////////
    private void UI_updateStateTable(float coord_com, float coord_coc_left, float coord_coc_right, float coord_lateral_vector){

        //contour, left edge : -7, right edge : 7\n center of contour : 0 \n center of mass : 0 \n lateral m_LateralVector : 0.0"/>
        m_DetailList = new ArrayList<String>();

        m_DetailList.add(getString(R.string.ui_state_vector) + " : " + String.format("%1.3f", coord_lateral_vector));
        m_DetailList.add(getString(R.string.ui_state_com) + String.format("%1.3f",coord_com));
        m_DetailList.add(getString(R.string.ui_state_coc) + String.format("%1.3f",((coord_coc_left + coord_coc_right)/2)));
        m_DetailAdapter = new ResultListAdapter(this, 0, m_DetailList);

        m_lvDetailStateList = (ListView)findViewById(R.id.TV_SEAT_LOG);
        m_lvDetailStateList.setAdapter(m_DetailAdapter);
    }

    private class ResultListAdapter extends ArrayAdapter<String>{

        public ResultListAdapter(Context context, int textViewResourceId, ArrayList<String> objects) {
            super(context, textViewResourceId, objects);
            //this.m_DetailList = objects;
        }
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.state_list, null);
            }

            // ImageView 인스턴스
            ImageView imageView = (ImageView)v.findViewById(R.id.itemImage);

            // 리스트뷰의 아이템에 이미지를 변경한다.
            switch (position) {
                case 0: // List 0 : Laterl vector
                    if(  - 0.7< m_LateralVector && m_LateralVector < -0.3){
                        imageView.setImageResource(R.drawable.vector_left);
                    }else if(m_LateralVector <= - 0.7){
                        imageView.setImageResource(R.drawable.vector_big_left);
                    }else if( 0.3 < m_LateralVector && m_LateralVector < 0.7 ){
                        imageView.setImageResource(R.drawable.vector_right);
                    }else if(m_LateralVector >= 0.7){
                        imageView.setImageResource(R.drawable.vector_big_right);
                    }else{
                        imageView.setImageResource(R.drawable.vector_center);
                    }                    break;
                case 1: // List 1 : COM
                    imageView.setImageResource(R.drawable.com);
                    break;
                case 2: // List 2 : COC
                    imageView.setImageResource(R.drawable.coc);
                    break;
            }

            TextView textView = (TextView)v.findViewById(R.id.itemText);
            textView.setText(m_DetailList.get(position));

            final String text = m_DetailList.get(position);

            return v;
        }
    }
    //////////////   STATE TABLE VIEW ENDS HERE   ///////////////////////////////


    /////////////////////////////////////////////////////////////////////////
    //
    //  SAVE CSV BLOCK
    //
    /////////////////////////////////////////////////////////////////////////

    /**
     *
     * @brief To Save Sitting Data, Make CSV File
     * @details IF you Click the Button, Check the Button String. if the String is'Start Save' then this App Start saving Date and when you click the Button again, Make CSV File
     * @param
     * @return
     * @throws
     */

    public void onClick_Save(View v){
        switch (v.getId()) {
            case R.id.Save_Start:
                //  start save...
                if(m_isSavingOn==false){
                    //파일 저장 작업 시작
                    createCSVFile();

                    m_isSavingOn = true;
                    mSave_Start.setText(getResources().getString(R.string.ui_btn_save_stop));
                }
                //  end save...
                else{
                    m_isSavingOn = false;
                    //파일 저장 작업 종료
                    closeCSVFile();
                    mSave_Start.setText(getResources().getString(R.string.ui_btn_save_start));
                }
                break;
        }

    }


    /**
     *
     * @brief Save File - create, append and close(save)
     * @details When you Click 'Save Stop' Button, this Function count the now date and create CSV File. the File name is TODAY.csv
     * @param
     * @return
     * @throws
     */
    File m_fiCSV_Save;
    String m_sCSV_PathName;
    PrintWriter m_wrCSV_Writer;

    /**
     *
     * @brief Create CSV File
     * @details When you Click 'Save Stop' Button, this Function count the now date and create CSV File. the File name is TODAY.csv
     * @param
     * @return
     * @throws
     */
    private void createCSVFile() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formatDate = sdfNow.format(date);

        m_sCSV_PathName = Environment.getExternalStorageDirectory().getPath() + "/Download/" + formatDate + ".csv";
        Log.d("save", "m_sCSV_PathName: " + m_sCSV_PathName);

        try{
            m_fiCSV_Save = new File(m_sCSV_PathName);

            if(m_fiCSV_Save.exists() == false){
                m_fiCSV_Save.createNewFile();
            }

            m_wrCSV_Writer = new  PrintWriter(new FileWriter(m_fiCSV_Save,true));

            headingCSVFile(formatDate);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    private void headingCSVFile(String sFileTime){
        String sComma = ", ";
        String sCarriageReturn = "\r\n";

        m_wrCSV_Writer.print("TimeStamp" + sComma);

        int cell_index = 0;
        for (cell_index = 0; cell_index < PacketParser.def_CUSTOM_SENSOR_NUM; cell_index++) {
            String sText = "Cell:" + Integer.toString(cell_index + 1);
            m_wrCSV_Writer.print(sText + sComma);
        }
        m_wrCSV_Writer.print(sCarriageReturn);

    }

    private void appendCSVFile(String sBuf) {
        if( (m_isSavingOn == false) || (m_wrCSV_Writer == null) )
            return;

        m_wrCSV_Writer.print(sBuf);
    }

    private void closeCSVFile() {
        m_wrCSV_Writer.close();

        //  Display file path
        String msg = ("file path : " + m_sCSV_PathName);
        mTxtSavePath.setText(msg);
        Toast toast = Toast.makeText(getApplicationContext(), "Saved successfully", Toast.LENGTH_LONG);
        toast.show();
    }


    //  Save data
    private void UI_saveCSVProc(){
        if (m_isSavingOn == false){
            return;
        }

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyyMMdd_HHmmss");

        String formatDate = sdfNow.format(date);
        String sComma = ",";
        String sCarriageReturn = "\r\n";

        {
            appendCSVFile(formatDate + sComma);

            int nSensorValue = 0;
            int cell_index = 0;

            for (cell_index = 0; cell_index < PacketParser.def_CUSTOM_SENSOR_NUM; cell_index++) {
                nSensorValue = m_PacketParser.getCustomCellValue1(cell_index);
                String sPoint = Integer.toString(nSensorValue);

                appendCSVFile(sPoint + sComma);
            }

            //  line change
            appendCSVFile(sCarriageReturn);
        }
    }
    //////////////   SAVE CSV ENDS HERE   ///////////////////////////////


    /////////////////////////////////////////////////////////////////////////
    //
    //  REPLAY BLOCK
    //
    /////////////////////////////////////////////////////////////////////////
    public void onClick_Replay(View v) throws InterruptedException {
        switch (v.getId()) {
            case R.id.BTV_REPLAY_OPEN:
                //  open file
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

//                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/csv");
//                String[] mimeTypes = { "*/csv", "text/plain", "text/comma-separated-values" ,"application/pdf","image/*"};
//                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                startActivityForResult(intent, REQUEST_FILE_OPEN);
                break;

            case R.id.REPLAY_PLAY_PAUSE:
                //  toggle : pause to play
                if(m_isReplayOn==false){
//                        m_ReplayTimer.schedule(new com.marveldex.MdexDaq.MainActivity.TimerTask_Replay(), 0, TIMER_READ_INTERVAL);
                    m_ReplayTimerTask = new com.marveldex.MdexDaq.MainActivity.TimerTask_Replay();
                    m_ReplayTimer.schedule(m_ReplayTimerTask, 0, TIMER_READ_INTERVAL);

                    mbt_PlayPause.setText("PAUSE");
                    m_isReplayOn = true;
                }
                //  toggle : play to pause
                else{
                    mbt_PlayPause.setText("PLAY");
                    m_isReplayOn = false;
                }
                break;
        }
    }

    public void Replay_ForceCancel() {
        mbt_PlayPause.setText("PLAY");
        if(m_ReplayTimerTask == null)
            return;

//        m_ReplayTimerTask.cancel();
//        m_ReplayTimerTask = null;

        m_isReplayOn = false;
        m_Replay_PacketCount = 1;
    }

    private TimerTask m_ReplayTimerTask = null;
    private Timer m_ReplayTimer  = new Timer();
    private final int TIMER_READ_INTERVAL  = 100; // 1000 : 1 sec
    int m_Replay_ElapsedTime_Last= 0;
    int m_Replay_PacketCount = 1;
    int m_Replay_PacketNum = 0;

    List<String[]> m_Replay_RowsAll = null;

    //  Reference - Timer Pause) https://singo112ok.tistory.com/25
    private class TimerTask_Replay extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(m_BLE_State == MDEX_BLE_STATE_CONNECTED) {
                        mtv_PostureState.setText("리플레이 전에 BLE 연결을 끊어주세요. ");
                        return;
                    }
                    //  if pause, don't process more.
                    if(m_isReplayOn == false) {
                        return;
                    }

                    PacketParser.setDataFromCSV(m_Replay_RowsAll.get(m_Replay_PacketCount));
                    PacketParser.m_isHaveAllData = true;
                    onDataProc();
                    //  set progress bar
                    {
                        msb_Replay_RandomAccess.setProgress(m_Replay_PacketCount);
                    }

                    m_Replay_ElapsedTime_Last = m_Replay_PacketCount;
                    mtv_PostureState.setText(m_Replay_ElapsedTime_Last/10 + "." + m_Replay_ElapsedTime_Last%10 + " sec");

                    m_Replay_PacketCount++;
                    //  if end of packet. Kill timer
                    if(m_Replay_PacketCount == m_Replay_PacketNum) {
                        Replay_ForceCancel();
                        return;
                    }
                }
            });
        }
    }
    //////////////   REPLAY BLOCK ENDS HERE   ///////////////////////////////


}