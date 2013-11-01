package de.amr.plt.rcTestapp;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.graphics.*;
import android.view.View;
import android.content.Context;
//TODO Import context!

public class MapActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		
		//Bitmap b = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
		//Canvas c = new Canvas(b);
		//c.drawText("Dies ist ein Canvas-Text", 0, 0, 0, 0, null);
		//Context mContext = getBaseContext();
		//View MapView = new MapView(mContext, null);
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

}
