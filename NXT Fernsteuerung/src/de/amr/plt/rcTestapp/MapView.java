package de.amr.plt.rcTestapp;

import java.util.ArrayList;

import de.amr.plt.rcParkingRobot.IAndroidHmi.ParkingSlot;
import de.amr.plt.rcParkingRobot.IAndroidHmi.ParkingSlot.ParkingSlotStatus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

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
	 * @version 0.8.0
	 */
	static class MapThread extends Thread {
		//finals
		//Variable for default background color
		final private int CLEAR=0xff000000;
		
		//Scaling parameter for drawing Pose in cm on screen
		final private float PX_SCALE = ((110/27));
		
		//Bitmap matrix
		Matrix matrix;
		
		//Variables for Menu Button properties
		final private int BUTTON_COUNT=4;
		final private int BUTTON_WIDTH=60;
		
		//Array containing the current and past locations of the pointer
		final private int MAX_PATH_PTS = 1056;
		private float[] path = new float[MAX_PATH_PTS+2];	
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
		private boolean mPaused = false;
		private Resources res;
		private boolean vPosActive;
		private float vPOSX = 0;
		private float vPOSY = 0;
		private float dPOSX = 0;
		private float dPOSY = 0;
		private float ANGLE0 = 0;
		private float dANGLE = 0;
		Bitmap bBackground;  //original bitmap of Background
		Bitmap mBackground;  //scaled bitmap of Background
		Bitmap bRobot;
		Bitmap pointer;
		Bitmap rPointer;
		
		//paint
		Paint BUTTON_COLOR;  //color for general button rects
		Paint PARKINGSLOT_GOOD; //color for parking slots, which fit the robot
		Paint PARKINGSLOT_BAD; //color for parking slots, which do not fit the robot
		Paint PARKINGSLOT_RESCAN;
		Paint PARKINGSLOT_SELECTED; //color for selected slots|
		int paintcounter=0x00;
		boolean stepdir=true;
		
		
		//variables for parking slot
		ArrayList<ParkingSlot> ParkingSlot;
		ArrayList<RectF> aParkingSlotRectF;
		int ParkingSlotSelectionID = (-1);
		
		//Constructor
		public MapThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
			mSurfaceHolder=surfaceHolder;
			mContext=context;
			mHandler=handler;
			
			//create Matrix
			matrix = new Matrix();
			
			//fetch Resources
			res = mContext.getResources();
			
			//build ArrayLists
			aParkingSlotRectF = new ArrayList<RectF>();
			ParkingSlot = new ArrayList<ParkingSlot>();
			
			//initialize all Bitmaps
			//BitmapFactory.Options options=new BitmapFactory.Options();
			//options.inSampleSize = 8;
			
			bBackground = BitmapFactory.decodeResource(res, R.drawable.bg_map);	
			pointer = BitmapFactory.decodeResource(res, R.drawable.spr_ptr_nxtbot);			
			
			//create a paint-set for the color of selection areas
			BUTTON_COLOR= new Paint();
			PARKINGSLOT_GOOD= new Paint();
			PARKINGSLOT_BAD=new Paint();
			PARKINGSLOT_RESCAN=new Paint();
			PARKINGSLOT_SELECTED=new Paint();
				
			BUTTON_COLOR.setColor(Color.YELLOW);
			BUTTON_COLOR.setStyle(Paint.Style.STROKE); 
			BUTTON_COLOR.setStrokeWidth(4.5f);
			
			PARKINGSLOT_GOOD.setColor(Color.GREEN);
			PARKINGSLOT_GOOD.setStyle(Paint.Style.STROKE);
			PARKINGSLOT_GOOD.setStrokeWidth(4.5f);
			
			PARKINGSLOT_BAD.setColor(Color.RED);
			PARKINGSLOT_BAD.setStyle(Paint.Style.STROKE);
			PARKINGSLOT_BAD.setStrokeWidth(4.5f);
			
			PARKINGSLOT_RESCAN.setColor(Color.DKGRAY);
			PARKINGSLOT_RESCAN.setStyle(Paint.Style.STROKE);
			PARKINGSLOT_RESCAN.setStrokeWidth(4.5f);
			
			PARKINGSLOT_SELECTED.setColor(Color.CYAN);
			PARKINGSLOT_SELECTED.setStyle(Paint.Style.STROKE);
			PARKINGSLOT_SELECTED.setStrokeWidth(4.5f);
			
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
				c.drawRect(rect2, mPaint);
				
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
			
			/*
			 * set the origin-point for the coordinate-system, in which the robot
			 * is supposed to be drawn	
			 */
			float POSY0= c.getHeight()*(float)((1.1/7)+ (4.2/9));
			float POSX0= c.getWidth()*(float)(1.3/15.5);

			
			
			//load pointer image
			//pointer = BitmapFactory.decodeResource(res, R.drawable.ic_launcher_nxt);
			
			//clear the Screen
			c.drawColor(CLEAR);
			
			//draw the background image, scale it so that it is left to the Buttons
			if (map_visible) {
			//Bitmap mBackground = Bitmap.createBitmap(bBackground, 0, 0, c.getWidth(), c.getHeight());
			c.drawBitmap(mBackground, 0, 0, null);
			}
			
			
			//c.drawPoints(pts, offset, count, paint)
			//draw the Pointer
			if (vPosActive == false) {
				alignPath(POSX0, POSY0);
			c.drawPoints(path, 0 , MAX_PATH_PTS , BUTTON_COLOR);
			rPointer=rotateBitmap(pointer, dANGLE);
			//Log.d("MapView","Drawing pointer X0="+POSX0+" Y0="+POSY0+" dX="+dPOSX+" dY="+dPOSY+" Angle:"+dANGLE);
			c.drawBitmap(rPointer, 
					POSX0+(dPOSX*PX_SCALE)-rPointer.getWidth()/2, //draw left-side on POS0X and shift to center
					POSY0-(dPOSY*PX_SCALE)-rPointer.getHeight()/2, //draw top-side on POS0Y and shift to center
					null);
			} else {
				alignPath(vPOSX, vPOSY);
				c.drawPoints(path, 0, MAX_PATH_PTS, BUTTON_COLOR);
				rPointer=rotateBitmap(pointer, dANGLE);
				//Log.d("MapView","Drawing vpointer");
				c.drawBitmap(pointer, 
						POSX0+vPOSX-pointer.getWidth()/2, //draw left-side on POS0X and shift to center
						POSY0-vPOSY-pointer.getHeight()/2, //draw top-side on POS0Y and shift to center
						null);
			}
			
			//FIXME BUTTON_COLOR-Spielerei
			BUTTON_COLOR.setColor(Color.rgb(paintcounter, 255-paintcounter, paintcounter/2));
			
			if (stepdir == true) {
				paintcounter++;
				if (paintcounter >= 255) {
					paintcounter=255;
					stepdir=false;
				}		
			}
			else {			
				paintcounter--;
				if (paintcounter <=0) {
					paintcounter=0;
					stepdir=true;
				}
			}
				
			
			//draw all known ParkingSlots
			try {
				aParkingSlotRectF.clear();
				for (int i=0; i < this.ParkingSlot.size(); i++) {
					RectF ParkingSlotRect = new RectF(
							this.ParkingSlot.get(i).getBackBoundaryPosition().x*PX_SCALE,
							this.ParkingSlot.get(i).getBackBoundaryPosition().y*PX_SCALE,
							this.ParkingSlot.get(i).getFrontBoundaryPosition().x*PX_SCALE,
							this.ParkingSlot.get(i).getFrontBoundaryPosition().y*PX_SCALE);
					aParkingSlotRectF.add(ParkingSlotRect); //save the slot in a list
					
					//check if the last MotionEvent marks the RectF as selected and propagate
					Log.d("doDraw","histEvent[" + histEvent.getX() + " | "+histEvent.getY()+"] ParkingSlot [left:"+ParkingSlotRect.left+" right:"+ParkingSlotRect.right+" bottom:"+ParkingSlotRect.bottom+" top:"+ParkingSlotRect.top+"]");
					if ( (histEvent.getX() > ParkingSlotRect.left) //is the touch point inside the RectF?
							&& (histEvent.getX() < ParkingSlotRect.right) 
							&& (histEvent.getY() < ParkingSlotRect.bottom)
							&& (histEvent.getY() > ParkingSlotRect.top)
							&& (this.ParkingSlot.get(i).getParkingSlotStatus()==ParkingSlotStatus.GOOD)) { //and is it okay to select?
								c.drawRect(ParkingSlotRect, PARKINGSLOT_SELECTED); //if yes, draw it as selected RectF and save, which one it is
								ParkingSlotSelectionID=i; 
								Log.d("doDraw", "ParkingSlotSelection: "+i);
					}									
					else {
								switch (this.ParkingSlot.get(i).getParkingSlotStatus()) {  //switch draw color depending on Status of Parking Slot
								case GOOD: c.drawRect(ParkingSlotRect, PARKINGSLOT_GOOD); break;
								case BAD: c.drawRect(ParkingSlotRect, PARKINGSLOT_BAD); break;
								case RESCAN: c.drawRect(ParkingSlotRect, PARKINGSLOT_RESCAN); break;
								}
							}			
				}
			} 
			catch (NullPointerException e) {
				Log.e("doDraw", e.getMessage() + ": Skipping drawing of ParkingSlots!");
			}
			catch (IndexOutOfBoundsException e) {
				Log.e("doDraw", e.getMessage()+ ": Skipping drawing of ParkingSlots!");
			}
			
		}
		
		/**
		 * Executes the main algorithm of the thread. This is not to be called
		 * directly.
		 */		
		@Override
		public void run() {
			//at start, initialize Background once
			Canvas c=null;
			//scale Background and pointer
			try {
				c = mSurfaceHolder.lockCanvas(null);
				synchronized (mSurfaceHolder) {
					mBackground=Bitmap.createScaledBitmap(bBackground, 
							c.getWidth(),
							c.getHeight(), 
							false);
					
					pointer=Bitmap.createScaledBitmap(pointer,
							pointer.getWidth()/2,
							pointer.getHeight()/2,
							false);
				}
			}
			finally {
				if (c != null)
					mSurfaceHolder.unlockCanvasAndPost(c);
			}			
			
			//enter run loop
			while (mRun=true) {
				while (mPaused) {
					try {
						Thread.sleep(50L);
					} catch (InterruptedException ignore) {
					}
				}
				c = null;				
				try {	
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) {
						if (c != null) {//FIXME This strangely leads to a freeze, without this line you get a NullPointerException						
							doDraw(c);
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
		private boolean alignPath(float POSX0, float POSY0) {
			try {
			if (pathcount < MAX_PATH_PTS && pathcount>=0) {
				path[pathcount]=POSX0+dPOSX*PX_SCALE;
				path[pathcount+1]=POSY0-dPOSY*PX_SCALE;
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
			synchronized (mSurfaceHolder) {
				dPOSX=fx;
				dPOSY=fy;
			}
		}
		
		/**
		 * Sets the parameter for the relative angle.
		 * @param angle
		 */
		public void setDAngle(float angle) {
			synchronized (mSurfaceHolder) {
				dANGLE=angle;
			}
		}
		
		/**
		 * Returns true if the vPointer is currently activated.
		 * @return
		 */
		public boolean vPointerIsActive() {
			return vPosActive;
		}
		
		/**
		 * Sets the ParkingSlot Array. Only passes the list
		 * if it contains elements.
		 * @param sa
		 */
		public void setParkingSlots(ArrayList<ParkingSlot> sa) {
			synchronized (mSurfaceHolder) {
				if (sa.size() > 0)
				ParkingSlot=sa;
			}
		}
		
		/**
		 * Returns the identification-number of the currently
		 * chosen Parking Slot.
		 * @return
		 */
		public int getParkingSlotSelectionID() {
			return ParkingSlotSelectionID;
		}
		
		/**
		 * Pauses or unpauses the thread.
		 * @param b
		 */
		public void setPaused(boolean b) {
			mPaused=b;
		}
		
		/**
		 * Returns whether the thread is paused or not.
		 * @return mPaused
		 */
		public boolean isPaused() {
			return mPaused;
		}
		
		/**
		 * Rotates a bitmap. The rotation angle has to be specified.
		 * @param source
		 * @param angle
		 * @return 
		 */
		private Bitmap rotateBitmap(Bitmap source, float angle) {
			matrix.reset();
			matrix.postRotate( (-angle) );
			return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
		}
		
	}
	
	/**
	 * Listens to gestures concerning every element which is transposed
	 * on the map canvas.
	 * @author Daniel Wohllebe
	 *
	 */
	private static MotionEvent histEvent = null;
	
	class MapGestureListener extends GestureDetector.SimpleOnGestureListener implements OnGenericMotionListener {
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
            return true;
        }
        
        @Override
        public void onLongPress(MotionEvent event) {
        	Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
        	//histEvent=event;
        }
        
        @Override
        public boolean onDoubleTap(MotionEvent event) {
        	Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
        	histEvent=event;
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
	private ArrayList<ParkingSlot> atParkingSlot;
	
	
	public MapView(Context context, AttributeSet attrs) {
		//initialize with given context
		super(context, attrs);
		
		//register interest to hear about surface changes
		SurfaceHolder mapholder=getHolder();
		mapholder.addCallback(this);
		
		//create a thread, this will be started on surface creation
		thread = new MapThread(mapholder, context, new Handler());
		
		//create a gesture listener
		mDetector = new GestureDetector(context, new MapGestureListener(), new Handler()); //FIXME	
		
		//create temporary ArrayList
		atParkingSlot = new ArrayList<ParkingSlot>();
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
	@Override
	public void surfaceCreated(SurfaceHolder holder){
		//start thread
		//thread = new MapThread(mapholder, context, new Handler());
		thread.setRunning(true);
		
		//if the Thread has been paused, then one instance already exists, so do not create another one
		if (!thread.isPaused())
			thread.start();
		else
			thread.setPaused(false); //resume paused thread
	}
	
	/**
	 * Callback invoked when the Surface has been destroyed and
	 * must no longer be touched.
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//halt thread and wait for it to finish
		Log.d("MapThread", "Entered surfaceDestroyed-Callback");
		thread.setPaused(true);
		
		/*boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				Log.d("MapThread", "MapThread joined");
				retry = false;
			} catch (InterruptedException e) {
				Log.e("MapThread", e.getMessage());
			  } 					
		}*/
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
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
	
	public void setPose(float posx, float posy, float angle) {
		thread.setDPos(posx, posy);
		thread.setDAngle(angle);
	}
	
	/**
	 * Adds a ParkingSlot to the List of Elements
	 * that should be drawn.
	 * @param ps
	 */
	public void addParkingSlot(ParkingSlot ps) {
		atParkingSlot.add(ps);
	}
	
	/**
	 * Pushes all Parking slots to the draw thread
	 * and clears the list of Parking slots.
	 */
	public void propagateParkingSlots() {
		thread.setParkingSlots(atParkingSlot);
		atParkingSlot.clear();
	}
	
	/**
	 * Returns the currently selected ParkingSlots' index number.
	 * @return
	 */
	public int getParkingSlotSelectionID() {
		return thread.getParkingSlotSelectionID();
	}
	
}
