package de.amr.plt.rcTestapp;

import java.util.Timer;
import java.util.TimerTask;

import parkingRobot.INxtHmi.Mode;
import parkingRobot.hsamr1.GuidanceAT.CurrentStatus;
import de.amr.plt.rcParkingRobot.AndroidHmiPLT;
import de.amr.plt.rcTestapp.R;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnGenericMotionListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ToggleButton;
import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;

public class MapActivity extends Activity {
	
	
	//representing local Bluetooth adapter
	BluetoothAdapter mBtAdapter = null;
	//representing the bluetooth hardware device
	BluetoothDevice btDevice = null;
	//instance handels bluetooth communication to NXT
	AndroidHmiPLT hmiModule = null;	
	//request code 
	final int REQUEST_SETUP_BT_CONNECTION = 1;
	
	//create a listener for the MapView Motion Events
    static Handler mMotionHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		Log.d("Motion Handler","Message recieved"); //TODO insert stuff
    	}
    	
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		
		//STEP 1: INITIATE BLUETOOTH COMMUNICATION AND THE HMI-MODULE
		//----------------------------------------------------------------
		//get the BT-Adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();       
     
        //If the adapter is null, then Bluetooth is not supported
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        } 
           
        final Button connectButton = (Button) findViewById(R.id.buttonBluetoothConnect); //TODO change button
        //on click call the BluetoothActivity to choose a listed device
        connectButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v){
        		Intent serverIntent = new Intent(getApplicationContext(),BluetoothActivity.class);
				startActivityForResult(serverIntent, REQUEST_SETUP_BT_CONNECTION);
        	}
        });
		
		//STEP 2: BUILD THE UI-ELEMENTS
        //------------------------------------------------------------------
		MapView map= (MapView) findViewById(R.id.map);
		map.setOnGenericMotionListener(new OnGenericMotionListener() {
			public boolean onGenericMotion(View view, MotionEvent event) {
				Log.d("ClickCallback","X value:"+Float.toString(event.getX()));
				return true;
			}
		});
		
		//hide the action bar, might cause problems with the API level
		ActionBar actionbar = getActionBar();
		actionbar.hide();      
        
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	public void onDestroy(){
		super.onDestroy();
    	if(mBtAdapter != null){
    		//release resources  
    	mBtAdapter.cancelDiscovery();
    	}
	}
	
	/**
	 * handle pressing button with alert dialog if connected(non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		
	    if (hmiModule != null && hmiModule.connected) {
	    	//creating new AlertDialog
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to terminate the connection?")
			       .setCancelable(false)
			       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   //disconnect and return to initial screen
			        	   terminateBluetoothConnection();
			        	   restartActivity();
			           }
			       })
			       .setNegativeButton("No", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
	    }
	}
	
	/**
	 * instantiating AndroidHmiPlt object and display received data(non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		switch(resultCode){
		
		//user pressed back button on bluetooth activity, so return to initial screen 
		case Activity.RESULT_CANCELED:
			break;
		//user chose device
		case Activity.RESULT_OK:
			//connect to chosen NXT
			establishBluetoothConnection(data);							
			//display received data from NXT
			if(hmiModule.connected){			
				//After establishing the connection make sure the start mode of the NXT is set to PAUSE
//				
				hmiModule.setMode(Mode.PAUSE);
				
				//enable toggle button
				final ToggleButton toggleMode = (ToggleButton) findViewById(R.id.toggleMode);
				toggleMode.setEnabled(true);
				
				//disable connect button
				final Button connectButton = (Button) findViewById(R.id.buttonSetupBluetooth);
				connectButton.setEnabled(false);
				
				displayDataNXT();
				break;
			} else{
				Toast.makeText(this, "Bluetooth connection failed!", Toast.LENGTH_SHORT).show();
				Toast.makeText(this, "Is the selected NXT really present & switched on?", Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
	
	/**
	 * Connect to the chosen device 
	 * @param data
	 */
	private void establishBluetoothConnection(Intent data){
		//get instance of the chosen bluetooth device
		String address = data.getExtras().getString(BluetoothActivity.EXTRA_DEVICE_ADDRESS);	
		btDevice = mBtAdapter.getRemoteDevice(address);		
		
		//get name and address of the device
		String btDeviceAddress = btDevice.getAddress();		
		String btDeviceName = btDevice.getName();
		
		//instantiate client module
		hmiModule = new AndroidHmiPLT(btDeviceName, btDeviceAddress);
		
		//connect to the specified device
		hmiModule.connect();
		
		//wait till connection really is established and 
		int i = 0;
		while (!hmiModule.isConnected()&& i<100000000/2) {
			i++;
		}
		
		//now prepare the Spinner which links the Module with the user
		createModeSpinner();
	}
	
	/**
     * Display the current data of NXT
     */
	private void displayDataNXT(){
		
		new Timer().schedule(new TimerTask() {
			
			@Override
            public void run() {
				
                runOnUiThread(new Runnable() {
                    public void run() {
                    	if(hmiModule != null){
                    		//display x value
                        	final TextView fld_xPos = (TextView) findViewById(R.id.textViewValueX);
                    		fld_xPos.setText(String.valueOf(hmiModule.getPosition().getX()+" cm"));
                    		//display y value
                    		final TextView fld_yPos = (TextView) findViewById(R.id.textViewValueY);
                    		fld_yPos.setText(String.valueOf(hmiModule.getPosition().getY()+" cm"));
                    		//display angle value
                    		final TextView fld_angle = (TextView) findViewById(R.id.TextViewValueAngle); 
                    		fld_angle.setText(String.valueOf(hmiModule.getPosition().getAngle()+"°"));
                    		//display status of NXT
                    		final TextView fld_status = (TextView) findViewById(R.id.textViewValueStatus);
                    		fld_status.setText(String.valueOf(hmiModule.getCurrentStatus()));
                    		//display distance front
                    		final TextView fld_distance_front = (TextView) findViewById(R.id.textViewValueDistanceFront);
                    		fld_distance_front.setText(String.valueOf(hmiModule.getPosition().getDistanceFront())+" mm");
                    		//display distance back
                    		final TextView fld_distance_back = (TextView) findViewById(R.id.textViewValueDistanceBack);
                    		fld_distance_back.setText(String.valueOf(hmiModule.getPosition().getDistanceBack())+" mm");
                    		//display distance right	
                    		final TextView fld_distance_front_side = (TextView) findViewById(R.id.textViewValueDistanceFrontSide);
                    		fld_distance_front_side.setText(String.valueOf(hmiModule.getPosition().getDistanceFrontSide())+" mm");
                    		//display distance left
                    		final TextView fld_distance_back_side = (TextView) findViewById(R.id.textViewValueDistanceBackSide);
                    		fld_distance_back_side.setText(String.valueOf(hmiModule.getPosition().getDistanceBackSide())+" mm");
                    		//display bluetooth connection status
                    		final TextView fld_bluetooth = (TextView) findViewById(R.id.textViewValueBluetooth);
                    		//display connection status
                    		if(hmiModule.isConnected()){
                    			fld_bluetooth.setText("connected");
                    		} else {
                    			fld_bluetooth.setText("not connected");
                    		}
                    		//restart activity when disconnecting
                    		if(hmiModule.getCurrentStatus()==CurrentStatus.EXIT){
                    			terminateBluetoothConnection();
                    			restartActivity();
                    		}
                    	}
                    }
                });
            }
        }, 200, 100);
				
	}
	
	/**
	 * Terminate the bluetooth connection to NXT
	 */
	private void terminateBluetoothConnection(){
		Toast.makeText(this, "Bluetooth connection was terminated!", Toast.LENGTH_LONG).show();
		hmiModule.setMode(Mode.DISCONNECT);
		hmiModule.disconnect();
		
		while(hmiModule.isConnected()){
			//wait until disconnected
		}
		hmiModule = null;
	}
	
	/**
	 * restart the activity
	 */
	private void restartActivity(){
		Intent restartIntent = new Intent(getApplicationContext(),MainActivity.class);                    			
		startActivity(restartIntent);
		finish();
	}
    	
	/**
	 * Changes the mode of the Position-Drawer.
	 * @param view
	 */
	public void onFollowVirtualPosClicked(View view) {
		View MapView = findViewById(R.id.map);
		Button Button=(Button)(findViewById(R.id.Button_followVirtualPos));
		
		if (((de.amr.plt.rcTestapp.MapView) MapView).vPointerIsActive() == false ) {
			((de.amr.plt.rcTestapp.MapView) MapView).setVPointer(true);  //activate Pointer if previously deactivated
			Toast.makeText(this, "Following virtual position values", Toast.LENGTH_SHORT).show(); //show Info
			findViewById(R.id.Button_updateVirtualPos).setVisibility(View.VISIBLE); //make Update-Button visible
			
			Button.setText("Return to real values");
			
		}
		else {
			((de.amr.plt.rcTestapp.MapView) MapView).setVPointer(false); //deactivate Pointer if previously deactivated
			Toast.makeText(this, "Following real position values", Toast.LENGTH_SHORT).show(); //show Info
			findViewById(R.id.Button_updateVirtualPos).setVisibility(View.INVISIBLE); //make Update-Button invisible
			Button.setText("Activate virtual values");
		}
		
		//-------------------------------------------->
		EditText EditText_PosX = (EditText)findViewById(R.id.editText_PosX);
		EditText EditText_PosY = (EditText)findViewById(R.id.editText_PosY);
		
		//fetch coordinate data from EditText
		int vxpos = Integer.parseInt((EditText_PosX.getText()).toString());
		int vypos = Integer.parseInt((EditText_PosY.getText()).toString());
		
		//give the coordinate data to the MapView
		((de.amr.plt.rcTestapp.MapView) MapView).setVPos(vxpos, vypos);
		//----------------------------------------------<   TODO replace with updateVPos()
	}
	
	public void onUpdateVirtualPosClicked(View View) {
		View Button = findViewById(R.id.Button_updateVirtualPos);
		if (Button.isShown()) {
			updateVPos();
		}
	}
	
	private void updateVPos() {
		View MapView = findViewById(R.id.map);
		
		EditText EditText_PosX = (EditText)findViewById(R.id.editText_PosX);
		EditText EditText_PosY = (EditText)findViewById(R.id.editText_PosY);
		
		//fetch coordinate data from EditText
		try {
			int vxpos = Integer.parseInt((EditText_PosX.getText()).toString());
			int vypos = Integer.parseInt((EditText_PosY.getText()).toString());
			
			//give the coordinate data to the MapView
			((de.amr.plt.rcTestapp.MapView) MapView).setVPos(vxpos, vypos);
		}
		catch (NumberFormatException e){  //catch Problems that arise, when nothing has been entered
			Toast.makeText(this, "Incorrect values", Toast.LENGTH_LONG).show();
		}	
		catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		//FIXME Get this to work.
	}
	
	/**
	 * Creates a Spinner for the Mode-Selection,
	 * if an hmiModule has been established.
	 * @return success
	 */
	private boolean createModeSpinner() {
		if (hmiModule != null) {
			//prepare Spinner
	        //Source: http://developer.android.com/guide/topics/ui/controls/spinner.html
	        Spinner Spinner = (Spinner) findViewById(R.id.modeSpinner);
	        ArrayAdapter<CharSequence> Adapter = ArrayAdapter.createFromResource(this,
	        R.array.controlmodes, android.R.layout.simple_spinner_item);
	        Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        Spinner.setAdapter(Adapter);
			
	        //create a listener for the Spinner
	        Spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					//TextView which shows the target state
					TextView textview = (TextView)findViewById(R.id.textView_State);
					//change Mode depending on the selected item
					switch(arg2) {
					case 0: 
						hmiModule.setMode(parkingRobot.INxtHmi.Mode.SCOUT); //TODO outsource setText to actual State?
						textview.setText("SCOUT");
						Log.d("Spinner", "SCOUT selected");
						break;
					case 1: 
						hmiModule.setMode(parkingRobot.INxtHmi.Mode.PARK_NOW);
						textview.setText("PARK NOW");
						Log.d("Spinner", "PARK_NOW selected");
						break;
					case 2:
						hmiModule.setMode(parkingRobot.INxtHmi.Mode.PARK_THIS);
						textview.setText("PARK THIS");
						Log.d("Spinner", "PARK_THIS selected");
						break;
					case 3:
						hmiModule.setMode(parkingRobot.INxtHmi.Mode.PAUSE);
						textview.setText("PAUSE");
						Log.d("Spinner", "PAUSE selected");
						break;
					case 4:
						hmiModule.setMode(parkingRobot.INxtHmi.Mode.DISCONNECT);
						textview.setText("DISCONNECTED");
						Log.d("Spinner", "DISCONNECT selected");
						break;
					default:
						Log.e("Spinner","Could not settle for any case in onItemSelected(...)");		
					}
					
				}

				public void onNothingSelected(AdapterView<?> arg0) {
					Log.d("Spinner","No Item was selected.");	
				}
	        });
	        return true;
		}
		return false;
	}
		

}
