package fr.maximelucquin.arduinogpsreader;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends ActionBarActivity implements SerialInputOutputManager.Listener, OnMapReadyCallback {

    private Toolbar toolbar;
    private TextView text1, text2, text3, text4, text5, text6;
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


        lat=0;
        lng=0;
        latLngMsg="";
        driver=null;

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
                if(rcvMsg.indexOf(";")!=-1){
                    String latLng[] = latLngMsg.split(",");
                    latLng[1] = latLng[1].replace(";", "");
                    text5.setText(latLng[0]);
                    text6.setText(latLng[1]);
                    lat = Double.valueOf(latLng[0]);
                    lng = Double.valueOf(latLng[1]);
                    latLngMsg="";
                    System.out.println("lat "+lat);
                    System.out.println("lng "+lng);
                    setMarker();
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
}
