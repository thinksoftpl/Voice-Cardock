package pl.thinksoft.voicecardock;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.telephony.SmsManager;

public class SmsActivity extends Activity{

	private void sendSMS(String phoneNumber, String message)
    {        
//        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, SMS.class), 0);                
//        SmsManager sms = SmsManager.getDefault();
//        sms.sendTextMessage(phoneNumber, null, message, pi, null);        
    }    
	
}
