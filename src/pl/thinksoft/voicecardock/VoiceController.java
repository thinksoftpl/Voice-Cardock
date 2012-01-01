package pl.thinksoft.voicecardock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

public class VoiceController {

	final static int RESULT_ALERTDIALOG = 200;
	final static int RESULT_ARRAYLIST = 201;
	
	private Context context;
	public HashMap<String, String[]> contactsList = new HashMap<String, String[]>();
	private Contact contacts;
	private ArrayList<String> arrayListOutput = new ArrayList<String>();
	private ArrayList<String> numbersForCall = new ArrayList<String>();
	private AlertDialog alertDialogOutput;
	private Activity vca;
	private ArrayList<String> localePhoneType = new ArrayList<String>();
	private Pattern FIND_CALL;
	private Pattern FIND_SMS;
	

	public VoiceController(Context context, Contact contacts, Activity dis) {
		this.context = context;
		this.contacts = contacts;
		this.vca = dis;
		initPatterns();
	}

	public void initPatterns(){
		FIND_CALL = Pattern.compile("^"+context.getString(R.string.findCall)+" (.*)");
		FIND_SMS = Pattern.compile("wyślij (esemes|sms) do\\s+([a-z,A-Z,0-9, ]+)");
	}
	
	// pobieram wszystkie kontakty do zmiennej contactsList, z indeksem
	// DISPLAY_NAME
	public void getAllContacts() {
		ContentResolver cr = context.getContentResolver();
		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
				null, null, null);
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				String id = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts._ID));
				String name = cur
						.getString(cur
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (Integer
						.parseInt(cur.getString(cur
								.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
					Cursor pCur = cr.query(
							ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							null,
							ContactsContract.CommonDataKinds.Phone.CONTACT_ID
									+ " = ?", new String[] { id }, null);
					if (pCur.getColumnCount() > 0){
						while (pCur.moveToNext()) {
							String phoneNumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
							Integer phoneType = pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
							String label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(),phoneType, "").toString().toLowerCase(Locale.getDefault());
							addPhoneLabel(label);
							Contact.addNumber(name.toLowerCase(), phoneNumber, label);	//label jest w języku wybranym na telefonie
						}
					}
					pCur.close();
				}
			}
		}
		Contact.setLock();
	}

	private void addPhoneLabel(String label){
		if (!localePhoneType.contains(label))
			localePhoneType.add(label);
	}

	/**
	 * Zwraca przygotowane przez kontroler dane wyjściowe
	 * @return
	 */
	public Object getOutputData (int resultKind){
		if (resultKind == RESULT_ALERTDIALOG)
			return alertDialogOutput;
		else if (resultKind == RESULT_ARRAYLIST)
			return arrayListOutput;
		else
			return null;
	}

	/**
	 * Resetu zmienną danych wyjściowychs
	 */
	public void resetOutputData(){
		arrayListOutput.clear();
		numbersForCall.clear();
		if (alertDialogOutput != null)
			alertDialogOutput.cancel();
	}
	
	public int runIntents(ArrayList<String> matches) {
		Iterator<String> iMatches = matches.iterator();
		while (iMatches.hasNext()) {
			Matcher m;
			String expression = iMatches.next();
			Log.v("VCDA","recognized expression::"+expression+" findcall::"+FIND_CALL);
			m = FIND_CALL.matcher(expression);
			
			if (m.matches()) {
				String[] words = m.group(1).split(" ");
				String lastWord = words[words.length-1];
				
				int count = words.length;
				boolean isPrecisedTypePhone = false;
				
				//jezeli ostatnim słowem jest typ telefonu
				if (localePhoneType.contains(lastWord)){
					count --;
					isPrecisedTypePhone = true;
				}
				
				String displayNameRecognized = "";
				for (int i=0; i<count; i++)
					displayNameRecognized +=words[i]+" ";
				
				String displayNameTrimmed = displayNameRecognized.trim();

//				Toast.makeText(context, ">>"+displayNameTrimmed+"<<", Toast.LENGTH_SHORT).show();
				
				if (Contact.isContact(displayNameTrimmed)){
					ArrayList<String[]> numery = new ArrayList<String[]>();
					
					Contact instanceContact = Contact.get(displayNameTrimmed); 
					if (instanceContact != null){
						if (isPrecisedTypePhone)
							numery = instanceContact.getNumbersByType(lastWord);
						else
							numery = instanceContact.getNumbers();
						Iterator <String[]> it = numery.iterator();
						while (it.hasNext()){
							String[] b = it.next();
							arrayListOutput.add(displayNameTrimmed+": "+b[0].replace("-", "")+" ("+b[1]+")");
							numbersForCall.add(b[0]);
						}
					}
				}
				
			}
	
			m = FIND_SMS.matcher(expression);
			if (m.matches()) {
				/*
				 * PendingIntent pi = PendingIntent.getActivity(this, 0, new
				 * Intent(this, SmsActivity.class), 0); SmsManager sms =
				 * SmsManager.getDefault(); sms.sendTextMessage(m.group(1), null,
				 * message, pi, null);
				 */
				// return true;
			}
		}
		
		//Mamy już wyniki dopasowane
		if (arrayListOutput!= null && arrayListOutput.size() > 0){
			
			//tworzymy dialog box
			final String[] items = arrayListOutput.toArray(new String[arrayListOutput.size()]);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(vca);
			builder.setTitle(context.getString(R.string.chooseNumber));
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        Toast.makeText(context, items[item], Toast.LENGTH_SHORT).show();
			        VoiceCarDockActivity.callIntent(Intent.ACTION_CALL, numbersForCall.get(item));
			    }
			});
			alertDialogOutput = builder.create();
			return RESULT_ALERTDIALOG;
		}else{
			Toast.makeText(context, context.getString(R.string.noContacts), Toast.LENGTH_SHORT).show();
		}

		
		
		return 0;
	}

}
