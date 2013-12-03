package de.amr.plt.rcTestapp;

import de.amr.plt.rcParkingRobot.AndroidHmiPLT;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;
import android.app.ActionBar;

public class MapActivity extends Activity {
	
	AndroidHmiPLT hmiModule =null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		
		//hide the action bar, might cause problems with the API level
		ActionBar actionbar = getActionBar();
		actionbar.hide();
		
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
				//change Mode depending on the selected item
				switch(arg2) {
				case 0: 
					//hmiModule.setMode(parkingRobot.INxtHmi.Mode.SCOUT);
					Log.d("Spinner", "SCOUT selected");
					break;
				case 1: 
					//hmiModule.setMode(parkingRobot.INxtHmi.Mode.PARK_NOW);
					Log.d("Spinner", "PARK_NOW selected");
					break;
				case 2:
					//hmiModule.setMode(parkingRobot.INxtHmi.Mode.PARK_THIS);
					Log.d("Spinner", "PARK_THIS selected");
					break;
				case 3:
					//hmiModule.setMode(parkingRobot.INxtHmi.Mode.PAUSE);
					Log.d("Spinner", "PAUSE selected");
					break;
				case 4:
					//hmiModule.setMode(parkingRobot.INxtHmi.Mode.DISCONNECT);
					Log.d("Spinner", "DISCONNECT selected");
					break;
				default:
					Log.e("Spinner","Could not settle for any case in onItemSelected()");		
				}
				
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				Log.d("Spinner","No Item was selected.");	
			}
        });
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
		

}
