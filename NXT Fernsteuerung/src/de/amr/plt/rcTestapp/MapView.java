package de.amr.plt.rcTestapp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.NinePatch;
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
 * hierarchy, to display related map information about the NXT robot.
 * 
 * @author Daniel Wohllebe
 *
 */
public class MapView extends SurfaceView implements SurfaceHolder.Callback {
	
	//------------------------------------------
	// DECLARATION OF THREAD
	/**
	 * Thread of class MapView. Handles all drawing functionality.
	 * @author Daniel Wohllebe
	 * @version 0.1.1
	 */
	static class MapThread extends Thread { //TODO extend with AsyncTask!
		//finals
		//Variable for default background color
		final private int CLEAR=0xff000000; 
		
		//variables
		private SurfaceHolder mSurfaceHolder;
		private Context mContext;
		private Handler mHandler;
		private boolean mRun = false;
		private Resources res;
		private boolean vPosActive;
		private float vPOSX = 0;
		private float vPOSY = 0;
		Bitmap bBackground;
		Bitmap bRobot;
		Bitmap pointer;
		
		//Constructor
		public MapThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
			mSurfaceHolder=surfaceHolder;
			mContext=context;
			mHandler=handler;
			
			//fetch Resources
			res = mContext.getResources();
			
			//initialize all Bitmaps
			bBackground = BitmapFactory.decodeResource(res, R.drawable.bg_map);
			pointer = BitmapFactory.decodeResource(res, R.drawable.ic_launcher_nxt);
			
			
		}
		
		public void setRunning(boolean b) {
			mRun = b;
		}
		
		/**
		 * Method for testing various Draw-related functions.
		 */
		public void drawTestImage() {
			Canvas c= null;
			Paint mPaint = new Paint();
			mPaint.setARGB(255, 255, 255, 255);
			try {
				c = mSurfaceHolder.lockCanvas(null);
				Bitmap image = BitmapFactory.decodeResource(res, R.drawable.ic_launcher_nxt);
				Bitmap sprite = BitmapFactory.decodeResource(res, R.drawable.spr_self);
				int magn = 16;   //magnify
				sprite=Bitmap.createScaledBitmap(sprite, sprite.getWidth()*magn, sprite.getHeight()*magn, false);
				c.drawColor(0xff00ff00); //draw green background
				c.drawBitmap(bBackground, 0, 0, null);
				c.drawBitmap(image, 120, 120, null);
				c.drawText("Hi, I am text.",  0, 0, mPaint);
				c.drawBitmap(sprite, 240, 240, null);
				//NinePatch patch = new NinePatch(sprite, sprite.getNinePatchChunk(), null);
				//RectF pRect = new RectF(2,2,2,2);
				//patch.draw(c, pRect);
				
			} finally {
				if (c != null)
					mSurfaceHolder.unlockCanvasAndPost(c);
			}			
		}
		
		/**
		 * Draws the Map Background on the Canvas.
		 */
		public void drawMap() {
			Canvas c= null;
			try {
				c = mSurfaceHolder.lockCanvas(null);
				Paint mPaint = new Paint();
				mPaint.setColor(0xff000000);
				
				//clear screen
				c.drawColor(CLEAR);
				
				//draw map from resource
				Bitmap mBackground = null;
				mBackground=Bitmap.createScaledBitmap(bBackground, c.getWidth(), c.getHeight(), false);
				c.drawBitmap(mBackground, 0, 0, null);
				
				RectF rect2 = new RectF(0, 0, c.getWidth()*11/15, c.getHeight()*(float)(4.1/9));
				rect2.offset(c.getWidth()*(float)((1.2)/15), c.getHeight()*(float)(1.1/7));
				mPaint.setAntiAlias(true);
				mPaint.setColor(Color.CYAN);
				mPaint.setStyle(Paint.Style.STROKE); 
				mPaint.setStrokeWidth(4.5f);
				//rect.offset(5, 5);
				c.drawRect(rect2, mPaint); //TODO Cleanup
				
			} finally {
				if (c != null)
					mSurfaceHolder.unlockCanvasAndPost(c);
			}			
		}
		
		/**
		 * Resets the screen.
		 */
		public void clearScreen() {
			Canvas c= null;
			try {	
				c = mSurfaceHolder.lockCanvas(null);
				c.drawColor(CLEAR);
				
			} finally {
				if (c != null)
					mSurfaceHolder.unlockCanvasAndPost(c);
			}			
			
		}
		
		/**
		 * Draws the coordinate system in which the robot is
		 * supposed to move.
		 */
		private void drawCoordinateSystem() {
			Canvas c= null;
			try {	
				c = mSurfaceHolder.lockCanvas(null);
				Bitmap mBackground=Bitmap.createScaledBitmap(bBackground, c.getWidth(), c.getHeight(), false);
				
				//define a rectangle that covers the entire screen
				//left, top, right, bottom
				RectF rect2 = new RectF(c.getWidth()/8, c.getHeight()/8, c.getWidth()/4, c.getHeight()/2+c.getHeight()/9);
				Paint mPaint = new Paint();
				mPaint.setAntiAlias(true);
				mPaint.setColor(Color.RED);
				mPaint.setStyle(Paint.Style.STROKE); 
				mPaint.setStrokeWidth(4.5f);
				//rect.offset(5, 5);
				c.drawRect(rect2, mPaint);
				
				
				
				//mBackground.getScaledHeight(c)
			} finally {
				if (c != null)
					mSurfaceHolder.unlockCanvasAndPost(c);
			}			
			
		}
		
		/**
		 * Draws a map and pointer on the given canvas.
		 * @param c
		 */
		public void doDraw(Canvas c) {
			/*
			 * set the origin-point for the coordinate-system, in which the robot
			 * is supposed to be drawn	
			 */
			final float POSX0= c.getWidth()*(float)(((1.2)/15) + (4.1/9));
			final float POSY0= c.getHeight()*(float)((1.1/7)+ (4.1/9));
			
			//TODO set robot starting position in the relative grid
			float posx = 0;
			float posy = 0;
			
			//load pointer image
			pointer = BitmapFactory.decodeResource(res, R.drawable.ic_launcher_nxt);
			//Bitmap.createScaledBitmap(src, dstWidth, dstHeight, filter)
			
			//draw the background image, this counts as clearing the screen
			Bitmap mBackground = null;
			mBackground=Bitmap.createScaledBitmap(bBackground, c.getWidth(), c.getHeight(), false);
			c.drawBitmap(mBackground, 0, 0, null);
			
			//draw the Pointer
			if (vPosActive == false) {
			c.drawBitmap(pointer, 
					POSX0-pointer.getWidth()/2, //draw left-side on POS0X and shift to center
					POSY0-pointer.getHeight()/2, //draw top-side on POS0Y and shift to center
					null);
			} else {
				c.drawBitmap(pointer, 
						vPOSX-pointer.getWidth()/2, //draw left-side on POS0X and shift to center
						vPOSY-pointer.getHeight()/2, //draw top-side on POS0Y and shift to center
						null);
			}
			
		}
		
		/**
		 * Executes the main algorithm of the thread.
		 */
		@Override		
		public void run() {
			while (mRun=true) {  //TODO add this again later
				Canvas c = null;
				try {	
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) {
						if (c != null) {  //FIXME This strangely leads to a freeze, without this line you get a NullPointerException
							doDraw(c);
						}
					}
				
				} finally {
					if (c != null)
						mSurfaceHolder.unlockCanvasAndPost(c);
				}			
			}
		}
		
		public void setVPOS(boolean b) {
			vPosActive=b;
		}
		
		public void setVPosX(float f) {
			vPOSX=f;
		}
		
		public void setVPosY(float f) {
			vPOSY=f;
		}
		
	}
	
	//------------------------------------
	// DECLARATION OF CLASS
	
	private MapThread thread;
	private SurfaceHolder mapholder;
	private Context context;
	
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
	public void surfaceChanged(SurfaceHolder holder, int format,int width,int height) {
		//get current screen orientation
		//int orientation = this.getResources().getConfiguration().orientation;
		
		//check screen orientation
		//1 = PORTRAIT
		//2 = LANDSCAPE
		//if (orientation == 1) {
			//thread.clearScreen();
			
			//thread.drawCoordinateSystem();
			
		//}
		//else if (orientation == 2){
			//thread.clearScreen();
			//thread.drawMap();
	//	}
		
	}
	
	/**
	 * Callback invoked when the Surface has been created and is ready to
	 * be used.
	 */
	public void surfaceCreated(SurfaceHolder holder){
		//start thread
		//thread = new MapThread(mapholder, context, new Handler());
		thread.setRunning(true);
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
	
	public void setVPointer(boolean b) {
		thread.setVPOS(b);
	}
	
	public void setVPosX(float f) {
		thread.setVPosX(f);	
	}
	
	public void setVPosY(float f) {
		thread.setVPosY(f);
	}
	
}
