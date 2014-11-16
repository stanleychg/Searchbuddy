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
	
	private WindowManager windowManager; //Window of phone
	private WindowManager.LayoutParams viewParams; //Layout Parameters for viewOverlay
	private WindowManager.LayoutParams windowParams;
	private ImageView viewOverlay;

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
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
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
			
			private ImageView lineBorder;
			
			public boolean onTouch(View v, MotionEvent event){
				//Check type of movement
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					//Movement initiated
					initX = viewParams.x;
					initY = viewParams.y;
					initTouchX = event.getRawX();
					initTouchY = event.getRawY();
					
					/*
					//Draw boundaries of lower boundaries of viewOverlay
					*/
					//Grab phone width
					Display display = windowManager.getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					int phoneWidth = size.x;
					int phoneHeight = size.y;
					
					//Draw bottom border with thin white line with margins
					int lineWidth = 15;
					int widthMargin = 10; 
					int heightMargin = 275;
					
					Paint paint = new Paint();
					paint.setColor(Color.argb(200, 255, 255, 255));
					paint.setStrokeWidth(lineWidth);

					
					Bitmap bm = Bitmap.createBitmap(phoneWidth, phoneHeight ,Bitmap.Config.ARGB_4444);
					bm.eraseColor(Color.argb(0, 255, 255, 255));
					Canvas canvas = new Canvas(bm);
					canvas.drawLine(widthMargin, phoneHeight - heightMargin, phoneWidth - widthMargin, phoneHeight - heightMargin, paint);
					
					//Set bitmap of imageview
					lineBorder = new ImageView(MyService.this);
					lineBorder.setImageBitmap(bm);
					
					//Set LayoutParams of imageview
					windowParams = new WindowManager.LayoutParams(
						WindowManager.LayoutParams.MATCH_PARENT,
						WindowManager.LayoutParams.MATCH_PARENT,
						WindowManager.LayoutParams.TYPE_PHONE,
						WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
							PixelFormat.TRANSLUCENT);
					//windowParams.height = 500;
					
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
					break;
				default:
					break;
				}
				return false;
			}
		});
	}
	


}
