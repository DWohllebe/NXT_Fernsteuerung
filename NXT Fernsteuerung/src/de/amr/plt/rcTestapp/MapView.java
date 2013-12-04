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
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
	 * @version 0.1.2
	 */
	static class MapThread extends Thread {
		//finals
		//Variable for default background color
		final private int CLEAR=0xff000000;
		
		//Variables for Menu Button properties
		final private int BUTTON_COUNT=4;
		final private int BUTTON_WIDTH=60;
		
		//Array containing the current and past locations of the pointer
		private float[] path = new float[20];
		private int pathcount=0;
		
		//last known motion event
		private MotionEvent histMotionEvent = null;
		
		//variables for drawing
		boolean draw_initialized;
		boolean button_hover;
		boolean button_pressed;
		boolean button_visible;
		boolean info_bar_visible=true;
		boolean map_visible=true;
		
		//variables
		private SurfaceHolder mSurfaceHolder;
		private Context mContext;
		private Handler mHandler;
		private boolean mRun = false;
		private Resources res;
		private boolean vPosActive;
		private float vPOSX = 0;
		private float vPOSY = 0;
		private float dPOSX = 0;
		private float dPOSY = 0;
		private float ANGLE0 = 0;
		private float dANGLE = 0;
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
		
/*		/**
		 * Draws graphics which are supposed to be static background or
		 * not part of a regular screen update.
		 * @param c
		 
		private void doInitDraw(Canvas c) {
			Paint BUTTON_COLOR= new Paint();
			BUTTON_COLOR.setColor(Color.RED);
			BUTTON_COLOR.setStyle(Paint.Style.STROKE); 
			BUTTON_COLOR.setStrokeWidth(4.5f);
			
			final float POSY0= c.getHeight()*(float)((1.1/7)+ (4.2/9));
			final float POSX0= c.getWidth()*(float)(1.3/15.5);
			
			//clear the Screen
			c.drawColor(CLEAR);
			
			//draw the background image, scale it so that it is left to the Buttons
			Bitmap mBackground = null;
			mBackground=Bitmap.createScaledBitmap(bBackground, c.getWidth()-BUTTON_WIDTH, c.getHeight(), false);
			c.drawBitmap(mBackground, 0, 0, null);
			
			//draw the Buttons on right-hand side
			for (int i=0; (i<BUTTON_COUNT); i++) {
				c.drawRect(new RectF(c.getWidth()-BUTTON_WIDTH, 
						i*c.getHeight()/BUTTON_COUNT, 
						c.getWidth(),
						c.getHeight()/BUTTON_COUNT*(1+i)), 
						BUTTON_COLOR);
			}
			 
		}
*/
		
		/**
		 * Draws all graphics which are supposed to change dynamically.
		 * @param c
		 */
		private void doDraw(Canvas c) {
			Paint BUTTON_COLOR= new Paint();
			BUTTON_COLOR.setColor(Color.RED);
			BUTTON_COLOR.setStyle(Paint.Style.STROKE); 
			BUTTON_COLOR.setStrokeWidth(4.5f);
			
			/*
			 * set the origin-point for the coordinate-system, in which the robot
			 * is supposed to be drawn	
			 */
			final float POSY0= c.getHeight()*(float)((1.1/7)+ (4.2/9));
			final float POSX0= c.getWidth()*(float)(1.3/15.5);

			
			
			//load pointer image
			pointer = BitmapFactory.decodeResource(res, R.drawable.ic_launcher_nxt);
			
			//clear the Screen
			c.drawColor(CLEAR);
			
			//draw the background image, scale it so that it is left to the Buttons
			if (map_visible) {
			Bitmap mBackground = null;
			mBackground=Bitmap.createScaledBitmap(bBackground, c.getWidth()-BUTTON_WIDTH, c.getHeight(), false);
			c.drawBitmap(mBackground, 0, 0, null);
			}
			
			
			//c.drawPoints(pts, offset, count, paint)
			//draw the Pointer
			if (vPosActive == false) {
				alignPath(POSX0, POSY0);
			c.drawPoints(path, 0 , 10 , BUTTON_COLOR);
			c.drawBitmap(pointer, 
					POSX0+dPOSX-pointer.getWidth()/2, //draw left-side on POS0X and shift to center
					POSY0+dPOSY-pointer.getHeight()/2, //draw top-side on POS0Y and shift to center
					null);
			} else {
				alignPath(vPOSX, vPOSY);
				c.drawPoints(path, 0, 10, BUTTON_COLOR);
				c.drawBitmap(pointer, 
						vPOSX-pointer.getWidth()/2, //draw left-side on POS0X and shift to center
						vPOSY-pointer.getHeight()/2, //draw top-side on POS0Y and shift to center
						null);
			}
			
			//draw the info bar
			if (info_bar_visible) {
			Bitmap mInfobar = BitmapFactory.decodeResource(res, R.drawable.infobalken_z);
			mInfobar=Bitmap.createScaledBitmap(mInfobar, c.getWidth(), mInfobar.getHeight(), false);
			c.drawBitmap(mInfobar, 0, c.getHeight()-mInfobar.getHeight(), null);
			}
			
			//draw the Buttons on right-hand side
			for (int i=0; (i<BUTTON_COUNT); i++) {
				c.drawRect(new RectF(c.getWidth()-BUTTON_WIDTH, 
						i*c.getHeight()/BUTTON_COUNT, 
						c.getWidth(),
						c.getHeight()/BUTTON_COUNT*(1+i)), 
						BUTTON_COLOR);
						//c.getHeight()/BUTTON_COUNT+i*c.getHeight()/BUTTON_COUNT);			
			}
		}
		
		/**
		 * Executes the main algorithm of the thread. This is not to be called
		 * directly.
		 */		
		public void run() {
			while (mRun=true) {
				Canvas c = null;				
				try {	
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) {
						if (c != null) {//FIXME This strangely leads to a freeze, without this line you get a NullPointerException						
							doDraw(c); //
						}
					}
				
				} finally {
					if (c != null)
						mSurfaceHolder.unlockCanvasAndPost(c);
				}			
			}
		}
		
		/**
		 * Aligns path to given view point with known relative
		 * deviation. Returns true if successful.
		 * @param POSX0
		 * @param POSY0
		 * @return success
		 */
		private boolean alignPath(float POSX0, float POSY0) { //TODO integrate / cleanup
			try {
			if (pathcount<18 && pathcount>=0) {
				path[pathcount]=dPOSX+POSX0;
				path[pathcount+1]=dPOSY=POSY0;
				pathcount=pathcount+2;
				return true;
			}
			else
				pathcount=0;
				return false;
			}
			catch (NullPointerException e) {
				pathcount=0;
				Log.d("MapView","alignPath:",e);
				return false;
			}
		}
		
		/**
		 * Activates virtual position drawing.
		 * @param b
		 */
		public void activateVPos(boolean b) {
			vPosActive=b;
		}
		
		/**
		 * Sets virtual position parameters.
		 * @param fx
		 * @param fy
		 */
		public void setVPos(float fx, float fy) {
			vPOSX=fx;
			vPOSY=fy;
		}
		
		/**
		 * Sets the parameter for the relative distance from the
		 * origin point.
		 * @param fx
		 * @param fy
		 */
		public void setDPos(float fx, float fy) {
			dPOSX=fx;
			dPOSY=fy;
		}
		
		/**
		 * Returns true, if the vPointer is currently activated.
		 * @return
		 */
		public boolean vPointerIsActive() {
			return vPosActive;
		}
		
	}
	
	/**
	 * Listens to gestures concerning every element which is transposed
	 * on the map canvas.
	 * @author Daniel Wohllebe
	 *
	 */
	private MotionEvent histEvent;
	
	class MapGestureListener extends GestureDetector.SimpleOnGestureListener implements OnGenericMotionListener { //TODO Test this.
        private static final String DEBUG_TAG = "Gestures"; 
       // protected MotionEventHandler mHandler = new MotionEventHandler();
       // Message message = new Message();
        
        /*
        Callback callback = new Callback() {
        	public boolean handleMessage(Message msg) {
        		
        	}
        }; */
        
        @Override
        public boolean onDown(MotionEvent event) { 
            Log.d(DEBUG_TAG,"onDown: " + event.toString());
            histEvent=event;
            return true;
            
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
            histEvent=event;
           // event.getX(); //TODO get X and compare with a RectF
            return true;
        }
        
        public boolean onGenericMotion(View view, MotionEvent event) { //TODO make callback work
        	Log.d("MotionCallback","Callback recieved.");
        	return true;
        }
        
        public MotionEvent getLastEvent() {
        	return histEvent;
        }
        
        //define a handler to pass information about Motion Inputs
     /*   class MotionEventHandler extends Handler {
        	
        	public MotionEventHandler(Handler.Callback cb) {
        		super(cb); //FIXME
        	}
        	
        	
        	
        
        } */
    }

	
	//------------------------------------
	// DECLARATION OF CLASS
	
	private MapThread thread;
	private SurfaceHolder mapholder;
	private Context context;
	private MapGestureListener GestureListener;
	private GestureDetector mDetector;
	
	
	public MapView(Context context, AttributeSet attrs) {
		//initialize with given context
		super(context, attrs);
		
		//register interest to hear about surface changes
		SurfaceHolder mapholder=getHolder();
		mapholder.addCallback(this);
		
		//create a thread, this will be started on surface creation
		thread = new MapThread(mapholder, context, new Handler());
		
		//create a gesture listener
		mDetector = new GestureDetector(new MapGestureListener()); //FIXME		
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
	
	@Override
	public boolean onTouchEvent(MotionEvent event) { //FIXME implement Touch-Detection
		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}
	
	
	public void setVPointer(boolean b) {
		thread.activateVPos(b);
	}
	
	public void setVPos(float fx, float fy) {
		thread.setVPos(fx, fy);	
	}
	
	public boolean vPointerIsActive() {
		return thread.vPointerIsActive();
	}
	
	public int getMenuButtonCount() {
		return thread.BUTTON_COUNT;	
	}
	
	public int getMenuButtonWidth() {
		return thread.BUTTON_WIDTH;
	}
	
	public void setMapVisibility(boolean b) {
		thread.map_visible=b;
	}
	
	public void setInfoBarVisibility(boolean b) {
		thread.info_bar_visible=b;
	}
	
}
