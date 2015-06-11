package fr.maximelucquin.arduinogpsreader;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends ActionBarActivity implements SerialInputOutputManager.Listener, OnMapReadyCallback, View.OnClickListener {

    private Toolbar toolbar;
    private TextView text1, text2, text3, text4, text5, text6, text7, text8;
    private FloatingActionButton fab;
    private RelativeLayout progressContainer;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private List<UsbSerialPort> portList;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private String latLngMsg;
    private double lat, lng;
    private SupportMapFragment map;
    private GoogleMap googleMap;
    private Marker marker;
    private Boolean fileMode;
    private FileOutputStream file;
    private PrintWriter fileWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.main_activity_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setLogo(R.mipmap.ic_launcher);
        text1 = (TextView) findViewById(R.id.text1);
        text2 = (TextView) findViewById(R.id.text2);
        text3 = (TextView) findViewById(R.id.text3);
        text4 = (TextView) findViewById(R.id.text4);
        text5 = (TextView) findViewById(R.id.text5);
        text6 = (TextView) findViewById(R.id.text6);
        text7 = (TextView) findViewById(R.id.text7);
        text8 = (TextView) findViewById(R.id.text8);
        progressContainer = (RelativeLayout) findViewById(R.id.progress_container);
        fab = (FloatingActionButton) findViewById(R.id.main_activity_fab);
        fab.setOnClickListener(this);

        lat=0;
        lng=0;
        latLngMsg="";
        driver=null;
        fileMode = false;
        file=null;

        map = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.current_place_map);
        map.getMapAsync(this);

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            text1.setText("USB not detected");
            return;
        } else {
            text1.setText("USB detected");
        }

        // Open a connection to the first available driver.
        driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            text2.setText("Permission Refused");
            return;
        } else {
            text2.setText("Permission Accepted");
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        detectUSBDevice();

    }

    private void detectUSBDevice() {
        if (driver == null) {
            return;
        } else{
            portList = driver.getPorts();
        }

        text3.setText("No error");
        try {
            portList.get(0).open(connection);
            portList.get(0).setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            System.out.println("Error setting up device: " + e.getMessage());
            text3.setText("Error opening device: " + e.getMessage());
            try {
                portList.get(0).close();
            } catch (IOException e2) {
                // Ignore.
            }

            return;
        }
        text4.setText("Serial device: " + portList.get(0).getClass().getSimpleName());
        startIoManager();
    }

    private void startIoManager() {
        if (portList.get(0) != null) {
            System.out.println("Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(portList.get(0), this);
            mExecutor.submit(mSerialIoManager);
        }
    }

    @Override
    public void onNewData(final byte[] data) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String rcvMsg = hexToString(HexDump.toHexString(data));
                latLngMsg=latLngMsg+rcvMsg;
                if(fileMode==false && rcvMsg.indexOf("#")!=-1){//debut mode enregistrement
                    System.out.println("-------------------Debut fichier--------------------");
                    fileMode=true;
                    progressContainer.setVisibility(View.VISIBLE);
                    String msg[] = latLngMsg.split("#");
                    latLngMsg=msg[1];
                } else if(fileMode==true && rcvMsg.indexOf("#")!=-1){//fin enregistrement
                    System.out.println("-------------------fin fichier--------------------");
                    String msg[] = latLngMsg.split("#");
                    latLngMsg=msg[0];
                    latLngMsg = latLngMsg.replaceAll("\r\n", "");
                    fileWriter.print(latLngMsg);
                    if(file!=null){
                        try {
                            fileWriter.flush();
                            fileWriter.close();
                            file.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    fileMode=false;
                    latLngMsg=msg[1];
                    progressContainer.setVisibility(View.GONE);
                } else if(rcvMsg.indexOf(";")!=-1){//message entier
                    if(fileMode==false && rcvMsg.indexOf("#")==-1) {//mode reception donnée instantané
                        String latLng[] = latLngMsg.split(",");
                        if(latLng.length==4) {
                            latLng[3] = latLng[1].replace(";", "");
                            text5.setText("Lat : " + latLng[0]);
                            text6.setText("Lon : " + latLng[1]);
                            text7.setText("Alt : " + latLng[2].substring(0, 3));
                            text8.setText("Vit : " + latLng[3].substring(0, 3));
                            lat = Double.valueOf(latLng[0]);
                            lng = Double.valueOf(latLng[1]);
                            System.out.println("lat " + lat);
                            System.out.println("lng " + lng);
                            setMarker();
                        }
                    } else if(fileMode==true && rcvMsg.indexOf("#")==-1){//en cours d'enregistrement
                        if(file!=null){
                            //line = latLngMsg.replace(System.getProperty("line.separator"), "");
                            latLngMsg = latLngMsg.replaceAll("\r\n", "");
                            fileWriter.print(latLngMsg);
                        } else {
                            file=createFile();
                            fileWriter = new PrintWriter(file);
                        }
                    }
                    latLngMsg = "";
                }
                System.out.println("-------------------NEXT--------------------");
            }
        });
    }

    @Override
    public void onRunError(Exception e) {
        System.out.println("Stop runner");
    }

    public static String hexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        char[] hexData = hex.toCharArray();
        for (int count = 0; count < hexData.length - 1; count += 2) {
            int firstDigit = Character.digit(hexData[count], 16);
            int lastDigit = Character.digit(hexData[count + 1], 16);
            int decimal = firstDigit * 16 + lastDigit;
            sb.append((char)decimal);
        }
        return sb.toString();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap=googleMap;
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {

            @Override
            public void onMyLocationChange(Location loc) {
                // TODO Auto-generated method stub

                CameraPosition cameraPosition = new CameraPosition.Builder().target(
                        new LatLng(loc.getLatitude(),
                                loc.getLongitude())).zoom(16).build();
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                googleMap.setOnMyLocationChangeListener(null);
            }
        });

    }

    private void setMarker(){
        if(googleMap!=null) {
            if(marker==null) {
                MarkerOptions markerOp = new MarkerOptions()
                        .position(new LatLng(lat, lng))
                        .title("Arduino Place")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                marker = googleMap.addMarker(markerOp);
            } else {
                marker.setPosition(new LatLng(lat, lng));

            }
        }
    }

    private FileOutputStream createFile(){
        try {
            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(root + "/Arduino_GPS_Reader");
            myDir.mkdirs();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            Date now = new Date();
            String fileName = "GPS_log_"+formatter.format(now) + ".csv";
            File myFile = new File(myDir, fileName);
            FileOutputStream file = new FileOutputStream(myFile);
            return file;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onClick(View view) {
        if(view.equals(fab)){
            Intent myIntent = new Intent(MainActivity.this, FileActivity.class);
            this.startActivity(myIntent);
        }
    }
}
