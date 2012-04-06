package net.yihabits.mobile.ringtone.db;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.api.translate.Language;
import com.google.api.translate.Translate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class RingtoneDAO {

	private SQLiteDatabase db;
	private final Context context;

	private static RingtoneDAO instance;
	private RingtoneDBOpenHelper sdbHelper;
	
	private RingtoneDAO(Context c) {
		this.context = c;
		this.sdbHelper = new RingtoneDBOpenHelper(this.context);
	}

	public void close() {
		if(db != null){
		db.close();
		}
	}

	public void open() throws SQLiteException {
		try {
			db = sdbHelper.getWritableDatabase();
		} catch (SQLiteException ex) {
			Log.v("Open database exception caught", ex.getMessage());
			db = sdbHelper.getReadableDatabase();
		}
	}

	public static RingtoneDAO getInstance(Context c) {
		if (instance == null) {
			instance = new RingtoneDAO(c);
		}
		return instance;
	}

	public Cursor getAllRingtone() {
		Cursor c = db.query(RingtoneDBOpenHelper.RINGTONE_TABLE_NAME, null, null,
				null, null, null, null);
				
		return c;
	}
	
	public Cursor getRingtoneByUrl(String url) {
		Cursor c = db.query(RingtoneDBOpenHelper.RINGTONE_TABLE_NAME, null, RingtoneDBOpenHelper.URL + " = ?", new String[]{url},
				 null, null, null);
				
		return c;
	}

	public int getMaxId() {
		return 0;
	}

	public long insert(RingtoneModel am) {
		open();
		try{
			ContentValues newRingtoneValue = new ContentValues();
			newRingtoneValue.put(RingtoneDBOpenHelper.URL, am.getUrl());
			newRingtoneValue.put(RingtoneDBOpenHelper.NAME, am.getName());
			newRingtoneValue.put(RingtoneDBOpenHelper.NAME_EN, am.getNameEn());
			newRingtoneValue.put(RingtoneDBOpenHelper.LOCATION, am.getLocation());
			newRingtoneValue.put(RingtoneDBOpenHelper.IS_USED, 0);
			return db.insert(RingtoneDBOpenHelper.RINGTONE_TABLE_NAME, null, newRingtoneValue);
			} catch(SQLiteException ex) {
				Log.v("Insert into database exception caught",
						ex.getMessage());
				return -1;
			}
	}
	
	public long update(RingtoneModel am) {

		try{
			ContentValues newRingtoneValue = new ContentValues();
//			newServerValue.put("_id", sm.getId());
			newRingtoneValue.put(RingtoneDBOpenHelper.URL, am.getUrl());
			newRingtoneValue.put(RingtoneDBOpenHelper.NAME, am.getName());
			if(am.getNameEn() != null){
			newRingtoneValue.put(RingtoneDBOpenHelper.NAME_EN, am.getNameEn());
			}
			if(am.getLocation() != null){
				newRingtoneValue.put(RingtoneDBOpenHelper.LOCATION, am.getLocation());
			}
			newRingtoneValue.put(RingtoneDBOpenHelper.IS_USED, am.getIsUsed());
			return db.update(RingtoneDBOpenHelper.RINGTONE_TABLE_NAME, newRingtoneValue, "_id=" + am.getId(), null);
			} catch(SQLiteException ex) {
				Log.v("update database exception caught",
						ex.getMessage());
				return -1;
			}
	}
	
	public long updateOrInsert(RingtoneModel rm) {
		long res = update(rm);
		if(res > 0){
			return res;
		}else{
			//insert
			rm.setNameEn(translate(rm.getName()));
			return insert(rm);
		}
	}
	
	public long updateLocation(String url, String location) {

		try{
			ContentValues newRingtoneValue = new ContentValues();
			newRingtoneValue.put(RingtoneDBOpenHelper.LOCATION, location);
			return db.update(RingtoneDBOpenHelper.RINGTONE_TABLE_NAME, newRingtoneValue, RingtoneDBOpenHelper.URL + "='" + url +"'", null);
			} catch(SQLiteException ex) {
				Log.v("update database exception caught",
						ex.getMessage());
				return -1;
			}
	}
	
	public long updateUsed(String url, int used) {

		try{
			ContentValues newRingtoneValue = new ContentValues();
			newRingtoneValue.put(RingtoneDBOpenHelper.IS_USED, used);
			return db.update(RingtoneDBOpenHelper.RINGTONE_TABLE_NAME, newRingtoneValue, RingtoneDBOpenHelper.URL + "='" + url +"'", null);
			} catch(SQLiteException ex) {
				Log.v("update database exception caught",
						ex.getMessage());
				return -1;
			}
	}
	
	public long delete(long id) {
		try{
			return db.delete(RingtoneDBOpenHelper.RINGTONE_TABLE_NAME, "_id=" + id, null);
			} catch(SQLiteException ex) {
				Log.v("delete database exception caught",
						ex.getMessage());
				return -1;
			}
	}
	
	public String translate(String src) {
		Translate.setHttpReferrer("http://www.omdasoft.com");

		String translatedText;
		try {
			translatedText = Translate.execute(src, Language.CHINESE,
					Language.ENGLISH);
		} catch (Exception e) {
			translatedText = src;
		}

		return translatedText;
	}

	public Cursor getLocalRingtone(int page) {
		Cursor c = db.query(RingtoneDBOpenHelper.RINGTONE_TABLE_NAME, null, RingtoneDBOpenHelper.IS_USED + " > 0", null,
				 null, null, null, ((page - 1)*10) + ", " + 10);
		return c;
	}
}
