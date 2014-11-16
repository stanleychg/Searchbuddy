package com.buddy.search;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;


public class MyService extends Service {
	
	//UI
	private WindowManager windowManager; //Window of phone
	private WindowManager.LayoutParams viewParams; //Layout Parameters for viewOverlay
	private WindowManager.LayoutParams windowParams;
	private ImageView viewOverlay;
	private ImageView lineBorder;
	private int phoneWidth;
	private int phoneHeight;
	
	//Set margins for white line
	int lineWidth = 15;
	int widthMargin = 10; 
	int heightMargin = 275;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
//	public int onStartCommand(Intent intent, int flags, int startId){
//		Toast.makeText(this, "Service Started!", Toast.LENGTH_SHORT);
//		return START_STICKY;
//	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		Toast.makeText(this, "Service Destroyed!", Toast.LENGTH_SHORT);
		if(viewOverlay != null){
			//Destroy view
			windowManager.removeView(viewOverlay);
		}
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
		
		//Grab window of phone
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		
		//Grab phone dimensions
		Display display = windowManager.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		phoneWidth = size.x;
		phoneHeight = size.y;
		
		//Prepare line border
		
		//Set paint properties
		Paint paint = new Paint();
		paint.setColor(Color.argb(200, 255, 255, 255));
		paint.setStrokeWidth(lineWidth);
		
		//Generate line bitmap
		Bitmap bm = Bitmap.createBitmap(phoneWidth, phoneHeight ,Bitmap.Config.ARGB_4444);
		bm.eraseColor(Color.argb(0, 255, 255, 255));
		Canvas canvas = new Canvas(bm);
		canvas.drawLine(widthMargin, phoneHeight - heightMargin, 
				phoneWidth - widthMargin, phoneHeight - heightMargin, paint);
		
		//Set bitmap of white line
		lineBorder = new ImageView(MyService.this);
		lineBorder.setImageBitmap(bm);
		
		//Set LayoutParams of white line
		windowParams = new WindowManager.LayoutParams(
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.TYPE_PHONE,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		
		//Prepare viewOverlay and Set LayoutParams of white line
		viewOverlay = new ImageView(this);
		viewOverlay.setImageResource(R.drawable.ic_launcher);
		viewParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		
		viewParams.gravity = Gravity.TOP | Gravity.LEFT;
		viewParams.x = 0;
		viewParams.y = 100;
		windowManager.addView(viewOverlay, viewParams);
		
		//Set touch listener
		viewOverlay.setOnTouchListener(new OnTouchListener(){
			//Temp variables to store init position of view
			private int initX;
			private int initY;
			private float initTouchX;
			private float initTouchY;
			
			public boolean onTouch(View v, MotionEvent event){
				//Check type of movement
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					//Movement initiated
					initX = viewParams.x;
					initY = viewParams.y;
					initTouchX = event.getRawX();
					initTouchY = event.getRawY();
					
					//Add line border to window
					windowManager.addView(lineBorder, windowParams);
					
					break;
				case MotionEvent.ACTION_MOVE:
					//Move viewOverlay
					viewParams.x = initX + (int)(event.getRawX() - initTouchX);
					viewParams.y = initY + (int)(event.getRawY() - initTouchY);
					windowManager.updateViewLayout(viewOverlay, viewParams);
					break;
				case MotionEvent.ACTION_UP:
					windowManager.removeView(lineBorder);
					
					//Check position of viewOverlay
					if(viewParams.y < phoneHeight - heightMargin - 150){
						//Run search
						String s = getClosestWord(viewParams.x,viewParams.y);
						searchWord(s);
					} else{
						//Destroy service
						MyService.this.stopSelf();
					}
					break;
				default:
					break;
				}
				return false;
			}
		});
	}
	
	//Get word closest to position of viewOverlay
	String getClosestWord(int x, int y){
		String s = null;
		return s;
	}
	
	//Redirect to bing search for word
	void searchWord(String aWord){
		
	}
	


}
