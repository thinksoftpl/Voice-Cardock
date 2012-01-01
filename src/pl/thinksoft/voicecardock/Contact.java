package pl.thinksoft.voicecardock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Contact {

	private static HashMap<String, Contact> instance = new HashMap<String, Contact>();

	//przechowuje numer i typ numeru
	private ArrayList<String[]> number = new ArrayList<String[]>();
	//number [0]numer; [1]englishlabel; [2]localelabel
	private String contact;

	private static boolean locked = false;
	
	private Contact(String contact){
		this.contact = contact;
	}
	
	public static Contact get (String contactName){
		if (!instance.containsKey(contactName))
			return null;
		return instance.get(contactName);
	}
	
	/**
	 * Metoda zwraca informacje, czy istnieje taki kontakt
	 * @param contactName
	 * @return
	 */
	public static boolean isContact (String contactName){
		return instance.containsKey(contactName);
	}
	
	/**
	 * Metoda dodająca numer do kontaktu, który istnieje
	 * Gdy kontakt nie istnieje, tworzy go
	 * @param contactName - DISPLAY_NAME
	 * @param number - numer
	 * @param typeString - etykieta numeru
	 * @param typeLocale - etykieta numeru w danym języku
	 */
	public static void addNumber (String contactName, String number, String type){
		if (locked)
			return;
		
		//jeżeli nie ma takiego kontaktu, to dodajemy
		if (!instance.containsKey(contactName)){
			instance.put(contactName, new Contact(contactName));
		}
		
		//pobieramy kontakt i wpisujemy numer i rodzaj kontaktu
		instance.get(contactName).number.add(new String[]{number, type});
	}
	
	/**
	 * Metoda zwraca wszytstkie numery danego kontaktu wraz z typami
	 * @return
	 */
	public ArrayList<String[]> getNumbers(){
		return number;
	}
	
	/**
	 * Metoda zwraca numery tylko pewnego typu, podanego z rozpoznania mowy (np praca)
	 * @param type - String typ telefonu (np. praca)
	 * @return ArrayList<String[]> Lista numerów z opisem
	 */
	public ArrayList<String[]> getNumbersByType(String type){
		ArrayList<String[]> result = new ArrayList<String[]>();
		Iterator <String[]> it = number.iterator();
		while (it.hasNext()){
			String[] currIt = it.next();
			if (currIt[1].equals(type)){
				result.add(currIt);
			}
		}
		return result;
	}
	
	/**
	 * Ustawia flagę, aby nie wczytywać ponownie kontaktów
	 */
	public static void setLock(){
		locked  = true;
	}
	
	
}
