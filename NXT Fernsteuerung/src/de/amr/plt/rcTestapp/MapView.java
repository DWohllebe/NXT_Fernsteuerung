package de.amr.plt.rcTestapp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

/**
 * This class offers functionality as a Surface View. It can therefore
 * be used to implement a dedicated drawing surface within the View
 * hierarchy.
 * 
 * @author Daniel
 *
 */
public class MapView extends SurfaceView implements SurfaceHolder.Callback {
	
	//------------------------------------------
	// DECLARATION OF THREAD
	/**
	 * Thread of class MapView. Handles all drawing functionality.
	 * @author Daniel Wohllebe
	 * @version 0.1
	 */
	class MapThread extends Thread {
		//variables
		private SurfaceHolder mSurfaceHolder;
		private Context mContext;
		private Handler mHandler;
		
		//Constructor
		public MapThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
			mSurfaceHolder=surfaceHolder;
			mContext=context;
			mHandler=handler;		
		}
		
		public void drawRect() {
			Canvas c = null;
			Paint mPaint = new Paint();
			mPaint.setARGB(255, 0, 255, 0);
			try {
				c = mSurfaceHolder.lockCanvas(null);
							
				c.drawText("Hi, I am text.",  0, 0, mPaint);
			}
			finally {
				if (c != null)
					mSurfaceHolder.unlockCanvasAndPost(c);
			}
		}
		
	}
	
	//------------------------------------
	// DECLARATION OF CLASS
	
	private MapThread thread;
	
	public MapView(Context context, AttributeSet attrs) {
		//initialize with given context
		super(context, attrs);
		
		//register interest to hear about surface changes
		SurfaceHolder mapholder=getHolder();
		mapholder.addCallback(this);
		
		//create a thread, this will be started on surface creation
		thread = new MapThread(mapholder, context, new Handler());
		
	}
	
	
	/**
	 * Callback invoked when the surface dimensions change.
	 */
	public void surfaceChanged(SurfaceHolder holder, int format,int width0,int height) {
		
	}
	
	/**
	 * Callback invoked when the Surface has been created and is ready to
	 * be used.
	 */
	public void surfaceCreated(SurfaceHolder holder){
		//start thread
		thread.start();
	}
	
	/**
	 * Callback invoked when the Surface has been destroyed and
	 * must no longer be touched.
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		//halt thread and wait for it to finish
		boolean retry = true;
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
				
			  } 					
		}
	}
}
