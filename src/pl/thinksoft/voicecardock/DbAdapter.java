package pl.thinksoft.voicecardock;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {
	private static final int DB_VERSION = 2;
	private static final String DB_NAME = "database.db";
	private static final String DB_COORDINAT_TABLE = "coordinates";
	private SQLiteDatabase db;
	private Context context;
	private DatabaseHelper dbHelper;
	private static final String CREATE_DB_COORDINAT_TABLE = "CREATE TABLE "
			+ DB_COORDINAT_TABLE +"(" 
			+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, latitude real, longitude real, distance real, counted_distance integer, timestamp text)";

	public DbAdapter(Context applicationContext) {
		this.context = applicationContext;
	}

	public DbAdapter open() {
		dbHelper = new DatabaseHelper(context, DB_NAME, null, DB_VERSION);
		
		try {
			db = dbHelper.getWritableDatabase();	
			db.execSQL("delete from "+DB_COORDINAT_TABLE);
		} catch (SQLException e) {
			db = dbHelper.getReadableDatabase();
		}

		return this;
	}

	public void close() {
		dbHelper.close();
	}

	public long insertCoordinates(double latitude, double longitude, float distance, int realDistance, String tStamp) {
		ContentValues newValues = new ContentValues();
		newValues.put("latitude", latitude);
		newValues.put("longitude", longitude);
		newValues.put("distance", distance);
		newValues.put("counted_distance", realDistance);
		newValues.put("timestamp", tStamp);
		return db.insert(DB_COORDINAT_TABLE, null, newValues);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_DB_COORDINAT_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO
			db.execSQL("DROP TABLE IF EXISTS "+DB_COORDINAT_TABLE);
			onCreate(db);
		}

	}
}
