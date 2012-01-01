package pl.thinksoft.voicecardock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallStateListener extends PhoneStateListener{
	private Context context;
	private VCDAudioManager am;
	private SharedPreferences prefs;

	public CallStateListener(Context context, VCDAudioManager am) {
		this.context = context;
		this.am = am;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	//zapobiegamy odpaleniu głośnika, jeżeli nie jest aktywne activity
	private boolean isActivityActive(){
		if (prefs.getBoolean("activeActivity", false) && prefs.getBoolean("speakerOn", false))
			return true;
		else 
			return false;
	}

	public void onCallStateChanged(int state, String incomingNumber) {
		Log.d("VCDA", "onCallStateChanged:: "+ state);
		switch (state) {

		case TelephonyManager.CALL_STATE_IDLE:{
			am.speakerOff();
			Log.d("VCDA", "speakerOff");
		}
			break;
			
		case TelephonyManager.CALL_STATE_OFFHOOK:
			if (!am.speakerActive() && isActivityActive()){
				am.speakerOn();
				Log.d("VCDA", "speakerOn_1");
			}
			break;

		case TelephonyManager.CALL_STATE_RINGING:
			if (!am.speakerActive() && isActivityActive()){
				am.speakerOn();
				Log.d("VCDA", "speakerOn");
			}
			break;
		}

	}
}
//context.startService(new Intent(context, AutoAnswerIntentService.class));
