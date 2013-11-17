package de.amr.plt.rcTestapp;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

public class MapActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}
	
	public void onStart() {
		super.onStart();
	}
	/**
	 * Changes the mode of the Position-Drawer.
	 * @param view
	 */
	public void onFollowVirtualPosClicked(View view) {
		View MapView = (MapView) findViewById(R.id.map);
		((de.amr.plt.rcTestapp.MapView) MapView).setVPointer(true); //TODO make it alternate
		
		EditText EditText_PosX = (EditText)findViewById(R.id.editText_PosX);
		EditText EditText_PosY = (EditText)findViewById(R.id.editText_PosY);
		
		//fetch coordinate data from EditText
		int vxpos = Integer.parseInt((EditText_PosX.getText()).toString()); //TODO get resources of vPOSx
		int vypos = Integer.parseInt((EditText_PosY.getText()).toString());
		
		//give the coordinate data to the MapView
		((de.amr.plt.rcTestapp.MapView) MapView).setVPosX(vxpos);
		((de.amr.plt.rcTestapp.MapView) MapView).setVPosY(vypos);
		
		//TODO TEST THIS FUNCTION
		Toast.makeText(this, "Following virtual position values", Toast.LENGTH_SHORT).show();
	}
	

}
