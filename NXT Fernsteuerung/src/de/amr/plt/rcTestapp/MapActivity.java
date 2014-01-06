package de.amr.plt.rcTestapp;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import parkingRobot.INxtHmi.Mode;
import de.amr.plt.rcParkingRobot.AndroidHmiPLT;
import de.amr.plt.rcParkingRobot.IAndroidHmi.ParkingSlot;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ToggleButton;
import android.widget.ImageButton;
import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import parkingRobot.hsamr1.GuidanceAT;
import parkingRobot.hsamr1.GuidanceAT.*;
import parkingRobot.INavigation;


public class MapActivity extends Activity {
	
	
	//representing local Bluetooth adapter
	BluetoothAdapter mBtAdapter = null;
	//representing the bluetooth hardware device
	BluetoothDevice btDevice = null;
	//instance handels bluetooth communication to NXT
	AndroidHmiPLT hmiModule = null;	
	//request code 
	final int REQUEST_SETUP_BT_CONNECTION = 1;
	
	boolean test= false;
	
	//determines, how incoming Pos-Data should be valued
	final float MEASUREMENT_SCALE= (1/100);
	
	//create a listener for the MapView Motion Events
    static Handler mMotionHandler = new Handler() {
    	@Override
		public void handleMessage(Message msg) {
    		Log.d("Motion Handler","Message recieved"); //TODO insert stuff
    	}
    	
    };
    
    //prepare picture resources
    Bitmap bScout; //TODO create routine that updates image buttons
    Bitmap bPark_this;
    Bitmap bPark_now;
    Bitmap bPause;
    Bitmap bDisconnect;
    Bitmap bSensorInfoToggle;
    Bitmap bBluetoothOn;
    Bitmap bBluetoothOff;

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
           
        final ImageButton connectButton = (ImageButton) findViewById(R.id.imageButton_BluetoothConnect); //TODO change button
        //on click call the BluetoothActivity to choose a listed device
        connectButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v){
        		Intent serverIntent = new Intent(getApplicationContext(),BluetoothActivity.class);
				startActivityForResult(serverIntent, REQUEST_SETUP_BT_CONNECTION);
        	}
        });
        
        final Button testButton = (Button) findViewById(R.id.buttonTest);
        testButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v){
        		test= true;
        		for (int i=0; i < 250000; i++) {
        			MapView map= (MapView)findViewById(R.id.map);
        			map.setPose((float)(i/10000), 0,(float) (i/1000));
        		}
        		test = false;
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
		
		//decode the Bitmap-Resources	
		Resources res = this.getBaseContext().getResources();
		bScout = BitmapFactory.decodeResource(res, R.drawable.bt_scout);
		bPark_this = BitmapFactory.decodeResource(res, R.drawable.bt_park_this);
		bPark_now = BitmapFactory.decodeResource(res, R.drawable.bt_park_now);
		bPause = BitmapFactory.decodeResource(res, R.drawable.bt_pause);
		bDisconnect = BitmapFactory.decodeResource(res, R.drawable.bt_disconnect);
		//bSensorInfoToggle = BitmapFactory.decodeResource(res, ...); //TODO draw something
		bBluetoothOn = BitmapFactory.decodeResource(res, R.drawable.bt_bluetooth_on);
		bBluetoothOff = BitmapFactory.decodeResource(res, R.drawable.bt_bluetooth_off);
        
		//instruct the user to connect to the parking robot
		TextView infotext = (TextView) findViewById(R.id.textView_Info);
		infotext.setText("Please connect your device via Bluetooth.");
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
	
	@Override
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
				//final ToggleButton toggleMode = (ToggleButton) findViewById(R.id.toggleMode);
				//toggleMode.setEnabled(true);
				
				//disable connect button
				final ImageButton connectButton = (ImageButton) findViewById(R.id.imageButton_BluetoothConnect);
				connectButton.setEnabled(false);
				
				//start listening for status updates
				startStatusListener();
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
                        	final TextView fld_xPos = (TextView) findViewById(R.id.textView_XValue);
                    		fld_xPos.setText(String.valueOf(hmiModule.getPosition().getX()+" cm"));
                    		//display y value
                    		final TextView fld_yPos = (TextView) findViewById(R.id.textView_YValue);
                    		fld_yPos.setText(String.valueOf(hmiModule.getPosition().getY()+" cm"));
                    		//display angle value
                    		final TextView fld_angle = (TextView) findViewById(R.id.textView_AngleValue); 
                    		fld_angle.setText(String.valueOf(hmiModule.getPosition().getAngle()+"Â°"));
                    		
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
		Intent restartIntent = new Intent(getApplicationContext(),MapActivity.class);                    			
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
	
	/**
	 * Manually updates the virtual Pointer. For debug purposes.
	 */
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
					//TextView textview = (TextView)findViewById(R.id.textView_State);
					final ImageButton btMode= (ImageButton) findViewById(R.id.imageButton_ChangeMode);
					final TextView infotext= (TextView) findViewById(R.id.textView_Info);
					
					//change Mode depending on the selected item
					try {
					switch(arg2) {
					case 0: 
						hmiModule.setMode(parkingRobot.INxtHmi.Mode.PAUSE);
						btMode.setImageBitmap(bPause);
						infotext.setText("PAUSE-mode selected!");
						Log.d("Spinner", "PAUSE selected");
						break;
					case 1:
						hmiModule.setMode(parkingRobot.INxtHmi.Mode.SCOUT); //TODO outsource setText to actual State?
						btMode.setImageBitmap(bScout);
						infotext.setText("SCOUT-mode selected!");	
						Log.d("Spinner", "SCOUT selected");
						break;
					case 2: 
						hmiModule.setMode(parkingRobot.INxtHmi.Mode.PARK_NOW);
						btMode.setImageBitmap(bPark_now);
						infotext.setText("PARK_NOW-mode selected!");
						Log.d("Spinner", "PARK_NOW selected");
						break;
					case 3:
						final MapView map =(MapView)findViewById(R.id.map);
						hmiModule.setMode(parkingRobot.INxtHmi.Mode.PARK_THIS);
						btMode.setImageBitmap(bPark_this);
						infotext.setText("PARK_THIS-mode selected! Parking in selected Slot " + map.getParkingSlotSelectionID() + ".");
						Log.d("Spinner", "PARK_THIS selected");			
						hmiModule.setSelectedParkingSlot( map.getParkingSlotSelectionID() ); //give selected ParkingSlot to the HMIModule
						break;					
					case 4:
						hmiModule.setMode(parkingRobot.INxtHmi.Mode.DISCONNECT);
						btMode.setImageBitmap(bDisconnect);
						infotext.setText("Attempting to DISCONNECT!");
						Log.d("Spinner", "DISCONNECT selected");
						break;
					default:
						Log.e("Spinner","Could not settle for any case in onItemSelected(...)");		
					}
					} catch (NullPointerException e) {
						Log.e("Spinner",e.getMessage() + "PointerException: Does an hmiModule exist? Ignoring...");
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
	
	/**
	 * Switches the visibility of the additional sensor information output.
	 * @param View
	 */
	public void onToggleSensorInfoClicked(View View) {	
		final TextView fld_xPos = (TextView) findViewById(R.id.textView_XValue);
		final TextView fld_xPosL = (TextView) findViewById(R.id.textView_XValue_Label);
		final TextView fld_yPos = (TextView) findViewById(R.id.textView_YValue);
		final TextView fld_yPosL = (TextView) findViewById(R.id.textView_YValue_Label);
		final TextView fld_angle = (TextView) findViewById(R.id.textView_AngleValue);
		final TextView fld_angleL = (TextView) findViewById(R.id.textView_AngleValue_Label);
		final ImageView infobox = (ImageView) findViewById(R.id.image_infobox);
		
		//check if TextViewLabels are visible, 
		//if yes, make them and their values invisible
		//and reverse
		
		if (fld_xPos.isShown()) {
			fld_xPos.setVisibility(android.view.View.INVISIBLE);
			fld_xPosL.setVisibility(android.view.View.INVISIBLE);
		}
		else {
			fld_xPos.setVisibility(android.view.View.VISIBLE);
			fld_xPosL.setVisibility(android.view.View.VISIBLE);
		}
		
		if (fld_yPos.isShown()) {
			fld_yPos.setVisibility(android.view.View.INVISIBLE);
			fld_yPosL.setVisibility(android.view.View.INVISIBLE);
		}
		else {
			fld_yPos.setVisibility(android.view.View.VISIBLE);
			fld_yPosL.setVisibility(android.view.View.VISIBLE);
		}
		
		if (fld_angle.isShown()) {
			fld_angle.setVisibility(android.view.View.INVISIBLE);
			fld_angleL.setVisibility(android.view.View.INVISIBLE);
		}
		else {
			fld_angle.setVisibility(android.view.View.VISIBLE);
			fld_angleL.setVisibility(android.view.View.VISIBLE);
		}
		
		if (infobox.isShown()) {
			infobox.setVisibility(android.view.View.INVISIBLE);
		}
		else {
			infobox.setVisibility(android.view.View.VISIBLE);
		}
	}
	
	public void onChangeModeClicked(View View) {
		
		//tell the user what to do
		final TextView infotext = (TextView) findViewById(R.id.textView_Info);
		if (hmiModule != null)
			infotext.setText("Please select a mode");
		else
			infotext.setText("No Connection-Interface known. Try connecting!");
		
		//activate the Spinner
		final Spinner spinner = (Spinner) findViewById(R.id.modeSpinner);
		spinner.performClick();
	}
	
	private void createTestModeSpinner() {
		//TODO DELETE THIS LATER
		//prepare Spinner
        //Source: http://developer.android.com/guide/topics/ui/controls/spinner.html
        final Spinner Spinner = (Spinner) findViewById(R.id.modeSpinner);
        ArrayAdapter<CharSequence> Adapter = ArrayAdapter.createFromResource(this,
        R.array.controlmodes, android.R.layout.simple_spinner_item);
        Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner.setAdapter(Adapter);
		
        //create a listener for the Spinner
        Spinner.post(new Runnable() {
        	public void run() {
        		Spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

        			public void onItemSelected(AdapterView<?> arg0, View arg1,
        					int arg2, long arg3) {
        				//TextView which shows the target state
        				TextView textview = (TextView)findViewById(R.id.textView_State);
        				//change Mode depending on the selected item
        				switch(arg2) {
        				case 0: 
        					textview.setText("TEST!PAUSE");
        					Log.d("Spinner", "PAUSE selected");
        					break;
        				case 1: 
        					textview.setText("TEST!SCOUT");
        					Log.d("Spinner", "SCOUT selected");
        					break;
        				case 2:
        					textview.setText("TEST!PARK THIS");
        					Log.d("Spinner", "PARK_THIS selected");
        					break;
        				case 3:
        					textview.setText("TEST!PARK NOW");
        					Log.d("Spinner", "PARK_NOW selected");
        					break;
        				case 4:
        					textview.setText("TEST!DISCONNECTED");
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
        	}
        	
        });
        
		
	}
	
	private void startStatusListener() {
new Timer().schedule(new TimerTask() {
			
			@Override
            public void run() {
				
                runOnUiThread(new Runnable() {
                    public void run() {                  	
                    	final ImageButton btBluetooth = (ImageButton) findViewById(R.id.imageButton_BluetoothConnect);
                    	final TextView tvState = (TextView) findViewById(R.id.textView_State);
                    	
                    	//propagate changes to Bluetooth-button
                    	if (hmiModule!= null) {
                    		if (hmiModule.isConnected()) {
                    			btBluetooth.setImageBitmap(bBluetoothOn);
                    		}
                    		else {
                    			btBluetooth.setImageBitmap(bBluetoothOff);
                    		}
                    		
                    		
                    	//propagate changes to Status-Text
                    		try {
                    			switch (hmiModule.getCurrentStatus()) {
                    				case DRIVING:
                    					tvState.setText("DRIVING"); break;
                    				//case PARKING:  TODO add later when declared
                    					//	tvState.setText("PARKING"); break;
                    				case INACTIVE:
                    					tvState.setText("INACTIVE"); break;
                    				case EXIT:
                    					tvState.setText("ABORTING"); break;
                    			}
                    		} catch (NullPointerException e) {
                    			Log.e("hmiModule", e.getMessage()+"! Cause:"+e.getCause()+". Does a state exist?");
                    			tvState.setText("UNDEFINED");
                    		}
                    		
                    	//give pose information to MapView
                    	float cXPOS = hmiModule.getPosition().getX()*MEASUREMENT_SCALE;
                    	float cYPOS = hmiModule.getPosition().getY()*MEASUREMENT_SCALE;
                    	float cANGLE = hmiModule.getPosition().getAngle();
                    	
                    	final MapView map = (MapView) findViewById(R.id.map);
                    	if (test==false)
                    	map.setPose(cXPOS, cYPOS, cANGLE);
                    	
                    	//get all current parking slots and pass them directly to MapView
                    	try {
                    		//populate the list of ParkingSlots
                    		for (int i=0; i < hmiModule.getNoOfParkingSlots(); i++) {
                    		map.addParkingSlot(hmiModule.getParkingSlot(i));
                    		}
                    		
                    		//send them to the Draw-Thread
                    		map.propagateParkingSlots();
                    	}
                    	catch (NullPointerException e) {
                    		Log.e("StatusListener", e.getMessage() + " Continuing without propagating ParkingSlots!");
                    	}
                    	
                    	//propagate changes to additional sensor info text
                    	//display x value
                    	final TextView fld_xPos = (TextView) findViewById(R.id.textView_XValue);
                		fld_xPos.setText(String.valueOf(cXPOS+" cm"));
                		//display y value
                		final TextView fld_yPos = (TextView) findViewById(R.id.textView_YValue);
                		fld_yPos.setText(String.valueOf(cYPOS+" cm"));
                		//display angle value
                		final TextView fld_angle = (TextView) findViewById(R.id.textView_AngleValue); 
                		fld_angle.setText(String.valueOf(cANGLE+"°"));
                		
                		//restart activity when disconnecting
                		if(hmiModule.getCurrentStatus()==CurrentStatus.EXIT){
                			terminateBluetoothConnection();
                			restartActivity();
                    	
                    	}
                    }
                    	
                 }                
                });
			};
	}, 200, 100);

	}
	


}

