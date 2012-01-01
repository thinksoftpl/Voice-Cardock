package pl.thinksoft.voicecardock;

import org.acra.*;
import org.acra.annotation.*;

import android.app.Application;

@ReportsCrashes(formKey = "dE90ZHZaeFZrQ1RCcE9zaWY1ZE1CbHc6MQ")
public class VoiceCarDock extends Application{
	@Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
    }
}
