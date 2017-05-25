package com.Brew;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import java.util.Timer;
import java.util.TimerTask;


import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.app.AlertDialog;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;





public class Control extends ActionBarActivity {

    Button btnOn,btnlink,start;
    TextView temp;
    TextView Og ,Fg, ABV;
    String tempr;
    Float tem;

    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //Context cts;
    //SPP UUID. Look for it
      // buffer store for the stream

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    InputStream tmpIn;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_control);

        //call the widgtes
        btnOn = (Button)findViewById(R.id.button2);
        btnlink = (Button)findViewById(R.id.button3);
        start = (Button)findViewById(R.id.button4);

        temp = (TextView) findViewById(R.id.textView7);
        Og = (TextView) findViewById(R.id.textView3);
        Fg = (TextView) findViewById(R.id.textView2);
        ABV = (TextView) findViewById(R.id.textView4);


        new ConnectBT().execute(); //Call the class to connect

        //msg("here");

        //commands to be sent to bluetooth


        start.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(Control.this);
                builder.setTitle(R.string.app_name);
                builder.setMessage("Do you want to start a new brew?");
                builder.setIcon(R.drawable.ic_launcher);
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        //delete();

                        String[] s = read("O");
                        Float value = Float.valueOf(s[1]);
                        tem = value;

                        //String t = read("T");
                        temp.setText(s[0] + "℉");
                        double x = value;

                        double n = correlation(x);
                        Og.setText(""+n);
                        Fg.setText(""+n);
                        double o = correction(n , Float.valueOf(s[0]));
                        double p = (o - 0)*131.25;

                        saveOG("OG",n);
                        ABV.setText(""+p);

                        tempr = s[0];

                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();

            }
        });

        btnOn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                Float value;
                String c;
                String[] s = read("O");
                if(s[1] != null) {
                    value = Float.valueOf(s[1]);
                    tem = value;
                }
                else {
                    value = tem;
                }

                double xx = (double) getOG("FG");
                //String t = read("T");
                temp.setText(s[0] + "℉");
                double x = value;

                double n =  correlation(x);
                Fg.setText(""+n);

                saveOG("FG",n);

                //Float value = Float.valueOf(n);


                double Og = (double) getOG("OG");
                msg(""+Og);

                if(s[1] != null){
                    c = s[1];
                }
                else{
                    c = tempr;
                }
                double o = correction(n, Float.valueOf(c));
                double xg = correction(Og , Float.valueOf(c));
                double p = (xg - o)*131.25;

                ABV.setText(""+p);


                btnOn.setEnabled(false);
                Timer buttonTimer = new Timer();
                buttonTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                btnOn.setEnabled(true);
                            }
                        });
                    }
                }, 2000);


            }
        });

        //click to web link
        btnlink.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //msg("pressed");
                Intent i = new Intent(Control.this, webView.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

    }

    public double correlation(double x){
       double n = 0.0;

        if(x > 4){
            n = 1.68;
        }
        if(x > 8){
            n = 1.58;
        }
        if(x>12){
            n = 1.48;
        }
        if(x>16){
            n = 1.38;
        }
        if(x>21){
            n = 1.28;
        }
        if(x>25){
            n = 1.18;
        }
        if(x>29){
            n = 1.08;
        }

        return n;

    }

    //correcting the specific gravity reading
    private double correction(double SG , double F){

        double correction = 1.313454 - (0.132674*F) + (0.002057793*(F*F)) - (0.000002627634*(F*F*F));
        double SG_corrected = SG + (correction * 0.001);

        return SG_corrected;
    }

    /*
     * Returns an array of temprature and sensor measurements by seprating data based of letter flags.
     *
     */
    private String[] read(String x)
    {
        String[] values = new String[2];
        String readMessage;
        String read = "";
        String r = "";

        int i = 1;
        if (btSocket!=null)
        {
            try
            {
                //msg("pressed");
                if (tmpIn.available() > 0) {
                    byte[] buffer = new byte[1024];
                    DataInputStream mmInStream = new DataInputStream(tmpIn);
                    int bytes = mmInStream.read(buffer);
                    readMessage = new String(buffer,0,bytes);

                    if(x == "O"){
                        double old = (double) getOG("FG");

                        //saveOG("oldfg",old);
                        Set<String> Gravity = get("FGs");
                        Gravity.add(""+old);
                        save("FGs",Gravity);

                        while (readMessage.charAt(i) != 'b') { // if it ends with b it a temp
                            read += readMessage.charAt(i - 1);
                            i = i + 1;

                        }

                        while(readMessage.charAt(i) != 'o'){ //ends with o its a sensor measurement
                            r += readMessage.charAt(i);
                            i = i + 1;

                        }




                    }


                }
                //if nothing available use the previous temp and sensor reading to no change
                else{
                   read = tempr;
                   r = "b0.00";

                }



            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
        else{
            read = "not set";
        }

        values[0] = read;
        values[1] = r.substring(1,r.length()); // gotta use the sensor measuments substrings to
        // not include b at beginning or o at end


        //read = r.substring(1,r.length());
        return values;
    }

    //Round the value for display
    public static double round1(double value, int scale) {
        return Math.round(value * Math.pow(10, scale)) / Math.pow(10, scale);
    }

    //to save Original gravity in shared prefrences.
    private void saveOG(String key, double value) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPref",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, (int) value);
        editor.commit();
    }

    //clear shared prefrences.
    private void delete(){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPref",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    //get original gravity at shared prefrences
    private int getOG(String key){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPref",MODE_PRIVATE);
        int savedPref = sharedPreferences.getInt(key, 0);
        return savedPref;
    }

    //save a string set of final gravity to do a change in gravity graph
    private void save(String key, Set value) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPref",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(key, value);
        editor.commit();
    }

    //get the string set of final gravity
    private Set get(String key){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPref",MODE_PRIVATE);
        Set<String> f  = new HashSet<String>(Arrays.asList(" "));;
        Set savedPref = sharedPreferences.getStringSet(key,f );
        return savedPref;
    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //connection from phone to the bluetooth chip on the board.
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(Control.this, "Connecting...", "Please wait");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                 myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                 BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                 btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                 myBluetooth.cancelDiscovery();
                 btSocket.connect();//start connection
                 tmpIn = btSocket.getInputStream();
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;

            }
            progress.dismiss();



        }
    }
}
