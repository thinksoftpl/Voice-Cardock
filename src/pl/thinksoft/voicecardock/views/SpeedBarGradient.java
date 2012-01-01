package pl.thinksoft.voicecardock.views;

import pl.thinksoft.voicecardock.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Region;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SpeedBarGradient extends SurfaceView implements Runnable{
	Thread thread = null;
	SurfaceHolder holder;
	boolean isOK  = false;
	private int mCanvasWidth;
	private int mCanvasHeight;
	private Paint mPaint = new Paint();
	private int xTo = 0;
	private Context context;
	private int maxValue;
	private Bitmap myBitmapFront;
	private Bitmap myBitmapBack;
	private int currentSpeed;
	private Typeface font;
	private boolean isGPS;
	boolean firstDraw = false;
	
	private int lastSpeed = -1;
	private int maxTripSpeed = 0;
	private int width;
	private String speedUnit;
	private int xToMax;
	private SharedPreferences pref;

	public SpeedBarGradient(Context context) {
		super(context);
		this.context = context;
		myBitmapFront = BitmapFactory.decodeResource(context.getResources(),R.drawable.gradient_face);
		myBitmapBack = BitmapFactory.decodeResource(context.getResources(),R.drawable.gradient_back);
		width = myBitmapFront.getWidth();	//pobieram maksymalną długość
		holder = getHolder();
		
	}
	
	public SpeedBarGradient(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		myBitmapFront = BitmapFactory.decodeResource(context.getResources(),R.drawable.gradient_face);
		myBitmapBack = BitmapFactory.decodeResource(context.getResources(),R.drawable.gradient_back);
		width = myBitmapFront.getWidth();	//pobieram maksymalną długość
		holder = getHolder();
	}

	public void setGPSStatus(boolean status){
		isGPS = status;
		Log.d("VCDA", "GPS state::"+status);
		firstDraw = false;
	}
	
	public void setSpeedUnit(String speedUnit){
		this.speedUnit = speedUnit;
	}
	
	public void setValue(int value, boolean force){
//		value = 116;		
		currentSpeed = value;
		
		float m = width * value;
		float m2 = maxValue;
		xTo = Math.round(m/m2); 
		if (force)
			firstDraw = false;
	}
	
	public void setMaxTripSpeed(int value){
		maxTripSpeed = value;
		float m = width * value;
		float m2 = maxValue;
		xToMax = Math.round(m/m2);
	}

	public int getValue(){
		return currentSpeed;
	}
	
	public void setInitValues(int maxValue,Typeface font, boolean isGPS, String speedUnit, SharedPreferences pref){
		this.maxValue = maxValue;
		this.font = font;
		this.isGPS = isGPS;
		firstDraw = false;
		this.speedUnit = speedUnit;
		this.pref = pref;
	}
	
	@Override
	public void run() {
		while(isOK){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (!holder.getSurface().isValid())
				continue;
			
			//sprawdzamy, czy warto rysować
			if (lastSpeed == currentSpeed && firstDraw)
				continue;
			synchronized (holder){
				Log.d("VCDA","Drawing;xTo::"+xTo+" lastSpeed::"+lastSpeed+" current::"+currentSpeed);
				Canvas canvas = holder.lockCanvas();

				mCanvasWidth = myBitmapFront.getWidth();
				mCanvasHeight = myBitmapFront.getHeight();
				
				if (isGPS){
					
					//prędkość
					canvas.clipRect(0, 0, xTo , mCanvasHeight, Region.Op.REPLACE);
					canvas.drawBitmap(myBitmapFront, 0, 0, mPaint);
					//czarne
					canvas.clipRect(xTo, 0, mCanvasWidth , mCanvasHeight, Region.Op.REPLACE);
					canvas.drawBitmap(myBitmapBack, 0, 0, mPaint);
					
					//maxtripspeed - 2px kreska
					if (maxTripSpeed > 2 && pref.getBoolean("maxSpeedOnBar", true)){
						canvas.clipRect(xToMax-2, 0, xToMax , mCanvasHeight-3, Region.Op.REPLACE);
						canvas.drawBitmap(myBitmapFront, 0, 0, mPaint);
					}
				}
				canvas.clipRect(0, 0, mCanvasWidth , mCanvasHeight, Region.Op.REPLACE);
				mPaint.setStyle(Paint.Style.FILL);
				mPaint.setAntiAlias(true);
                mPaint.setColor(Color.WHITE);
 
                if (isGPS){
                	mPaint.setTypeface(font);
                    mPaint.setTextSize(40);
                    canvas.drawText(currentSpeed+" "+speedUnit, 10, 40, mPaint);
                }else{
                	mPaint.setTypeface(Typeface.DEFAULT);
                    mPaint.setTextSize(20);
                	canvas.drawText(context.getString(R.string.enable_GPS), 40, 40, mPaint);
                }
				holder.unlockCanvasAndPost(canvas);
				lastSpeed  = currentSpeed;
				firstDraw = true;
			}
		}
	}

	public void pause(){
		isOK = false;
		while (true){
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		}
		thread = null;
	}
	
	public void resume(){
		isOK = true;
		firstDraw = false;
		thread = new Thread(this);
		thread.start();
	}

	public void setMaxSpeedValue(int prefMaxSpeed) {
		this.maxValue = prefMaxSpeed;
	}
}
