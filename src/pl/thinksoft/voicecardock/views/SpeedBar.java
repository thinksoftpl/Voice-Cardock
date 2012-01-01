package pl.thinksoft.voicecardock.views;

import java.util.ArrayList;

import pl.thinksoft.voicecardock.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class SpeedBar extends LinearLayout{

	private Context context;
	private int minValue;
	private int maxValue;
	private ArrayList<Integer> speedBarsImage = new ArrayList<Integer>();
	private int currentLastSpeedBars = 0;
	private int countBars = 21;	//ilość kresek
	
	public SpeedBar(Context context) {
		super(context);
        this.context = context;
        prepareLayout();
	}

	public SpeedBar(Context context, AttributeSet attrs) {
		super(context, attrs);
        this.context = context;
        prepareLayout();
	}

	private void prepareLayout() {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.speedbar, null);
		addView(view);
		

		speedBarsImage.add((R.id.imgA));
		speedBarsImage.add((R.id.imgB));
		speedBarsImage.add((R.id.imgC));
		speedBarsImage.add((R.id.imgD));
		speedBarsImage.add((R.id.imgE));
		speedBarsImage.add((R.id.imgF));
		speedBarsImage.add((R.id.imgG));
		speedBarsImage.add((R.id.imgH));
		speedBarsImage.add((R.id.imgI));
		speedBarsImage.add((R.id.imgJ));
		speedBarsImage.add((R.id.imgK));
		speedBarsImage.add((R.id.imgL));
		speedBarsImage.add((R.id.imgM));
		speedBarsImage.add((R.id.imgN));
		speedBarsImage.add((R.id.imgO));
		speedBarsImage.add((R.id.imgP));
		speedBarsImage.add((R.id.imgQ));
		speedBarsImage.add((R.id.imgR));
		speedBarsImage.add((R.id.imgS));
		speedBarsImage.add((R.id.imgT));
		speedBarsImage.add((R.id.imgU));
		
//		Bitmap gradient = BitmapFactory.decodeResource(getResources(), R.drawable.radiant);
//		Bitmap[] steps = splitBitmap(gradient);

	}

	private Bitmap[] splitBitmap(Bitmap picture)
	{
		Bitmap scaledBitmap = Bitmap.createScaledBitmap(picture, 240, 240, true);
		Bitmap[] imgs = new Bitmap[9];
		imgs[0] = Bitmap.createBitmap(scaledBitmap, 0, 0, 80 , 80);
		imgs[1] = Bitmap.createBitmap(scaledBitmap, 80, 0, 80, 80);
		imgs[2] = Bitmap.createBitmap(scaledBitmap,160, 0, 80,80);
		imgs[3] = Bitmap.createBitmap(scaledBitmap, 0, 80, 80, 80);
		imgs[4] = Bitmap.createBitmap(scaledBitmap, 80, 80, 80,80);
		imgs[5] = Bitmap.createBitmap(scaledBitmap, 160, 80,80,80);
		imgs[6] = Bitmap.createBitmap(scaledBitmap, 0, 160, 80,80);
		imgs[7] = Bitmap.createBitmap(scaledBitmap, 80, 160,80,80);
		imgs[8] = Bitmap.createBitmap(scaledBitmap, 160,160,80,80);
		return imgs;
	}
	
	public void setInitParams(int min, int max){
		this.minValue = min;
		this.maxValue = max;
	}
	
	/**
	 * Metoda, która oblicza, który segment ma być ostatni
	 * @param speed - prędkość
	 * @return
	 */
	private int calculateSpeedBars(int speed){
		int nextIndex = 0;
		float m = countBars*speed;
		float m2 = maxValue;
		float wynik = m/m2;
		nextIndex = Math.round(wynik);
//		Log.d("speedbar","index::"+nextIndex+" speed::"+speed+" przed roud::"+wynik+" cb::"+countBars+" maxV::"+maxValue+" m::"+m+" m2::"+m2);
		
		//jeżeli jest źle ustaiony max, wyrysuj tylko pełen zasięg bar.
		if (nextIndex > countBars)
			nextIndex = countBars;
		
		//jeżli jest prędkośc > 0 to narysuj jeden bar
		if (nextIndex == 0 && speed > 0)
			nextIndex = 1;
//		Log.d("speedbar","index::"+nextIndex+" speed::"+speed+" maxValue::"+maxValue);
		return nextIndex;
	}
	
	public void setSpeed(int speed){
//		Log.d("speedBar","sadsd");
		int nextIndex = calculateSpeedBars(speed);
		if (nextIndex > 0) nextIndex --; 
		ImageView iv;
		Log.d("speedbar","LAST::"+currentLastSpeedBars+" NEXT::"+nextIndex);
		
		if (currentLastSpeedBars < nextIndex){
			//zapal barsy
			for (int i=currentLastSpeedBars; i<nextIndex; i++){
				Log.d("speedbar",speedBarsImage.get(i)+" i::"+i);
				iv = (ImageView) findViewById(speedBarsImage.get(i));
				
				iv.setVisibility(View.VISIBLE);
			}
			currentLastSpeedBars = nextIndex;
		}
		
		if (currentLastSpeedBars > nextIndex){
			for (int i=nextIndex; i < currentLastSpeedBars; i++){
				iv = (ImageView) findViewById(speedBarsImage.get(i));
				iv.setVisibility(View.INVISIBLE);
			}
			currentLastSpeedBars = nextIndex;
		}
		
		
	}
	
	
}
