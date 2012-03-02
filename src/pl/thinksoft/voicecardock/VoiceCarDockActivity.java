package pl.thinksoft.voicecardock;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import pl.thinksoft.voicecardock.R;
import pl.thinksoft.voicecardock.VoiceCarDockActivity.CallReceiver;
import pl.thinksoft.voicecardock.views.SpeedBarGradient;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class VoiceCarDockActivity extends Activity implements OnInitListener {
		     
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1;
	private static final int INTENT_FOR_CONFIG = 500;
	private static final String VCDA = "VCDA";
	private static final double METERSTOMILES = 0.000621371192;
	private static final int COUNT_AVG_SPEED_ONLY_ON_MOVE = 1; 
	TextToSpeech tts;
	Button speakButton;  
//	Button regexed;
	public VoiceController vc;
	public Contact contacts = null;
	private LocationManager locationManager;
	private static Context context;
	private Location startLocation = null;
	protected Location currentLocation;
	private TextView tvInformations;
	private Geocoder geocoder;
	private TextView tvAdres;
	private long firstFixGPS = 0;
	private Location lastDistanseLocationGPS;
	protected boolean speedToZero;
	private Typeface font;
	private boolean isGPS;
	private PowerManager.WakeLock wl;
	
	private LocationListener locationListener = new LocationListener() {
        public void onStatusChanged(String provider, int status, Bundle extras) {
        	Log.d("VCDA","LL state::"+status);
        }
 
        public void onProviderEnabled(String provider) {
        	bar.setGPSStatus(true);
        }
 
        public void onProviderDisabled(String provider) {
        	bar.setGPSStatus(false);
        }
 
        public void onLocationChanged(Location location) {
//            speedToZero = true;
        	gpsAdditionalInfo(location);
            if (startLocation == null)
                startLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            currentLocation = location;
        }
    };
	private SpeedBarGradient bar;
	private int speedBarValue;
	private SharedPreferences pref;
	private int prefMaxSpeed;
	private int prefDistance;
	private long prefTime;
	private String prefSpeedUnit;
	private boolean prefWakelock;
	private long maxSpeed = 0;
	private TextView tvMaxSpeed;
	private TextView tvStartTime;
	private long subTimeMeter = 0;
	//dwie zmienne używane do obliczania średniej
	private long timeMeter;				//czas jazdy (speed > 00
	private double pointToPointDistance;	//dystans w odpowiedniej jednostce
	private int avgSpeed;
	//
	private TextView tvAvgSpeed;
	private Location lastLocation;
	private int realDistance = 0;
	private int prefAvgSpeed;
	private DbAdapter dbAdapter;
	public AudioManager audiomanager;
	private VCDAudioManager audioManager;
	private boolean prefSpeakerOn;
	private CallReceiver callReceiver;
	private IntentFilter intentPhoneState;
	public boolean activityIsActive;
	private Editor prefsEditor;
	private boolean prefMaxSpeedOnBar;
	private SharedPreferences saveState;
	private boolean wasPaused;
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.settingsMenu:
	    	Intent configIntent = new Intent(getApplicationContext(), SettingsActivity.class);
	    	startActivityForResult(configIntent, INTENT_FOR_CONFIG);
	        return true;
	    case R.id.reset:
	    	clearOnPause();
	    	resumeOnPause(true);
	    	return true;
	    case R.id.exit:
	    	Log.d("VCDA", "close dialog");
	    	closeDialog();
	    	return false;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

    @Override
    protected void onStart() {
        super.onStart();
        try{
        	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }catch (Exception e) {
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Error!");
			builder.setMessage("Can't find the GPS provider!.\nApplication will close.");
			builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
				
//				@Override
				public void onClick(DialogInterface dialog, int which) {
					VoiceCarDockActivity.this.finish();
				}
			});
			AlertDialog alertDialogOutput = builder.create();
			alertDialogOutput.show();
		}
        Log.d("VCDA","onStart()");
    }
     
	@Override
    protected void onStop() {
        locationManager.removeUpdates(locationListener);
        Log.d("VCDA","onStop()"+speedBarValue);
        setScreenLock(false);
        super.onStop();
    }
	
//	@Override
	public void onInit(int status) {
//		 if (status == TextToSpeech.SUCCESS){
//			 Toast.makeText(getApplicationContext(), "aplikacja start",Toast.LENGTH_SHORT).show();
//			 tts.speak("Hello world", TextToSpeech.QUEUE_FLUSH, null);
//		 }
		Log.d("VCDA","onInit()");
	}
	
	@Override
	public void onResume(){
		super.onResume();
		activityIsActive = true;
		resumeOnPause(false);
		setPrefActiveActivity(true);
		setScreenLock(true);
		checkGPS();
		bar.resume();
		tvMaxSpeed.setText("ala ma ota");
		Log.d("VCDA","onResume()");
	}
	
	@Override
	protected void onDestroy() {
		Log.d("VCDA", "onDestroy::");
		setScreenLock(false);
		locationManager.removeUpdates(locationListener);
		dbAdapter.close();
		try{
			unregisterReceiver(callReceiver);
		} catch (Exception e){
		}
		activityIsActive = false;
		super.onDestroy();
		Log.d("VCDA", "onDestroy::22");
	}

	@Override
	protected void onPause() {
		super.onPause();
		activityIsActive = false;
		setPrefActiveActivity(false);
		bar.pause();
		setScreenLock(false);
		saveOnPause();
		Log.d("VCDA","onPause()");
		//TODO: save current variable
	}

	private void saveOnPause(){
		SharedPreferences.Editor editor = saveState.edit();
		editor.putBoolean("savedState", true);
		editor.putInt("bar", bar.getValue());
		editor.putString("tvInformations", (String)tvInformations.getText());
		editor.putString("tvAdres", (String)tvAdres.getText());
		editor.putString("tvMaxSpeed", (String)tvMaxSpeed.getText());
		editor.putString("tvStartTime", (String)tvStartTime.getText());
		editor.putString("tvAvgSpeed", (String)tvAvgSpeed.getText());
		editor.putLong("maxSpeed", maxSpeed);
		editor.putLong("firstFixGPS", firstFixGPS);
		if (startLocation != null){
			editor.putBoolean("isStartLocation", true);
			editor.putString("startLocationLatitude", String.valueOf(startLocation.getLatitude()));
			editor.putString("startLocationLongitude", String.valueOf(startLocation.getLongitude()));
		}
		if (lastLocation != null){
			editor.putBoolean("isLastLocation", true);
			editor.putString("lastLocationLatitude", String.valueOf(lastLocation.getLatitude()));
			editor.putString("lastLocationLongitude", String.valueOf(lastLocation.getLongitude()));
		}
		editor.putString("pointToPointDistance", String.valueOf(pointToPointDistance));
		editor.putInt("realDistance", realDistance);
//		startLocation.getLatitude() startLocation.getLongitude()
		editor.commit();
	}
	
	private void resumeOnPause(boolean force){
		if (saveState.getBoolean("savedState", false) || force){
			bar.setValue(saveState.getInt("bar", 0), true);
			tvInformations.setText(saveState.getString("tvInformations", ""));
			tvAdres.setText(saveState.getString("tvAdres", ""));
			tvMaxSpeed.setText(saveState.getString("tvMaxSpeed", ""));
			tvStartTime.setText(saveState.getString("tvStartTime", ""));
			tvAvgSpeed.setText(saveState.getString("tvAvgSpeed", ""));
			maxSpeed = saveState.getLong("maxSpeed", 0);
			bar.setMaxTripSpeed(Integer.valueOf(maxSpeed+""));
			firstFixGPS = saveState.getLong("firstFixGPS", 0);
			if (saveState.getBoolean("isStartLocation", false)){
				startLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (saveState.getString("startLocationLatitude", "0") != null){
					startLocation.setLatitude(Double.parseDouble(saveState.getString("startLocationLatitude", "0")));
					if (saveState.getString("startLocationLongitude", "0") != null)
						startLocation.setLongitude(Double.parseDouble(saveState.getString("startLocationLongitude", "0")));
				}
			}
			if (saveState.getBoolean("isLastLocation", false)){
				lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (saveState.getString("lastLocationLatitude", "0") != null){
					lastLocation.setLatitude(Double.parseDouble(saveState.getString("lastLocationLatitude", "0")));
					if (saveState.getString("lastLocationLongitude", "0") != null)
						lastLocation.setLongitude(Double.parseDouble(saveState.getString("lastLocationLongitude", "0")));
				}
			}	
			pointToPointDistance = Double.parseDouble(saveState.getString("pointToPointDistance", "0"));
			realDistance = saveState.getInt("realDistance", 0);
		}
	}
	
	 
	private void clearOnPause(){
		SharedPreferences.Editor editor = saveState.edit();
		editor.putBoolean("savedState", false);
		editor.putInt("bar", 0);
		editor.putString("tvInformations", "");
		editor.putString("tvAdres", "");
		editor.putString("tvMaxSpeed", "");
		editor.putString("tvStartTime", "");
		editor.putString("tvAvgSpeed", "");
		editor.putLong("maxSpeed", 0);
		editor.putLong("firstFixGPS", 0);
		editor.putString("lastLocationLatitude","");
		editor.putString("lastLocationLongitude","");
		editor.putString("startLocationLatitude","");
		editor.putString("startLocationLongitude","");
		editor.commit();
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		saveState = getSharedPreferences("VCDA_state", Activity.MODE_PRIVATE);
		setWindowFeature();
		this.context = this;
		setContentView(R.layout.main);
		
		initPreferences();
		
		checkNetworkAvailable();
		
		initVariables();

		
		checkGPS();

		//inicjujemy speedbargradient
		bar.setInitValues(prefMaxSpeed, font, isGPS, prefSpeedUnit, pref);

		setScreenLock(true);
		returnValuesAfterRotate();
		
		if (vc == null)
			vc = new VoiceController(getApplicationContext(), contacts, this);

		checkVoiceRecognition();
		
		dbAdapter = new DbAdapter(getApplicationContext());
		dbAdapter.open();
		
		intentPhoneState = new IntentFilter("android.intent.action.PHONE_STATE");
		callReceiver = new CallReceiver();
		registerReceiver(callReceiver, intentPhoneState);

	}

	private void setPrefActiveActivity(boolean state){
		prefsEditor.putBoolean("activeActivity", state);
		prefsEditor.commit();
	}

	
	private void initVariables() {
		font = Typeface.createFromAsset(getAssets(), "fonts/HEMIHEAD.TTF");
		bar = (SpeedBarGradient) findViewById(R.id.speed);
		tvMaxSpeed = (TextView) findViewById(R.id.tvMaxSpeed);
		tvStartTime = (TextView) findViewById(R.id.tvStartTime);
		speakButton = (Button) findViewById(R.id.btnspeak);
//		regexed = (Button) findViewById(R.id.btnreg);
		tvInformations = (TextView) findViewById(R.id.tvInformations);
		tvAdres = (TextView) findViewById(R.id.tvAdres);
		tvAvgSpeed = (TextView) findViewById(R.id.tvAvgSpeed);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        tts = new TextToSpeech(getApplicationContext(), this);
        geocoder = new Geocoder(this, Locale.getDefault());
        audioManager = new VCDAudioManager(getApplicationContext(), prefSpeakerOn);	
        
        //preferencje zapobiegające odpaleniu głośnika, gdy activity jest wylaczone
        //bo nie działa unregisterReceiver
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefsEditor = prefs.edit();

	}

	private void checkVoiceRecognition() {
		OnClickListener listener = new OnClickListener() {

//			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.btnspeak) {
					startVoiceRecognitionActivity();
				}
			}
		};
		// Check to see if a recognition activity is present
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
				RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);

		
		//jeżeli jest zainstalowane rozszerzenie rozpoznawania mowy
		if (activities.size() != 0) {
			speakButton.setEnabled(false);
			speakButton.setText(this.getString(R.string.wait));
			GetAsyncAllContacts taskContakt = new GetAsyncAllContacts(this);
			taskContakt.execute();
			speakButton.setOnClickListener(listener);
		} else {
			speakButton.setEnabled(false);
			speakButton.setText(this.getString(R.string.no_voice_recogn));
		} 
	}

	private void returnValuesAfterRotate() {
		//w momencie obrotu telefonu, po odrysowaniu, przywracamy stary stan
		@SuppressWarnings("unchecked")
		final HashMap<String, Object> previousRotateValues = (HashMap<String, Object>)getLastNonConfigurationInstance();
		if (previousRotateValues != null) {
			bar.setValue((Integer)previousRotateValues.get("speedBar")   , true);
			tvInformations.setText((String)previousRotateValues.get("tvInformations"));
			tvAdres.setText((String)previousRotateValues.get("tvAdres"));
			tvMaxSpeed.setText((String)previousRotateValues.get("tvMaxSpeed"));
			tvStartTime.setText((String)previousRotateValues.get("tvStartTime"));
			tvAvgSpeed.setText((String)previousRotateValues.get("tvAvgSpeed"));
		} 
	}

	private void checkNetworkAvailable() {
		//sprawdzam dostęp do sieci - brak koniec aplikacji
		if (!isNetworkAvailable()){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Błąd!");
			builder.setMessage("Brak dostępu do sieci.\nAplikacja kończy działanie.");
			builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
				
//				@Override
				public void onClick(DialogInterface dialog, int which) {
					VoiceCarDockActivity.this.finish();
				}
			});
			AlertDialog alertDialogOutput = builder.create();
			alertDialogOutput.show();
		}
	}

	private void setWindowFeature() {
		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		/*
		 * //Remove notification bar
		 * this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		 * WindowManager.LayoutParams.FLAG_FULLSCREEN);
		 */
	}

//    android:configChanges="keyboardHidden|orientation"

	private void initPreferences() {
		pref = getSharedPreferences("VCDA_preferences", Activity.MODE_WORLD_READABLE);
		prefMaxSpeed = pref.getInt("maxSpeed", 200);
		prefDistance = pref.getInt("distance", 1000);
		prefTime = 1000 * pref.getInt("time", 15);
		//TODO speedUnit
		prefSpeedUnit = pref.getString("speedUnit", "km/h");
		prefWakelock = pref.getBoolean("wakeLock", true);
		prefSpeakerOn = pref.getBoolean("speakerOn", true);
		prefAvgSpeed = pref.getInt("avgSpeed", 1);
		prefMaxSpeedOnBar = pref.getBoolean("maxSpeedOnBar", true);
	}
 
	//zapis stanu zmiennych przed obrotem telefonu
	@Override
	public Object onRetainNonConfigurationInstance() {
	    HashMap<String, Object> tempValues = new HashMap<String, Object>();
	    tempValues.put("speedBar", bar.getValue());
	    tempValues.put("tvInformations", tvInformations.getText());
		tempValues.put("tvAdres", tvAdres.getText());
		tempValues.put("tvMaxSpeed", tvMaxSpeed.getText());
		tempValues.put("tvStartTime", tvStartTime.getText());
		tempValues.put("tvAvgSpeed", tvAvgSpeed.getText());
		final HashMap<String, Object> values = tempValues; 
	    return values;
	}
	
	private void setScreenLock(boolean on){
		Log.d("VCDA", "screenlock::"+on+" "+wl);
		if (wl == null){
			//ustawienie WAKELOCK - nie wygaszania ekranu z flagą jasnosc na maxa
			PowerManager power = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			wl = power.newWakeLock(
					PowerManager.SCREEN_BRIGHT_WAKE_LOCK
							| PowerManager.ON_AFTER_RELEASE, "VCDA");
		}
		
		if (on && !wl.isHeld() && prefWakelock){
			wl.acquire();
			Log.d("VCDA","acquire");
		}else if (!on){
			if (wl.isHeld()){
				wl.release();
				Log.d("VCDA","release");
			}
			wl = null;
		}
	}
	
	private void checkGPS() {
		try{
	        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
	            isGPS = false;
	        else
	        	isGPS = true;
		}catch (Exception e) {
			Log.e("VCDA", "Missing gps provider!");
			isGPS = false;
		}
		
        bar.setGPSStatus(isGPS);
	}

	/**
	 * Metoda pobierająca dane co jakiś czas 
	 * przy prędkości < 30km/h - co 30s
	 * przy prędkości > 30km/h - co 2km lub 5 minut lub zakręt
	 * @param latitude
	 * @param longitude
	 * @param speed
	 * @return
	 */
	private void getAdressInterval(Location location, boolean force){
		//double latitude, double longitude, long speed

		if (location == null)
        	return ;
        
        long speed = Math.round(location.getSpeed()*3.6);
     
        if (lastDistanseLocationGPS == null || 
        		force || 
        		speed == 0 || 
        		(speed > 30 && (location.distanceTo(lastDistanseLocationGPS) > prefDistance)) || 
        		(speed <= 30 && (lastDistanseLocationGPS.getTime() + prefTime) < System.currentTimeMillis())){

        	//warunek, zeby na postoju ciągle nie sciągał
        	if (speed == 0){
        		if (!speedToZero)
        			return;
        		else{
        			speedToZero = false;
        			if (subTimeMeter != 0){
        				timeMeter += System.currentTimeMillis() - subTimeMeter;
        				averageSpeed(location);
        			}
        			subTimeMeter = 0;
        		}
        	}else{
        		//jeżeli ruszyliśmy wystartuj timer
        		if (!speedToZero)
        			subTimeMeter = System.currentTimeMillis();
        		
        		speedToZero = true;
        	}
        	Log.d("VCDA", "Speed::"+speed+", speedToZero::"+speedToZero);
        	try {
            	getAdress(location.getLatitude(), location.getLongitude());
            	lastDistanseLocationGPS = location;
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            }
        
        }
	}

	private void getAdress(double latitude, double longitude) throws IOException{
		GetAsyncAdres task = new GetAsyncAdres(latitude, longitude);
		task.execute();
	}
	
	private void gpsAdditionalInfo(Location location) {
		if (startLocation != null && location != null){
			float dist = startLocation.distanceTo(location);
			if (lastLocation != null){
				float currentDistance = lastLocation.distanceTo(location); 
				realDistance +=  currentDistance;
				dist = realDistance;
//				if (currentDistance > 0)
//					dbAdapter.insertCoordinates(location.getLatitude(), location.getLongitude(), currentDistance, realDistance ,epoch2time(System.currentTimeMillis()));
			}
			lastLocation = location;
			String tvI = this.getString(R.string.distance) + ": ";
			DecimalFormat df = new DecimalFormat("#.###");
			
			if (prefSpeedUnit.equals("km/h")){
				pointToPointDistance = (dist / 1000);
				tvI += String.valueOf(df.format(pointToPointDistance))+" km";
			}else{
				pointToPointDistance = dist * METERSTOMILES;
				tvI += String.valueOf(df.format(pointToPointDistance))+" miles";
			}

			
			tvInformations.setText(tvI);
			if (firstFixGPS == 0){
				firstFixGPS = location.getTime();
				tvStartTime.setText("Start: "+epoch2time(firstFixGPS)+" ");
			}
			
			float speedms = location.getSpeed();
	        long speed = 0;
	        if (prefSpeedUnit.equals("km/h"))
	        	speed = Math.round(speedms * 3.6);
	        else
	        	speed = Math.round(speedms * 2.23693629);
	        
	        
	        if (maxSpeed < speed){
	        	bar.setMaxTripSpeed((int) speed);
	        	maxSpeed = speed;
	        	tvMaxSpeed.setText("V max: "+String.valueOf(maxSpeed)+" "+prefSpeedUnit);
	        }
	        String speedkmh = String.valueOf(speed);
	        getAdressInterval(location, false); 
	        bar.setValue(Integer.valueOf(speedkmh), false);
		}
	}
	
	private void averageSpeed(Location location) {
//		pointToPointDistance - odleglosc w odpowiedniej jednostce (mile lub km)
//		timeMeter - czas przejazdu
		int tempAvgSpeed ;
		long tTimeMeter = 0;

		if (prefAvgSpeed == COUNT_AVG_SPEED_ONLY_ON_MOVE)
			tTimeMeter = timeMeter;
		else
			tTimeMeter = location.getTime() - firstFixGPS;
		
		//zabezpieczenie przed bzdurami - musiał zostać dokonany pomiar czasu oraz odległośc > 0,2 jednostki
		if (tTimeMeter > 0 && pointToPointDistance > 0.2)
			tempAvgSpeed = (int)Math.round(pointToPointDistance / ((double)tTimeMeter/(double)3600000));
		else{
			tempAvgSpeed = 0;
			tvAvgSpeed.setText(this.getString(R.string.avgSpeed)+": "+ tempAvgSpeed +" "+prefSpeedUnit);
		}
//		Log.d(VCDA, "dist::"+pointToPointDistance+" | czas::"+tTimeMeter+" | cntTime::"+(double)((double)tTimeMeter/(double)3600000)+" | avg::"+tempAvgSpeed);
		if (avgSpeed != tempAvgSpeed){
			avgSpeed = tempAvgSpeed;
			tvAvgSpeed.setText(this.getString(R.string.avgSpeed)+": "+ avgSpeed +" "+prefSpeedUnit);
		}
	}

	public static String epoch2time(Long str){
		SimpleDateFormat formatter = new java.text.SimpleDateFormat ("HH:mm:ss");
		int tz = TimeZone.getDefault().getRawOffset(); 
		tz=0; //TODO: sprawdzić czy dobrze bierze strefy
		String epoch = formatter.format(new java.util.Date(str+tz));
		return epoch;
	}

	protected void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,getString(R.string.voice_command));
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		vc.resetOutputData();
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
			// Fill the list view with the strings the recognizer thought it could have heard
			ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//			 Toast.makeText(getApplicationContext(), matches.toString(), Toast.LENGTH_LONG).show();
			Log.d("VCDA", matches.toString());
			
			int result = vc.runIntents(matches);
			
			//jezeli jest wynik
			if (result > 0){
				if (result == VoiceController.RESULT_ALERTDIALOG){
					AlertDialog ad = (AlertDialog)vc.getOutputData(VoiceController.RESULT_ALERTDIALOG);
					ad.show();
				}
			}

		}
		
		if (requestCode == INTENT_FOR_CONFIG){
			initPreferences();
			bar.setMaxSpeedValue(prefMaxSpeed);
			bar.setSpeedUnit(prefSpeedUnit);
			Log.d("VCDA","afterConfig::"+prefWakelock);
			setScreenLock(prefWakelock);
			audioManager.setActivate(prefSpeakerOn);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}


	public static void callIntent(String intentType, String numer){
		Intent callIntent = new Intent();
		if (intentType.equals(Intent.ACTION_CALL)){
			String number = "tel:" + numer;
	        callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(number)); 
		}
	    context.startActivity(callIntent);
	}
	
	/**
	 * Metoda sprawdzająca, czy jest połączenie z internetem
	 * @return
	 */
	private boolean isNetworkAvailable() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
	    // if no network is available networkInfo will be null, otherwise check if we are connected
	    if (networkInfo != null && networkInfo.isConnected()) {
	        return true;
	    }
	    return false;
	}


	private class GetAsyncAllContacts extends AsyncTask<Void, Void, Void>{
		private Context ctx;

		public GetAsyncAllContacts(Context ctx) {
			this.ctx = ctx; 
		}

		@Override
		protected Void doInBackground(Void... params) {
			vc.getAllContacts(); 
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			speakButton.setText(ctx.getString(R.string.recognitionButton));
			speakButton.setEnabled(true);
		}
		
	}
	
	
	private class GetAsyncAdres extends AsyncTask<Void, Void, List<Address>>{
		
		private double latitude;
		private double longitude;

		public GetAsyncAdres(double latitude, double longitude){
			this.latitude = latitude;
			this.longitude = longitude;
		}
 
		@Override
		protected List<Address> doInBackground(Void... params) {
			List<Address> result; 
			try {
				result = geocoder.getFromLocation(latitude, longitude, 1);
				return result;
			} catch (IOException e) {
				e.printStackTrace();
			} 
			return null;
		}
		
		@Override
		protected void onPostExecute(List<Address> result){
			super.onPostExecute(result);
			if (result != null){
				String city = ""; 
				try{
					String[] line = result.get(0).getAddressLine(1).split(" ");
					city = line[1];
					tvAdres.setText(result.get(0).getAddressLine(0)+" "+city);
					Log.d("VCDA","Adres::"+result.get(0).getAddressLine(0)+" "+city);
				}catch (Exception e){
//					Log.d("ERROR", line.toString());
				}
//				tvAdres.setText(result.get(0).getAddressLine(0)+" "+city);
//				Log.d("VCDA","Adres::"+result.get(0).getAddressLine(0)+" "+city);
			}
		}
	}
	
	
	public class CallReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	Log.d("VCDA", "activityIsActive::"+activityIsActive);
	    	if (activityIsActive){
				CallStateListener phoneListener=new CallStateListener(context, audioManager);
				TelephonyManager telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
				telephony.listen(phoneListener,PhoneStateListener.LISTEN_CALL_STATE);
	    	}
	    }
	}
	
	private void closeDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Exit?");
		builder.setMessage("Would you like to stop the application?");
//		builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//			@Override
			public void onClick(DialogInterface dialog, int which) {
				clearOnPause();
		    	finish();
		    	System.exit(0);
			}
			
		});
		
		builder.setNegativeButton("No",new DialogInterface.OnClickListener() {
//			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
			
		}); 
		AlertDialog alertDialogOutput = builder.create();
		alertDialogOutput.show();
	}
}