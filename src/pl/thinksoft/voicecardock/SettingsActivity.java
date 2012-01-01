package pl.thinksoft.voicecardock;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity{

	private SharedPreferences pref;
	private EditTextPreference prefMaxSpeed;
	private EditTextPreference prefDistance;
	private EditTextPreference prefTime;
	private ListPreference prefSpeedUnit;
	private CheckBoxPreference prefWakelock;
	private ListPreference prefAvgSpeed;
	private CheckBoxPreference prefSpeakerOn;
	private CheckBoxPreference prefMaxSpeedOnBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		pref = getSharedPreferences("VCDA_preferences", Activity.MODE_PRIVATE);
		prefMaxSpeed = (EditTextPreference) findPreference("maxSpeed");
		prefDistance = (EditTextPreference) findPreference("distanceV30");
		prefTime = (EditTextPreference) findPreference("time30v");
		prefSpeedUnit = (ListPreference) findPreference("speedUnit");
		prefWakelock = (CheckBoxPreference) findPreference("wakeLock");
		prefSpeakerOn = (CheckBoxPreference) findPreference("speakerOn");
		prefAvgSpeed = (ListPreference) findPreference("avgSpeed");
		prefMaxSpeedOnBar = (CheckBoxPreference) findPreference("maxSpeedOnBar");
	}

	@Override
	protected void onPause() {
		super.onPause();
		savePreferences();
	}
	
	private void savePreferences() {
		SharedPreferences.Editor editor = pref.edit();
		try{
			editor.putInt("maxSpeed", Integer.parseInt(prefMaxSpeed.getText().trim()));
			editor.putInt("distance", Integer.parseInt(prefDistance.getText().trim()));
			editor.putInt("time", Integer.parseInt(prefTime.getText().trim()));
			editor.putString("speedUnit", prefSpeedUnit.getValue());
			editor.putBoolean("wakeLock", prefWakelock.isChecked());
			editor.putBoolean("speakerOn", prefSpeakerOn.isChecked());
			editor.putBoolean("maxSpeedOnBar", prefMaxSpeedOnBar.isChecked());
			editor.putInt("avgSpeed", Integer.parseInt(prefAvgSpeed.getValue()));
			editor.commit();
		}catch (Exception e) {
			editor.clear();
			Toast.makeText(getApplicationContext(), "Can't save setting because of entered wrong value!", Toast.LENGTH_LONG).show();
		}
	}

	
	
}
