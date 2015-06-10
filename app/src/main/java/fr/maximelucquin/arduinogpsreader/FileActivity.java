package fr.maximelucquin.arduinogpsreader;

import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import fr.maximelucquin.arduinogpsreader.entity.EntityList;
import fr.maximelucquin.arduinogpsreader.entity.Point;


public class FileActivity extends ActionBarActivity implements OnMapReadyCallback, AdapterView.OnItemClickListener, View.OnClickListener {

    private Toolbar toolbar;
    private SupportMapFragment map;
    private GoogleMap googleMap;
    private RelativeLayout listContainer;
    private ArrayAdapter<String> listAdapter;
    private ListView fileListview;
    private String[] theNamesOfFiles;
    private File filelist[];
    private FloatingActionButton fab;
    private Boolean hideMarker;

    private final double lat=48.29881172611295, lng=4.0776872634887695;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        toolbar = (Toolbar) findViewById(R.id.file_activity_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.title_activity_file);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        listContainer = (RelativeLayout) findViewById(R.id.list_container);
        fileListview = (ListView) findViewById(R.id.file_list);
        fab = (FloatingActionButton) findViewById(R.id.file_activity_fab);
        fab.setOnClickListener(this);

        hideMarker=false;

        map = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.file_place_map);
        map.getMapAsync(this);

        String path = Environment.getExternalStorageDirectory().toString()+"/Arduino_GPS_Reader";
        File f = new File(path);
        if(f!=null) {
            filelist = f.listFiles();
            if (filelist != null) {
                if (filelist.length > 0) {
                    theNamesOfFiles = new String[filelist.length];
                    for (int i = 0; i < theNamesOfFiles.length; i++) {
                        theNamesOfFiles[i] = filelist[i].getName();
                    }
                    listAdapter = new ArrayAdapter<String>(this, R.layout.listview_layout, theNamesOfFiles);
                    fileListview.setAdapter(listAdapter);
                    fileListview.setOnItemClickListener(this);

                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_list) {
            if(listContainer.getVisibility()== View.GONE){
                listContainer.setVisibility(View.VISIBLE);
            } else {
                listContainer.setVisibility(View.GONE);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap=googleMap;
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        //position de la carte sur le centre de Troyes
        CameraPosition cameraPosition = new CameraPosition.Builder().target(
                new LatLng(lat, lng)).zoom(8).build();
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        CameraUpdate zoom=CameraUpdateFactory.zoomTo(14);
        googleMap.animateCamera(zoom);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        listContainer.setVisibility(View.GONE);
        openFile(filelist[i]);

    }

    private void openFile(File file){
        FileInputStream fIn = null;
        try {
            fIn = new FileInputStream(file);
            try {

                BufferedReader reader = new BufferedReader(new InputStreamReader(fIn));
                String line;
                EntityList.pointList = new ArrayList<>();
                SimpleDateFormat fmt = new SimpleDateFormat("ddMMyyHHmmssSS");
                int i=1;
                while ((line = reader.readLine()) != null) {
                    String[] RowData = line.split(",");
                    Date date = new Date();
                    if(RowData.length==6) {
                        try {
                            date = fmt.parse(RowData[3]+RowData[4]);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        EntityList.pointList.add(new Point(i,//id
                                Double.valueOf(RowData[0]),//lat
                                Double.valueOf(RowData[1]),//lng
                                Double.valueOf(RowData[2]),//alt
                                date,//date
                                RowData[5].replace(";", "")));//note
                        i++;
                    }
                }
            }
            catch (IOException ex) {
                // handle exception
            }
            finally {
                try {
                    fIn.close();
                }
                catch (IOException e) {
                    // handle exception
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        addMarker();
    }

    private void addMarker(){
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        if(googleMap!=null){
            googleMap.clear();
            for(int i=0; i<EntityList.pointList.size();i++){
                if(hideMarker==true) {
                    String line = "Alt : " + Double.toString(EntityList.pointList.get(i).getAltitude()).substring(0, 3) + "\n" +
                            "Date : " + fmt.format(EntityList.pointList.get(i).getDate()) + "\n" +
                            "Note : " + EntityList.pointList.get(i).getNote();
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(EntityList.pointList.get(i).getLatitude(), EntityList.pointList.get(i).getLongitude()))
                            .title("ID : " + EntityList.pointList.get(i).getId())
                            .snippet(line)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                }
                if(i>0){
                    googleMap.addPolyline((new PolylineOptions())
                            .add(new LatLng(EntityList.pointList.get(i).getLatitude(), EntityList.pointList.get(i).getLongitude())
                                    , new LatLng(EntityList.pointList.get(i - 1).getLatitude(), EntityList.pointList.get(i - 1).getLongitude()))
                            .width(9)
                            .color(getResources().getColor(R.color.myAccentColor))
                            .visible(true));
                }
            }
        }
        CameraUpdate center=
                CameraUpdateFactory.newLatLng(new LatLng(EntityList.pointList.get(EntityList.pointList.size()-1).getLatitude(),
                        EntityList.pointList.get(EntityList.pointList.size()-1).getLongitude()));
        googleMap.animateCamera(center);
    }

    @Override
    public void onClick(View view) {
        if(view.equals(fab) && EntityList.pointList!=null) {
            if(EntityList.pointList.size()>0) {
                if (hideMarker == false) hideMarker = true;
                else hideMarker = false;
                addMarker();
            }
        }
    }
}
