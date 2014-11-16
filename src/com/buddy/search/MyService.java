package com.buddy.search;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;


public class MyService extends Service {
	
	private WindowManager windowManager;
	private WindowManager.LayoutParams params;
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
		params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		
		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = 0;
		params.y = 100;
		windowManager.addView(viewOverlay, params);
		
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
					initX = params.x;
					initY = params.y;
					initTouchX = event.getRawX();
					initTouchY = event.getRawY();
					break;
				case MotionEvent.ACTION_MOVE:
					params.x = initX + (int)(event.getRawX() - initTouchX);
					params.y = initY + (int)(event.getRawY() - initTouchY);
					windowManager.updateViewLayout(viewOverlay, params);
					break;
				}
				return false;
			}
		});
	}
	


}
