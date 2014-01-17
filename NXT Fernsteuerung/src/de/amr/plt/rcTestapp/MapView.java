package de.amr.plt.rcTestapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import android.graphics.PointF;
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
 * @version 1.0
 *
 */
public class MapView extends SurfaceView implements SurfaceHolder.Callback {
	
	//------------------------------------------
	// DECLARATION OF THREAD
	/**
	 * Thread of class MapView. Handles all drawing functionality.
	 * @author Daniel Wohllebe
	 * @version 1.0.0
	 */
	static class MapThread extends Thread {
		//::: Variable for default background color :::
		final private int CLEAR=0xff000000;
		
		//::: Scaling parameter for drawing Pose in cm on screen :::
		final private float PX_SCALE_X = (float)383/100;
		final private float PX_SCALE_Y = (float)394/100;
		
		//::: Scale variables for the pointer :::
		//pointer size = original size / POINTER_DIV
		final private double POINTER_DIV_WIDTH=1.5;
		final private double POINTER_DIV_HEIGHT=1.5;
		
		//::: Defines for drawing Method of ParkingSlots :::	
		//Range, in which the Parking Slot is allowed to have a deviation
		//when trying do draw a Rectangle for the PS-Selection
		//the Range is 1-PS_RANGE to 1+_PSRANGE
		final private double PS_RANGE=0.1;
		//Width of the drawn Rectangle
		final private int PS_WIDTH=80;
								
		
		//::: Variables concerning the offset of the distance sensor :::
				//the offsets are seen from the middle point of the robot
				final private float SENSOR_FRONT_ANGLE=			0;
				final private float SENSOR_FRONTSIDE_ANGLE=		-90;
				final private float SENSOR_BACK_ANGLE=			180;
				final private float SENSOR_BACKSIDE_ANGLE=		-90;

		//::: sensor information variables :::
				private double dDISTFRONT=0;
				private double dDISTFRONTSIDE=0;
				private double dDISTBACK=0;
				private double dDISTBACKSIDE=0;
				
				final private double dDISTSCALE=0.1;
				final private double SENSOR_START_VECTOR_LENGTH=30;
		
		//::: Bitmap matrix :::
		private Matrix matrix;
		
		//::: Variables for Menu Button properties (deprecated) :::
		//final private int BUTTON_COUNT=4;
		//final private int BUTTON_WIDTH=60;
		
		//::: Array containing the current and past locations of the pointer :::
		final private int MAX_PATH_PTS = 1056;
		private float[] path = new float[MAX_PATH_PTS+2];	
		private int pathcount=0;
		
		//::: last known motion event (deprecated) :::
		//private MotionEvent histMotionEvent = null;
		
		//::: variables for drawing :::
		private boolean map_visible=true;
		//private boolean draw_initialized;
		//private boolean button_hover;
		//private boolean button_pressed;
		//private boolean button_visible;
		//private boolean info_bar_visible=true;
		
		
		//::: other variables :::
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
		//private float ANGLE0 = 0;
		private float dANGLE = 0;
		private Bitmap bBackground;  //original bitmap of Background
		private Bitmap mBackground;  //scaled bitmap of Background
		//private Bitmap bRobot;
		private Bitmap pointer;
		private Bitmap rPointer;
		
		//::: paint :::
		private Paint BUTTON_COLOR;  //color for general button rectangles (also default color for testing)
		private Paint PARKINGSLOT_GOOD; //color for parking slots, which fit the robot
		private Paint PARKINGSLOT_BAD; //color for parking slots, which do not fit the robot
		private Paint PARKINGSLOT_RESCAN; //color for parking slots which need to be rescaned
		private Paint PARKINGSLOT_SELECTED; //color for selected slots
		private Paint DISTANCE_SENSOR_COLOR; //color for distance sensor visualisation
		private int paintcounter=0x00;
		private boolean stepdir=true;  //direction for color-switching of path
		
		
		//::: variables for parking slot :::
		private List<ParkingSlot> ParkingSlot;	
		private int ParkingSlotSelectionID = (-1);
		//ArrayList<ParkingSlot> ParkingSlot;
		//private List<RectF> aParkingSlotRectF;
		
		//maximum capacity of ParkingSlots to be saved
		//always ensure this is the same values as aPS_LIST_CAPACITY
		final private int PS_LIST_CAPACITY = 10;
		
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
			//aParkingSlotRectF = Collections.synchronizedList(new ArrayList<RectF>());
			ParkingSlot = Collections.synchronizedList(new ArrayList<ParkingSlot>());
			for (int i=0; i<PS_LIST_CAPACITY; i++) {
				ParkingSlot.add(new ParkingSlot(i, null, null, null));
			}
			
			//ParkingSlot = new ArrayList<ParkingSlot>();
			
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
			DISTANCE_SENSOR_COLOR=new Paint();
				
			BUTTON_COLOR.setColor(Color.YELLOW);
			BUTTON_COLOR.setStyle(Paint.Style.STROKE); 
			BUTTON_COLOR.setStrokeWidth(4.5f);
			
			PARKINGSLOT_GOOD.setColor(Color.GREEN);
			PARKINGSLOT_GOOD.setStyle(Paint.Style.STROKE);
			PARKINGSLOT_GOOD.setStrokeWidth(2.5f);
			
			PARKINGSLOT_BAD.setColor(Color.RED);
			PARKINGSLOT_BAD.setStyle(Paint.Style.STROKE);
			PARKINGSLOT_BAD.setStrokeWidth(2.5f);
			
			PARKINGSLOT_RESCAN.setColor(Color.DKGRAY);
			PARKINGSLOT_RESCAN.setStyle(Paint.Style.STROKE);
			PARKINGSLOT_RESCAN.setStrokeWidth(2.5f);
			
			PARKINGSLOT_SELECTED.setColor(Color.CYAN);
			PARKINGSLOT_SELECTED.setStyle(Paint.Style.STROKE);
			PARKINGSLOT_SELECTED.setStrokeWidth(3.5f);
			
			DISTANCE_SENSOR_COLOR.setColor(Color.BLUE);
			DISTANCE_SENSOR_COLOR.setStyle(Paint.Style.STROKE);
			DISTANCE_SENSOR_COLOR.setStrokeWidth(1.5f);
			
			//ParkingSlot.add(new ParkingSlot(0, new PointF(180 ,1), new PointF(1,1), ParkingSlotStatus.GOOD));
			//ParkingSlot.add(new ParkingSlot(0, new PointF(180 ,5), new PointF(180,40), ParkingSlotStatus.GOOD));
			//ParkingSlot.add(new ParkingSlot(0, new PointF(1 ,40), new PointF(1,20), ParkingSlotStatus.BAD));
			//ParkingSlot.add(new ParkingSlot(0, new PointF(1 ,1), new PointF(180,1), ParkingSlotStatus.GOOD));

		}
		
		public void setRunning(boolean b) {
			mRun = b;
		}
		
		/**
		 * Method for testing various Draw-related functions.
		 * @deprecated
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
		 * @deprecated
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
		
			//clear the Screen
			c.drawColor(CLEAR);
			
			//draw the background image, scale it so that it is left to the Buttons
			if (map_visible) {
			c.drawBitmap(mBackground, 0, 0, null);
			}
			
			//draw all known ParkingSlots
			try {
				//aParkingSlotRectF.clear();
				for (int i=0; i < this.ParkingSlot.size(); i++) {
					
					/*
					 * --- Check the orientation of the Rectangle ---
					 * This works as follows: 
					 * -> check if there are position variables of same dimension within a certain acceptable range
					 * -> divide BackPos by FrontPos and get deviation (also check for zero)
					 * -> if within acceptable range, this is not the changing variable
					 * -> check the actual changing variable for orientation
					 * -> determine drawing values correctly
					 * This is necessary, so that the values can be properly chosen later.
					 * 
					 * Attention: If the values are too small, then the RectF can
					 * not be drawn. This can be applied as a filter for very small
					 * ParkingSlots (which are an error anyway).
					 * 
					 *  Notice: Back is passed first, front is passed last
					 */
					float top=0;
					float right=0;
					float bottom=0;
					float left=0;
					double mDeviation;
					
					double xbackboundary=(double)(this.ParkingSlot.get(i).getBackBoundaryPosition().x);				
					double xfrontboundary=(double)(this.ParkingSlot.get(i).getFrontBoundaryPosition().x);
					double ybackboundary=(double)(this.ParkingSlot.get(i).getBackBoundaryPosition().y);
					double yfrontboundary=(double)(this.ParkingSlot.get(i).getBackBoundaryPosition().y);
					
					//filter zeros
					if (xbackboundary == 0)
						xbackboundary  += 0.1;
					
					if (xfrontboundary == 0)
						xfrontboundary  +=0.1;
					
					if (ybackboundary == 0)
						ybackboundary += 0.1;
					
					if (yfrontboundary == 0)
						yfrontboundary += 0.1;
					
					//x: check if values are within range
					mDeviation = ( xbackboundary / xfrontboundary );
					if ( (mDeviation > 1-PS_RANGE) && (mDeviation < 1+PS_RANGE) ) {
						//then y: determine orientation based on y statements
						if (this.ParkingSlot.get(i).getBackBoundaryPosition().y < this.ParkingSlot.get(i).getFrontBoundaryPosition().y) {
							// yBack -> yFront    	| x const.
							bottom = POSY0-this.ParkingSlot.get(i).getBackBoundaryPosition().y*PX_SCALE_Y;
							top = POSY0-this.ParkingSlot.get(i).getFrontBoundaryPosition().y*PX_SCALE_Y;
							left = POSX0+this.ParkingSlot.get(i).getBackBoundaryPosition().x*PX_SCALE_X;
							right = left+PS_WIDTH;
						}
						else {
							//yFront -> yBack		| x const.
							bottom = POSY0-this.ParkingSlot.get(i).getFrontBoundaryPosition().y*PX_SCALE_Y;
							top = POSY0-this.ParkingSlot.get(i).getBackBoundaryPosition().y*PX_SCALE_Y;
							right = POSX0+this.ParkingSlot.get(i).getBackBoundaryPosition().x*PX_SCALE_X;
							left = right-PS_WIDTH;
						}
						
					} //y: check if variables are within range
					else {
						mDeviation= (ybackboundary / yfrontboundary);
						if  ( (mDeviation > 1-PS_RANGE) && (mDeviation < 1+PS_RANGE) ) {
							//then x: determine orientation based on x statements
							if (this.ParkingSlot.get(i).getBackBoundaryPosition().x < this.ParkingSlot.get(i).getFrontBoundaryPosition().x) {
								// xBack -> xFront		| y const.
								top = POSY0-this.ParkingSlot.get(i).getBackBoundaryPosition().y*PX_SCALE_Y;
								bottom = POSY0+PS_WIDTH;
								left = POSX0+this.ParkingSlot.get(i).getBackBoundaryPosition().x*PX_SCALE_X;
								right = POSX0+this.ParkingSlot.get(i).getFrontBoundaryPosition().x*PX_SCALE_X;
							}
							else {
								// xFront -> xBack		| y const.
								bottom = POSY0-this.ParkingSlot.get(i).getBackBoundaryPosition().y*PX_SCALE_Y;
								top = bottom-PS_WIDTH;
								right = POSX0+this.ParkingSlot.get(i).getBackBoundaryPosition().x*PX_SCALE_X;
								left = POSX0+this.ParkingSlot.get(i).getFrontBoundaryPosition().x*PX_SCALE_X;			
							}
						}
					}
					
					RectF ParkingSlotRect = new RectF(left, top, right, bottom);
					
					//check if the last MotionEvent marks the RectF as selected and propagate
					//Log.d("doDraw","histEvent[" + histEvent.getX() + " | "+histEvent.getY()+"] ParkingSlot [left:"+ParkingSlotRect.left+" right:"+ParkingSlotRect.right+" bottom:"+ParkingSlotRect.bottom+" top:"+ParkingSlotRect.top+"]");
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
				//this is expected behavior for all unfilled slots
				//Log.e("doDraw", e.getMessage() + ": Skipping drawing of ParkingSlots!");
			}
			catch (IndexOutOfBoundsException e) {
				//Log.e("doDraw", e.getMessage()+ ": Skipping drawing of ParkingSlots!");
			}
			
			//c.drawPoints(pts, offset, count, paint)
			//draw the Pointer
			if (vPosActive == false) {
				alignPath(POSX0, POSY0);
			c.drawPoints(path, 0 , MAX_PATH_PTS , BUTTON_COLOR);
			rPointer=rotateBitmap(pointer, dANGLE);
			//Log.d("MapView","Drawing pointer X0="+POSX0+" Y0="+POSY0+" dX="+dPOSX+" dY="+dPOSY+" Angle:"+dANGLE);
			c.drawBitmap(rPointer, 
					POSX0+(dPOSX*PX_SCALE_X)-rPointer.getWidth()/2, //draw left-side on POS0X and shift to center
					POSY0-(dPOSY*PX_SCALE_Y)-rPointer.getHeight()/2, //draw top-side on POS0Y and shift to center
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
			
			//*---color scheme changer of BUTTON_COLOR*---
			/*
			 * This drawing method simply walks through a determined path of RGB values.
			 * When it has reached the maximum RGB spectrum, it traces its steps backwards.
			 */
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
				
			//*-----draw Sensor Information as a line----*   <- a MASTERPIECE!
			/*
			 * Basic way this works:
			 * - set a starting point on a circle (with the same middlepoint as the pointer)
			 * - set a second point from this point to the destination determined by the sensor values, given in polar coordinates (these are converted)
			 * - draw cool lines
			 * - enjoy
			 */
			double frontside_anchor_pt_x=dPOSX*PX_SCALE_X+convertToCartesianX(SENSOR_START_VECTOR_LENGTH,dANGLE-45);
			double frontside_anchor_pt_y=dPOSY*PX_SCALE_Y+convertToCartesianY(SENSOR_START_VECTOR_LENGTH,dANGLE-45);
			
			double backside_anchor_pt_x=dPOSX*PX_SCALE_X+convertToCartesianX(SENSOR_START_VECTOR_LENGTH, dANGLE-135);
			double backside_anchor_pt_y=dPOSY*PX_SCALE_Y+convertToCartesianY(SENSOR_START_VECTOR_LENGTH, dANGLE-135);
			
			double front_anchor_pt_x=dPOSX*PX_SCALE_X+convertToCartesianX(SENSOR_START_VECTOR_LENGTH, dANGLE);
			double front_anchor_pt_y=dPOSY*PX_SCALE_Y+convertToCartesianY(SENSOR_START_VECTOR_LENGTH, dANGLE);
			
			double back_anchor_pt_x=dPOSX*PX_SCALE_X+convertToCartesianX(SENSOR_START_VECTOR_LENGTH, dANGLE+180);
			double back_anchor_pt_y=dPOSY*PX_SCALE_Y+convertToCartesianY(SENSOR_START_VECTOR_LENGTH, dANGLE+180);
			
			//FRONT SENSOR
			c.drawLine(	(float)	(POSX0+front_anchor_pt_x), 
					(float)	(POSY0-front_anchor_pt_y), 
					(float)	(POSX0+front_anchor_pt_x+convertToCartesianX(dDISTFRONT, SENSOR_FRONT_ANGLE+dANGLE)*PX_SCALE_X), 
					(float)	(POSY0-(front_anchor_pt_y+convertToCartesianY(dDISTFRONT, SENSOR_FRONT_ANGLE+dANGLE)*PX_SCALE_Y)),
					DISTANCE_SENSOR_COLOR);
			
			//FRONTSIDE SENSOR
			c.drawLine(	(float)	(POSX0+frontside_anchor_pt_x), 
						(float)	(POSY0-frontside_anchor_pt_y), 
						(float)	(POSX0+frontside_anchor_pt_x+convertToCartesianX(dDISTFRONTSIDE, SENSOR_FRONTSIDE_ANGLE+dANGLE)*PX_SCALE_X), 
						(float)	(POSY0-(frontside_anchor_pt_y+convertToCartesianY(dDISTFRONTSIDE, SENSOR_FRONTSIDE_ANGLE+dANGLE)*PX_SCALE_Y)),
						DISTANCE_SENSOR_COLOR);
			
			//BACK SENSOR
			c.drawLine(	(float)	(POSX0+back_anchor_pt_x), 
					(float)	(POSY0-back_anchor_pt_y), 
					(float)	(POSX0+back_anchor_pt_x+convertToCartesianX(dDISTBACK, SENSOR_BACK_ANGLE+dANGLE)*PX_SCALE_X), 
					(float)	(POSY0-(back_anchor_pt_y+convertToCartesianY(dDISTBACK, SENSOR_BACK_ANGLE+dANGLE)*PX_SCALE_Y)),
					DISTANCE_SENSOR_COLOR);
			
			//BACKSIDE SENSOR
			c.drawLine(	(float)	(POSX0+backside_anchor_pt_x), 
					(float)	(POSY0-backside_anchor_pt_y), 
					(float)	(POSX0+backside_anchor_pt_x+convertToCartesianX(dDISTBACKSIDE, SENSOR_BACKSIDE_ANGLE+dANGLE)*PX_SCALE_X), 
					(float)	(POSY0-(backside_anchor_pt_y+convertToCartesianY(dDISTBACKSIDE, SENSOR_BACKSIDE_ANGLE+dANGLE)*PX_SCALE_Y)),
					DISTANCE_SENSOR_COLOR);		
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
							(int)(pointer.getWidth()/POINTER_DIV_WIDTH),
							(int)(pointer.getHeight()/POINTER_DIV_HEIGHT),
							false);
					//ptr_std_height=pointer.getScaledWidth(c);
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
						if (c != null) {					
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
				path[pathcount]=POSX0+dPOSX*PX_SCALE_X;
				path[pathcount+1]=POSY0-dPOSY*PX_SCALE_Y;
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
		 * @return vPosActive
		 */
		public boolean vPointerIsActive() {
			return vPosActive;
		}
		
		/**
		 * Sets the ParkingSlot Array. Only passes the list
		 * if it contains elements.
		 * @param atParkingSlot
		 */
		public synchronized void setParkingSlots(List<ParkingSlot> atParkingSlot) {
			synchronized (mSurfaceHolder) {			
				//if (atParkingSlot != ParkingSlot)
				ParkingSlot=atParkingSlot;
			}
		}
		
		/**
		 * Adds a ParkingSlot to the end of
		 * the list.
		 * @param ps
		 */
		public void addParkingSlot(ParkingSlot ps) {
			synchronized (mSurfaceHolder) {
				ParkingSlot.add(ps);
			}
		}
		
		/**
		 * Sets the ParkingSlot at the given index.
		 * The index can not be greater than PS_LIST_CAPACITY.
		 * @param ps
		 * @param index
		 */
		public synchronized void setParkingSlot(ParkingSlot ps, int index) {
			synchronized (mSurfaceHolder) {
				ParkingSlot.set(index, ps);
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
		 * Sets the sensor values, which are then drawn in doDraw().
		 * Sensor values are required in mm and are converted to cm
		 * (because the Perception module is returning the wrong
		 * values)
		 * @param front
		 * @param frontside
		 * @param back
		 * @param backside
		 */
		public void setSensorValues(double front, double frontside, double back, double backside) {
			dDISTFRONT = front*dDISTSCALE;
			dDISTFRONTSIDE = frontside*dDISTSCALE;
			dDISTBACK = back*dDISTSCALE;
			dDISTBACKSIDE = backside*dDISTSCALE;
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
		 * @return rotated_bitmap
		 */
		private Bitmap rotateBitmap(Bitmap source, float angle) {
			matrix.reset();
			matrix.postRotate( (-angle) );
			return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
		}
		
		/**
		 * Converts polar coordinates to Cartesian x value.
		 * The angle will be reserved to account for difference
		 * between coordinates systems from NXT Robot to
		 * Tablet.
		 * @param radius r
		 * @param angle phi
		 * @return x value
		 */
		private double convertToCartesianX(double r, double phi) {
			return ( r*(Math.cos( Math.toRadians(phi) )) );
		}
		
		/**
		 * Converts polar coordinates to Cartesian y value.
		 * The angle will be reserved to account for difference
		 * between coordinates systems from NXT Robot to
		 * Tablet.
		 * @param radius r
		 * @param angle phi
		 * @return y value
		 */
		private double convertToCartesianY(double r, double phi) {
			return ( r*(Math.sin( Math.toRadians(phi) )) );
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
        
        public boolean onGenericMotion(View view, MotionEvent event) {
        	Log.d("MotionCallback","Callback recieved.");
        	return true;
        }
        
        public MotionEvent getLastEvent() {
        	return histEvent;
        }
    }

	
	//------------------------------------
	// DECLARATION OF CLASS
	
	private MapThread thread;
	private SurfaceHolder mapholder;
	//private Context context;
	//private MapGestureListener GestureListener;
	private GestureDetector mDetector;
	private List<ParkingSlot> atParkingSlot;
	//private List<ParkingSlot> histParkingSlot;
	
	final private int aPS_LIST_CAPACITY = 10;
	
	
	public MapView(Context context, AttributeSet attrs) {
		//initialize with given context
		super(context, attrs);
		
		//register interest to hear about surface changes
		mapholder=getHolder();
		mapholder.addCallback(this);
		
		//create a thread, this will be started on surface creation
		thread = new MapThread(mapholder, context, new Handler());
		
		//create a gesture listener
		mDetector = new GestureDetector(context, new MapGestureListener(), new Handler());	
		
		//create temporary ArrayList
		atParkingSlot = Collections.synchronizedList(new ArrayList<ParkingSlot>());
		//histParkingSlot = Collections.synchronizedList(new ArrayList<ParkingSlot>());
		for (int i=0; i<aPS_LIST_CAPACITY; i++) {
			atParkingSlot.add(new ParkingSlot(i, null, null, null));
		}
	}
	
	
	/**
	 * Callback invoked when the surface dimensions change.
	 */
	public void surfaceChanged(SurfaceHolder holder, int format,int width,int height) {
		/*
		 * the following should only be reimplemented, if surface-changes should be
		*  listened to again
		*/
		
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
	
	/**
	 * Callback for TouchEvents.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}
	
	/**
	 * Tells the thread if the virtual pointer should be drawn instead of
	 * the real one.
	 * @param b
	 */
	public void setVPointer(boolean b) {
		thread.activateVPos(b);
	}
	
	/**
	 * Sets the coordinates where the virtual pointer will be drawn.
	 * These coordinates are generally affected by the general offset and
	 * the scaling routine.
	 * @param fx
	 * @param fy
	 */
	public void setVPos(float fx, float fy) {
		thread.setVPos(fx, fy);	
	}
	
	/**
	 * Returns true if the virtual pointer is currently
	 * being drawn.
	 * @return
	 */
	public boolean vPointerIsActive() {
		return thread.vPointerIsActive();
	}
	
	/**
	 * Determines if the background should be drawn.
	 * @param b
	 */
	public void setMapVisibility(boolean b) {
		thread.map_visible=b;
	}
	
	/**
	 * Sets the pose information for the pointer and pushes it
	 * to the thread.
	 * @param posx
	 * @param posy
	 * @param angle
	 */
	public void setPose(float posx, float posy, float angle) {
		thread.setDPos(posx, posy);
		thread.setDAngle(angle);
	}
	
	/**
	 * Adds a ParkingSlot to the list of elements
	 * that should be drawn.
	 * @param ps
	 */
	public synchronized void addParkingSlot(ParkingSlot ps) {
		if (!atParkingSlot.contains(ps)) {
			atParkingSlot.add(ps);
			thread.addParkingSlot(ps);
		}
	}
	
	/**
	 * Sets a ParkingSlot at a certain index.
	 * The ParkingSlot is masked with a copy of all available
	 * ParkingSlots and, if successful, is pushed to the thread.
	 * @param ps
	 * @param index
	 */
	public synchronized void setParkingSlot(ParkingSlot ps, int index) {
		if (!atParkingSlot.contains(ps)) {
			atParkingSlot.set(index, ps);
			thread.setParkingSlot(ps, index);
			Log.d("MapView","Pushing PS ["+ps.getID()+"] xb:"
			+ps.getBackBoundaryPosition().x+" yb:"
			+ps.getBackBoundaryPosition().y+" xf:"
			+ps.getFrontBoundaryPosition().x+" yf:"
			+ps.getFrontBoundaryPosition().y+" State:"+
			ps.getParkingSlotStatus());
		}
	}
	
	/**
	 * Pushes all Parking slots to the draw thread
	 * and clears the list of Parking slots. Only does so,
	 * if the list as changed.
	 * Since ParkingSlots are now set directly, this
	 * method has no effect anymore
	 * @deprecated
	 * 
	 */
	public synchronized void propagateParkingSlots() {
		/*
		 * This can be re-implemented for
		 * various purposes.
		 */
		
		//if (!(atParkingSlot.hashCode() == histParkingSlot.hashCode()) && !(atParkingSlot.isEmpty()))
		//thread.setParkingSlots(atParkingSlot);
		//histParkingSlot=atParkingSlot;
		//atParkingSlot.clear();	
	}
	
	/**
	 * Returns the currently selected ParkingSlots' index number.
	 * @return ParkingSlotID
	 */
	public int getParkingSlotSelectionID() {
		return thread.getParkingSlotSelectionID();
	}
	
	/**
	 * Passes the values of the Sensor for the doDraw()-Method to
	 * the thread.
	 * @param front
	 * @param frontside
	 * @param back
	 * @param backside
	 */
	public void setSensorValues(double front, double frontside, double back, double backside) {
		thread.setSensorValues(front, frontside, back, backside);
	}
	
}
