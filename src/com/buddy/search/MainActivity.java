package com.buddy.search;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Launch service
		Intent i = new Intent(this, MyService.class);
		startService(i);
		finish();
	}

}
