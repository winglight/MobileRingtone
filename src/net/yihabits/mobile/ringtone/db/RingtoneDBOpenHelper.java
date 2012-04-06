package net.yihabits.mobile.ringtone.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RingtoneDBOpenHelper extends SQLiteOpenHelper {
	
	public  static final int DATABASE_VERSION = 2;
    public  static final String RINGTONE_TABLE_NAME = "ringtone";
	public  static final String URL = "URL";
	public  static final String NAME = "NAME";
	public  static final String NAME_EN = "NAME_EN";
	public  static final String LOCATION = "LOCATION";
	public  static final String IS_USED = "IS_USED";
	
    public  static final String RINGTONE_TABLE_CREATE =
                "CREATE TABLE " + RINGTONE_TABLE_NAME + " (" +
                "_id integer primary key autoincrement," +
                URL + " TEXT, " +
                LOCATION + " TEXT, " +
                IS_USED + " INTEGER, " +
                NAME_EN + " TEXT, " +
    NAME + " TEXT);";
	public static final String DATABASE_NAME = "ringtone";

    RingtoneDBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RINGTONE_TABLE_CREATE);
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		android.util.Log.w("Constants", "Upgrading database, which will destroy all old	data");
				db.execSQL("DROP TABLE IF EXISTS " + RINGTONE_TABLE_NAME);
				onCreate(db);
		
	}

}
