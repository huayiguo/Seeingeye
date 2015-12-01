package com.example.huayiguo.seeingeye;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MapsActivity extends FragmentActivity {

    private static final int REQUEST_ENABLE_BT = 2;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private BluetoothAdapter bluetooth = null;
    private String deviceaddress =null;
    private ArrayAdapter<String> mArrayAdapter;

    private com.google.android.gms.maps.MapFragment mapFragment;
    private GoogleMap googleMap;
    private static UUID MY_UUID =UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice device=null;

    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private final int MESSAGE_WRITE = 0;
    private final int MESSAGE_READ = 2;

    private  ListView l;

    private List<LatLng> latLngs = new ArrayList<LatLng>();

    private boolean dataready = false;
    //https://developers.google.com/maps/documentation/directions/intro#Waypoints

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
       // setUpMapIfNeeded();


        mapFragment =(com.google.android.gms.maps.MapFragment) getFragmentManager().findFragmentById(R.id.map);
        if(mapFragment!=null){
            googleMap = mapFragment.getMap();
            googleMap.setMyLocationEnabled(true);
            System.out.println("googlemap not null");
        }

        /*Bluetooth part*/
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        if(bluetooth==null){
            System.out.println("the device does not support bluetooth");
        }
        //pop up dialog to user to enable the bluetooth if it's currently not

        if (!bluetooth.isEnabled())
        {
            System.out.println("the device is not enabling the bluetooth");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }else{
            System.out.println("the device is enabled with bluetooth");
        }

        l =(ListView)findViewById(R.id.Plusdevice);
        mArrayAdapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);


        Set<BluetoothDevice> pairedDevices=bluetooth.getBondedDevices();
        mArrayAdapter.add("Device found:");

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        bluetooth.startDiscovery();

        l.setAdapter(mArrayAdapter);
        l.setTextFilterEnabled(true);
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position,
                                    long id) {
                String devicename = parent.getItemAtPosition(position).toString();
                TextView tx =(TextView)findViewById(R.id.device_select);
                String[] stringarray = devicename.split("\n");
                deviceaddress = stringarray[1];
                tx.setText(devicename);
            }
        });

        Button confirm = (Button)findViewById(R.id.confirm);
        confirm.setOnClickListener(new confirmlistener());

        //Button confirm = (Button)findViewById(R.id.confirm);
        //confirm.setOnClickListener(new confirmlistener());
        System.out.println("Waitting ---------->reach the end");

        new AsyncCaller().execute();
    }

    private class confirmlistener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            //this listener will invoke the bluetooth connection method
           // String acquiring = "Acquiring Data";
           // TextView tv = (TextView)findViewById(R.id.status);
           // tv.setText(acquiring);
            device = bluetooth.getRemoteDevice(deviceaddress);

            ConnectThread connect = new ConnectThread(device);
            if(bluetooth.isDiscovering()){
                bluetooth.cancelDiscovery();
            }
            connect.start();
            System.out.println("connectthread established!"+deviceaddress);
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
              //  l.setAdapter(mArrayAdapter);
               // System.out.println("almost found one");
                System.out.println(device.getName() + "\n" + device.getAddress());
              //  System.out.println(device.toString());
               // device.
               // mArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
       // setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(39.9518271,-75.190501)).title("Upenn"));
    }

    public static class DirectionsResult {
        @Key("routes")
        public List<Route> routes;
    }

    public static class Route {
        @Key("overview_polyline")
        public OverviewPolyLine overviewPolyLine;
    }

    public static class OverviewPolyLine {
        @Key("points")
        public String points;
    }



    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        //############################  secure connection (with PC)
/*
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                System.out.println("Trying to connect");
            } catch (IOException e) { }
            mmSocket = tmp;

        }*/

        //##############################  Insecure connection

	 public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
            mmDevice = device;
	        // MY_UUID is the app's UUID string, also used by the server code
			//tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			Method m;
			try {
				m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
				tmp = (BluetoothSocket) m.invoke(device, 1);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
	        mmSocket = tmp;
	        if(tmp==null){
	        	System.out.println("tmp is null----------->");

	        }
	    }
        public void run() {
            // Cancel discovery because it will slow down the connection
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                System.out.println("connection eastiblisthing---------->");
                mmSocket.connect();
                System.out.println("connection eastiblisthed---------->");
                //Toast.makeText(getApplicationContext(), "connection eastiblisth", Toast.LENGTH_SHORT).show();
                ;	        } catch (IOException connectException) {
               // Log.e(deviceaddress, deviceaddress, connectException);
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
         //   mhandler.obtainMessage(1).sendToTarget();
            ConnectedThread manage = new ConnectedThread(mmSocket);
            manage.start();

        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void addMarkersToMap(List<LatLng> locations){
        int i = 0;
        float b = (float) 4.0;
        for(LatLng a : locations) {
            i++;
            googleMap.addMarker(new MarkerOptions().position(a));
            if(i==locations.size()/2){
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(a, b));
            }

        }
    }


    private class AsyncCaller extends AsyncTask<URL, Void, String>
    {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread

        }
        @Override
        protected String doInBackground(URL... urls) {

            //this method will be running on background thread so don't update UI frome here
            //do your long running http tasks here,you dont want to pass argument and u can access the parent class' variable url over here
            try {
                HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(JSON_FACTORY));
                    }
                });

                GenericUrl url = new GenericUrl("http://maps.googleapis.com/maps/api/directions/json");
                url.put("origin", "Chicago,IL");
                url.put("destination", "Los Angeles,CA");
                url.put("sensor",false);

                HttpRequest request = requestFactory.buildGetRequest(url);
                HttpResponse httpResponse = request.execute();
                DirectionsResult directionsResult = httpResponse.parseAs(DirectionsResult.class);
                String encodedPoints = directionsResult.routes.get(0).overviewPolyLine.points;
                latLngs = PolyUtil.decode(encodedPoints);
                dataready=true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

          //  return null;
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            addMarkersToMap(latLngs);
            //this method will be running on UI thread

        }

    }

    private final Handler mhandler= new Handler() {

        @SuppressLint("ShowToast")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();

                    break;
                case MESSAGE_WRITE:

                    String writMessage = "write";
                    Toast.makeText(getApplicationContext(),writMessage,Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(getApplicationContext(),"connected",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            System.out.println("trying to manage the connection and transfer data---------->");
            byte[] buffer = new byte[4000];  // buffer store for the stream
           // byte[] outbuffer1 = new byte[4];

            byte[] outbuffer4 = new byte[31];

            float [] array =  new float[9000];
            int bytes=0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                        if(dataready){
                            for(int i = 0 ; i<latLngs.size();i++){
                                buffer = (latLngs.get(i)).toString().getBytes();
                                mmOutStream.write(buffer);
                            }
                        }
                        //    System.out.println("start transferring data222222--->\n");
                        bytes = mmInStream.read(buffer);
                        System.out.println("bytes is --->"+bytes);
                        mhandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();


                } catch (IOException e) {
                    break;
                }
            }
        }
    }
}




