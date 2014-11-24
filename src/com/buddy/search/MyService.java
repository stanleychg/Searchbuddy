package com.buddy.search;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.buddy.tesseract.LanguageCodeHelper;
import com.buddy.tesseract.OcrCharacterHelper;
import com.buddy.tesseract.OcrInitAsyncTask;
import com.buddy.tesseract.OcrResult;
import com.buddy.tesseract.PreferencesActivity;
import com.buddy.tesseract.ViewfinderView;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;





public class MyService extends Service {
	
	//UI
	private WindowManager windowManager; //Window of phone
	private WindowManager.LayoutParams viewParams; //Layout Parameters for viewOverlay
	private WindowManager.LayoutParams windowParams;
	private WindowManager.LayoutParams debugParams;
	private ImageView viewOverlay;
	private ImageView lineBorder;
	private ViewfinderView viewfinderview;
	private int phoneWidth;
	private int phoneHeight;
	
	//Set margins for white line
	int lineWidth = 15;
	int widthMargin = 10; 
	int heightMargin = 275;
	
	
	
	
	
	 /** ISO 639-3 language code indicating the default recognition language. */
	  public static final String DEFAULT_SOURCE_LANGUAGE_CODE = "eng";
	  
	  /** ISO 639-1 language code indicating the default target language for translation. */
	  public static final String DEFAULT_TARGET_LANGUAGE_CODE = "es";
	  
	  /** The default online machine translation service to use. */
	  public static final String DEFAULT_TRANSLATOR = "Google Translate";
	  
	  /** The default OCR engine to use. */
	  public static final String DEFAULT_OCR_ENGINE_MODE = "Tesseract";
	  
	  /** The default page segmentation mode to use. */
	  public static final String DEFAULT_PAGE_SEGMENTATION_MODE = "Auto";
	  
	  /** Whether to use autofocus by default. */
	  public static final boolean DEFAULT_TOGGLE_AUTO_FOCUS = true;
	  
	  /** Whether to initially disable continuous-picture and continuous-video focus modes. */
	  public static final boolean DEFAULT_DISABLE_CONTINUOUS_FOCUS = true;
	  
	  /** Whether to beep by default when the shutter button is pressed. */
	  public static final boolean DEFAULT_TOGGLE_BEEP = false;
	  
	  /** Whether to initially show a looping, real-time OCR display. */
	  public static final boolean DEFAULT_TOGGLE_CONTINUOUS = false;
	  
	  /** Whether to initially reverse the image returned by the camera. */
	  public static final boolean DEFAULT_TOGGLE_REVERSED_IMAGE = false;
	  
	  /** Whether to enable the use of online translation services be default. */
	  public static final boolean DEFAULT_TOGGLE_TRANSLATION = true;
	  
	  /** Whether the light should be initially activated by default. */
	  public static final boolean DEFAULT_TOGGLE_LIGHT = false;

	  
	  /** Flag to display the real-time recognition results at the top of the scanning screen. */
	  private static final boolean CONTINUOUS_DISPLAY_RECOGNIZED_TEXT = true;
	  
	  /** Flag to display recognition-related statistics on the scanning screen. */
	  private static final boolean CONTINUOUS_DISPLAY_METADATA = true;
	  
	  /** Flag to enable display of the on-screen shutter button. */
	  private static final boolean DISPLAY_SHUTTER_BUTTON = true;
	  
	  /** Languages for which Cube data is available. */
	  public static final String[] CUBE_SUPPORTED_LANGUAGES = { 
	    "ara", // Arabic
	    "eng", // English
	    "hin" // Hindi
	  };

	  /** Languages that require Cube, and cannot run using Tesseract. */
	  private static final String[] CUBE_REQUIRED_LANGUAGES = { 
	    "ara" // Arabic
	  };
	  
	  /** Resource to use for data file downloads. */
	  public static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";
	  
	  /** Download filename for orientation and script detection (OSD) data. */
	  public static final String OSD_FILENAME = "tesseract-ocr-3.01.osd.tar";
	  
	  /** Destination filename for orientation and script detection (OSD) data. */
	  public static final String OSD_FILENAME_BASE = "osd.traineddata";
	  
	  /** Minimum mean confidence score necessary to not reject single-shot OCR result. Currently unused. */
	  static final int MINIMUM_MEAN_CONFIDENCE = 0; // 0 means don't reject any scored results

	  //private CaptureActivityHandler handler;
	  private ViewfinderView viewfinderView;
	  private SurfaceView surfaceView;
	  private SurfaceHolder surfaceHolder;
	  private TextView statusViewBottom;
	  private TextView statusViewTop;
	  private TextView ocrResultView;
	  private TextView translationView;
	  private View cameraButtonView;
	  private View resultView;
	  private View progressView;
	  private OcrResult lastResult;
	  private Bitmap lastBitmap;
	  private boolean hasSurface;
	  private TessBaseAPI baseApi; // Java interface for the Tesseract OCR engine
	  private String sourceLanguageCodeOcr; // ISO 639-3 language code
	  private String sourceLanguageReadable; // Language name, for example, "English"
	  private String sourceLanguageCodeTranslation; // ISO 639-1 language code
	  private String targetLanguageCodeTranslation; // ISO 639-1 language code
	  private String targetLanguageReadable; // Language name, for example, "English"
	  private int pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD;
	  private int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
	  private String characterBlacklist;
	  private String characterWhitelist;
	  private boolean isTranslationActive; // Whether we want to show translations
	  private boolean isContinuousModeActive; // Whether we are doing OCR in continuous mode
	  private SharedPreferences prefs;
	  private OnSharedPreferenceChangeListener listener;
	  private ProgressDialog dialog; // for initOcr - language download & unzip
	  private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine
	  private boolean isEngineReady;
	  private boolean isPaused;
	  private static boolean isFirstLaunch; // True if this is the first time the app is being run


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
	    
	    String previousSourceLanguageCodeOcr = sourceLanguageCodeOcr;
	    int previousOcrEngineMode = ocrEngineMode;
 
	    retrievePreferences();
	    
	    // Do OCR engine initialization, if necessary
	    boolean doNewInit = (baseApi == null) || !sourceLanguageCodeOcr.equals(previousSourceLanguageCodeOcr) || 
	        ocrEngineMode != previousOcrEngineMode;
	    if (doNewInit) {      
	      // Initialize the OCR engine
	      File storageDirectory = MyService.this.getStorageDirectory();
	      if (storageDirectory != null) {
	        initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
	      }
	    } else {
	      // We already have the engine initialized, so just start the camera.
	      resumeOCR();
	    }
	    

	    
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
		
		//Set LayoutParams of debugview
		debugParams = new WindowManager.LayoutParams(	
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		
		viewParams.gravity = Gravity.TOP | Gravity.LEFT;
		viewParams.x = 0;
		viewParams.y = 100;
		windowManager.addView(viewOverlay, viewParams);
		
		
		//Initialize OCR window
		viewfinderview = new ViewfinderView(this,null);
		
		
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
						String textResult;
						
						Bitmap bitmap = Bitmap.createBitmap(phoneWidth, phoneHeight ,Bitmap.Config.ALPHA_8);
//						bitmap.s
//						Canvas canvas = new Canvas(bitmap);
						viewOverlay.getRootView().setDrawingCacheEnabled(true);
						bitmap = Bitmap.createBitmap(viewOverlay.getRootView().getDrawingCache());
						viewOverlay.getRootView().setDrawingCacheEnabled(false);
//						
//						ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
//						bitmap.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos); 
//						byte[] bitmapdata = bos.toByteArray();
//						ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
//						
//						BitmapRegionDecoder brd;
//						try {
//							brd = BitmapRegionDecoder.newInstance(bs, true);
//							bitmap = brd.decodeRegion(new Rect(viewParams.x - viewOverlay.getWidth()/2, viewParams.y - viewOverlay.getHeight()/2,
//									viewParams.x + viewOverlay.getWidth()/2, viewParams.y + viewOverlay.getHeight()/2), null);
//							System.out.println("HI");
//						} catch (IOException e2) {
//							// TODO Auto-generated catch block
//							e2.printStackTrace();
//						}
				
					
						//Bitmap bitmap = Bitmap.createBitmap(viewOverlay.getWidth(), viewOverlay.getHeight() ,Bitmap.Config.ARGB_4444);
//						bitmap.eraseColor(Color.argb(255, 255, 255, 255));
						//Canvas canvas = new Canvas(bitmap);
						//viewOverlay.draw(canvas);
						ImageView debugView = new ImageView(MyService.this);
						debugView.setImageBitmap(bitmap);
						debugParams.y += 50;
						windowManager.addView(debugView, debugParams);
						
					    try {     
					        baseApi.setImage(ReadFile.readBitmap(bitmap));
					        textResult = baseApi.getUTF8Text();

					        // Check for failure to recognize text
					        if (textResult == null || textResult.equals("")) {
					        	Toast.makeText(MyService.this, "Cannot Read", Toast.LENGTH_SHORT).show();
					          return false;
					        } else{
					        	//Word recognized
					        	Toast.makeText(MyService.this, textResult, Toast.LENGTH_LONG).show();
					        }
//					        ocrResult = new OcrResult();
//					        ocrResult.setWordConfidences(baseApi.wordConfidences());
//					        ocrResult.setMeanConfidence( baseApi.meanConfidence());
//					        ocrResult.setRegionBoundingBoxes(baseApi.getRegions().getBoxRects());
//					        ocrResult.setTextlineBoundingBoxes(baseApi.getTextlines().getBoxRects());
//					        ocrResult.setWordBoundingBoxes(baseApi.getWords().getBoxRects());
//					        ocrResult.setStripBoundingBoxes(baseApi.getStrips().getBoxRects());
					        //ocrResult.setCharacterBoundingBoxes(baseApi.getCharacters().getBoxRects());
					      } catch (RuntimeException e) {
					        Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.");
					        e.printStackTrace();
					        try {
					          baseApi.clear();
					          //activity.stopHandler();
					        } catch (NullPointerException e1) {
					          // Continue
					        }
					        return false;
					      }
					    
						//windowManager.removeView(debugView);
						
						//String s = getClosestWord(viewParams.x,viewParams.y);
						//searchWord(s);
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
	
	  /**
	   * Gets values from shared preferences and sets the corresponding data members in this activity.
	   */
	  private void retrievePreferences() {
	      prefs = PreferenceManager.getDefaultSharedPreferences(this);
	      
	      // Retrieve from preferences, and set in this Activity, the language preferences
	      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	      setSourceLanguage(prefs.getString(PreferencesActivity.KEY_SOURCE_LANGUAGE_PREFERENCE, MyService.DEFAULT_SOURCE_LANGUAGE_CODE));
	      //setTargetLanguage(prefs.getString(PreferencesActivity.KEY_TARGET_LANGUAGE_PREFERENCE, MyService.DEFAULT_TARGET_LANGUAGE_CODE));
	      //isTranslationActive = prefs.getBoolean(PreferencesActivity.KEY_TOGGLE_TRANSLATION, false);
	      isTranslationActive = false;
	      
	      // Retrieve from preferences, and set in this Activity, the capture mode preference
	      if (prefs.getBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, MyService.DEFAULT_TOGGLE_CONTINUOUS)) {
	        isContinuousModeActive = true;
	      } else {
	        isContinuousModeActive = false;
	      }

	      // Retrieve from preferences, and set in this Activity, the page segmentation mode preference
	      String[] pageSegmentationModes = getResources().getStringArray(R.array.pagesegmentationmodes);
	      String pageSegmentationModeName = prefs.getString(PreferencesActivity.KEY_PAGE_SEGMENTATION_MODE, pageSegmentationModes[0]);
	      if (pageSegmentationModeName.equals(pageSegmentationModes[0])) {
	        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD;
	      } else if (pageSegmentationModeName.equals(pageSegmentationModes[1])) {
	        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO;
	      } else if (pageSegmentationModeName.equals(pageSegmentationModes[2])) {
	        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK;
	      } else if (pageSegmentationModeName.equals(pageSegmentationModes[3])) {
	        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR;
	      } else if (pageSegmentationModeName.equals(pageSegmentationModes[4])) {
	        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN;
	      } else if (pageSegmentationModeName.equals(pageSegmentationModes[5])) {
	        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE;
	      } else if (pageSegmentationModeName.equals(pageSegmentationModes[6])) {
	        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_WORD;
	      } else if (pageSegmentationModeName.equals(pageSegmentationModes[7])) {
	        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT;
	      } else if (pageSegmentationModeName.equals(pageSegmentationModes[8])) {
	        pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT;
	      }
	      
	      // Retrieve from preferences, and set in this Activity, the OCR engine mode
	      String[] ocrEngineModes = getResources().getStringArray(R.array.ocrenginemodes);
	      String ocrEngineModeName = prefs.getString(PreferencesActivity.KEY_OCR_ENGINE_MODE, ocrEngineModes[0]);
	      if (ocrEngineModeName.equals(ocrEngineModes[0])) {
	        ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
	      } else if (ocrEngineModeName.equals(ocrEngineModes[1])) {
	        ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
	      } else if (ocrEngineModeName.equals(ocrEngineModes[2])) {
	        ocrEngineMode = TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED;
	      }
	      
	      // Retrieve from preferences, and set in this Activity, the character blacklist and whitelist
	      characterBlacklist = OcrCharacterHelper.getBlacklist(prefs, sourceLanguageCodeOcr);
	      characterWhitelist = OcrCharacterHelper.getWhitelist(prefs, sourceLanguageCodeOcr);
	      
	      prefs.registerOnSharedPreferenceChangeListener(listener);
	      
	  }
	  
	  /**
	   * Sets default values for preferences. To be called the first time this app is run.
	   */
	  private void setDefaultPreferences() {
	    prefs = PreferenceManager.getDefaultSharedPreferences(this);

	    // Continuous preview
	    prefs.edit().putBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, MyService.DEFAULT_TOGGLE_CONTINUOUS).commit();

	    // Recognition language
	    prefs.edit().putString(PreferencesActivity.KEY_SOURCE_LANGUAGE_PREFERENCE, MyService.DEFAULT_SOURCE_LANGUAGE_CODE).commit();

	    // Translation
	    prefs.edit().putBoolean(PreferencesActivity.KEY_TOGGLE_TRANSLATION, MyService.DEFAULT_TOGGLE_TRANSLATION).commit();

	    // Translation target language
	    prefs.edit().putString(PreferencesActivity.KEY_TARGET_LANGUAGE_PREFERENCE, MyService.DEFAULT_TARGET_LANGUAGE_CODE).commit();

	    // Translator
	    prefs.edit().putString(PreferencesActivity.KEY_TRANSLATOR, MyService.DEFAULT_TRANSLATOR).commit();

	    // OCR Engine
	    prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, MyService.DEFAULT_OCR_ENGINE_MODE).commit();

	    // Autofocus
	    prefs.edit().putBoolean(PreferencesActivity.KEY_AUTO_FOCUS, MyService.DEFAULT_TOGGLE_AUTO_FOCUS).commit();
	    
	    // Disable problematic focus modes
	    prefs.edit().putBoolean(PreferencesActivity.KEY_DISABLE_CONTINUOUS_FOCUS, MyService.DEFAULT_DISABLE_CONTINUOUS_FOCUS).commit();
	    
	    // Beep
	    prefs.edit().putBoolean(PreferencesActivity.KEY_PLAY_BEEP, MyService.DEFAULT_TOGGLE_BEEP).commit();
	    
	    // Character blacklist
	    prefs.edit().putString(PreferencesActivity.KEY_CHARACTER_BLACKLIST, 
	        OcrCharacterHelper.getDefaultBlacklist(MyService.DEFAULT_SOURCE_LANGUAGE_CODE)).commit();

	    // Character whitelist
	    prefs.edit().putString(PreferencesActivity.KEY_CHARACTER_WHITELIST, 
	        OcrCharacterHelper.getDefaultWhitelist(MyService.DEFAULT_SOURCE_LANGUAGE_CODE)).commit();

	    // Page segmentation mode
	    prefs.edit().putString(PreferencesActivity.KEY_PAGE_SEGMENTATION_MODE, MyService.DEFAULT_PAGE_SEGMENTATION_MODE).commit();

	    // Reversed camera image
	    prefs.edit().putBoolean(PreferencesActivity.KEY_REVERSE_IMAGE, MyService.DEFAULT_TOGGLE_REVERSED_IMAGE).commit();

	    // Light
	    prefs.edit().putBoolean(PreferencesActivity.KEY_TOGGLE_LIGHT, MyService.DEFAULT_TOGGLE_LIGHT).commit();
	  }
	
	  /** Sets the necessary language code values for the given OCR language. */
	  private boolean setSourceLanguage(String languageCode) {
	    sourceLanguageCodeOcr = languageCode;
	    sourceLanguageCodeTranslation = LanguageCodeHelper.mapLanguageCode(languageCode);
	    sourceLanguageReadable = LanguageCodeHelper.getOcrLanguageName(this, languageCode);
	    return true;
	  }
	
	  /**
	   * Requests initialization of the OCR engine with the given parameters.
	   * 
	   * @param storageRoot Path to location of the tessdata directory to use
	   * @param languageCode Three-letter ISO 639-3 language code for OCR 
	   * @param languageName Name of the language for OCR, for example, "English"
	   */
	  private void initOcrEngine(File storageRoot, String languageCode, String languageName) {    
	    isEngineReady = false;
	    
	    // Set up the dialog box for the thermometer-style download progress indicator
//	    if (dialog != null) {
//	      dialog.dismiss();
//	    }
//	    dialog = new ProgressDialog(this);
	    
	    // If we have a language that only runs using Cube, then set the ocrEngineMode to Cube
	    if (ocrEngineMode != TessBaseAPI.OEM_CUBE_ONLY) {
	      for (String s : CUBE_REQUIRED_LANGUAGES) {
	        if (s.equals(languageCode)) {
	          ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
//	          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//	          prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
	        }
	      }
	    }

	    // If our language doesn't support Cube, then set the ocrEngineMode to Tesseract
	    if (ocrEngineMode != TessBaseAPI.OEM_TESSERACT_ONLY) {
	      boolean cubeOk = false;
	      for (String s : CUBE_SUPPORTED_LANGUAGES) {
	        if (s.equals(languageCode)) {
	          cubeOk = true;
	        }
	      }
	      if (!cubeOk) {
	        ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
//	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//	        prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
	      }
	    }
	    
	    // Display the name of the OCR engine we're initializing in the indeterminate progress dialog box
//	    indeterminateDialog = new ProgressDialog(this);
//	    indeterminateDialog.setTitle("Please wait");
//	    String ocrEngineModeName = getOcrEngineModeName();
//	    if (ocrEngineModeName.equals("Both")) {
//	      indeterminateDialog.setMessage("Initializing Cube and Tesseract OCR engines for " + languageName + "...");
//	    } else {
//	      indeterminateDialog.setMessage("Initializing " + ocrEngineModeName + " OCR engine for " + languageName + "...");
//	    }
//	    indeterminateDialog.setCancelable(false);
//	    indeterminateDialog.show();
	    
//	    if (handler != null) {
//	      handler.quitSynchronously();     
//	    }
//
//	    // Disable continuous mode if we're using Cube. This will prevent bad states for devices 
//	    // with low memory that crash when running OCR with Cube, and prevent unwanted delays.
//	    if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY || ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
//	      Log.d(TAG, "Disabling continuous preview");
//	      isContinuousModeActive = false;
//	      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//	      prefs.edit().putBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, false);
//	    }
	    
	    // Start AsyncTask to install language data and init OCR
	    baseApi = new TessBaseAPI();
	    new OcrInitAsyncTask(this, baseApi, dialog, indeterminateDialog, languageCode, languageName, ocrEngineMode)
	      .execute(storageRoot.toString());
	  }
	  
	  /** Finds the proper location on the SD card where we can save files. */
	  private File getStorageDirectory() {
	    //Log.d(TAG, "getStorageDirectory(): API level is " + Integer.valueOf(android.os.Build.VERSION.SDK_INT));
	    
	    String state = null;
	    try {
	      state = Environment.getExternalStorageState();
	    } catch (RuntimeException e) {
	      //Log.e(TAG, "Is the SD card visible?", e);
	      //showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
	    	System.out.println("Error A");
	    }
	    
	    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

	      // We can read and write the media
	      //    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
	      // For Android 2.2 and above
	      
	      try {
	        return getExternalFilesDir(Environment.MEDIA_MOUNTED);
	      } catch (NullPointerException e) {
	        // We get an error here if the SD card is visible, but full
//	        Log.e(TAG, "External storage is unavailable");
//	        showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
	        System.out.println("Error B");
	      }
	      
	      //        } else {
	      //          // For Android 2.1 and below, explicitly give the path as, for example,
	      //          // "/mnt/sdcard/Android/data/edu.sfsu.cs.orange.ocr/files/"
	      //          return new File(Environment.getExternalStorageDirectory().toString() + File.separator + 
	      //                  "Android" + File.separator + "data" + File.separator + getPackageName() + 
	      //                  File.separator + "files" + File.separator);
	      //        }
	    
	    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	    	// We can only read the media
//	    	Log.e(TAG, "External storage is read-only");
//	      showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
	    	System.out.println("Error C");
	    } else {
	    	// Something else is wrong. It may be one of many other states, but all we need
	      // to know is we can neither read nor write
//	    	Log.e(TAG, "External storage is unavailable");
//	    	showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable or corrupted.");
	    	System.out.println("Error D");
	    }
	    return null;
	  }
	  
	  void resumeOCR() {

		    // This method is called when Tesseract has already been successfully initialized, so set 
		    // isEngineReady = true here.
		    isEngineReady = true;
		    
		    isPaused = false;

//		    if (handler != null) {
//		      handler.resetState();
//		    }
		    if (baseApi != null) {
		      baseApi.setPageSegMode(pageSegmentationMode);
		      baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, characterBlacklist);
		      baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, characterWhitelist);
		    }

		  }
//	
//	  /** 
//	   * Method to start or restart recognition after the OCR engine has been initialized,
//	   * or after the app regains focus. Sets state related settings and OCR engine parameters,
//	   * and requests camera initialization.
//	   */
//	  void resumeOCR() {
//	    Log.d(TAG, "resumeOCR()");
//	    
//	    // This method is called when Tesseract has already been successfully initialized, so set 
//	    // isEngineReady = true here.
//	    isEngineReady = true;
//	    
//	    isPaused = false;
//
//	    if (handler != null) {
//	      handler.resetState();
//	    }
//	    if (baseApi != null) {
//	      baseApi.setPageSegMode(pageSegmentationMode);
//	      baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, characterBlacklist);
//	      baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, characterWhitelist);
//	    }
//
//	    if (hasSurface) {
//	      // The activity was paused but not stopped, so the surface still exists. Therefore
//	      // surfaceCreated() won't be called, so init the camera here.
//	      initCamera(surfaceHolder);
//	    }
//	  }
//	  
//	  /** Called when the shutter button is pressed in continuous mode. */
//	  void onShutterButtonPressContinuous() {
//	    isPaused = true;
//	    handler.stop();  
//	    beepManager.playBeepSoundAndVibrate();
//	    if (lastResult != null) {
//	      handleOcrDecode(lastResult);
//	    } else {
//	      Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
//	      toast.setGravity(Gravity.TOP, 0, 0);
//	      toast.show();
//	      resumeContinuousDecoding();
//	    }
//	  }
//
//	  /** Called to resume recognition after translation in continuous mode. */
//	  @SuppressWarnings("unused")
//	  void resumeContinuousDecoding() {
//	    isPaused = false;
//	    resetStatusView();
//	    setStatusViewForContinuous();
//	    DecodeHandler.resetDecodeState();
//	    handler.resetState();
//	    if (shutterButton != null && DISPLAY_SHUTTER_BUTTON) {
//	      shutterButton.setVisibility(View.VISIBLE);
//	    }
//	  }
//	
//	//Get word closest to position of viewOverlay
//	String getClosestWord(int x, int y){
//		String s = null;
//		return s;
//	}
//	
//	String getWord(){
//		OcrResult lastResult = ocrResult;
//	    
//	    // Test whether the result is null
//	    if (ocrResult.getText() == null || ocrResult.getText().equals("")) {
//	      Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
//	      toast.setGravity(Gravity.TOP, 0, 0);
//	      toast.show();
//	      return false;
//	    }
//	    
//	    ImageView bitmapImageView = (ImageView) findViewById(R.id.image_view);
//	    lastBitmap = ocrResult.getBitmap();
//	    if (lastBitmap == null) {
//	      bitmapImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
//	          R.drawable.ic_launcher));
//	    } else {
//	      bitmapImageView.setImageBitmap(lastBitmap);
//	    }
//
//	    // Display the recognized text
//	    TextView sourceLanguageTextView = (TextView) findViewById(R.id.source_language_text_view);
//	    sourceLanguageTextView.setText(sourceLanguageReadable);
//	    TextView ocrResultTextView = (TextView) findViewById(R.id.ocr_result_text_view);
//	    ocrResultTextView.setText(ocrResult.getText());
//	    // Crudely scale betweeen 22 and 32 -- bigger font for shorter text
//	    int scaledSize = Math.max(22, 32 - ocrResult.getText().length() / 4);
//	    ocrResultTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
//
//	}
//	
//	//Redirect to bing search for word
//	void searchWord(String aWord){
//		
//	}
	


}
