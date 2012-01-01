package pl.thinksoft.voicecardock;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

public class VCDAudioManager {

	private AudioManager audioManager;
	private boolean activate;
	private boolean semafor = false;

	public VCDAudioManager(Context ctx, boolean activate) {
        audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        this.activate = activate;
	}
	
	public void speakerOn(){
		if (activate && !semafor){
//			if (audioManager.getMode() == AudioManager.MODE_IN_CALL) {
//                return;
//			}
			audioManager.setSpeakerphoneOn(true);
			semafor = true;
			Log.d("VCDA","SpeakerOn");
		}
	}
	
	public void speakerOff(){
		if (activate && semafor){
			audioManager.setSpeakerphoneOn(false);
			semafor = false;
			Log.d("VCDA","SpeakerOff");
		}
	}
	
	public boolean speakerActive(){
		return audioManager.isSpeakerphoneOn();
	}
	
	public void setActivate(boolean act){
		this.activate = act;
	}
	
}
