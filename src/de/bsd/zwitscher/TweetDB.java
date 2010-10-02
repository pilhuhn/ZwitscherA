package de.bsd.zwitscher;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class TweetDB {
	
	private TweetDBOpenHelper tdHelper;

	public TweetDB(Context context) {
		tdHelper = new TweetDBOpenHelper(context, "TWEET_DB", null, 1);
	}
	
	private class TweetDBOpenHelper extends SQLiteOpenHelper {

		public TweetDBOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE statuses (" + //
					"ID LONG, " +//
					"USER TEXT, " + //
					"MSG TEXT )" 
					);
			db.execSQL("CREATE TABLE lastRead (" + //
					"item TEXT, " + //
					"time LONG )" //
			);
			db.execSQL("CREATE TABLE lists (" + //
					"name TEXT, " + //
					"id LONG )"
			);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	long getLastRead(String name) {
		SQLiteDatabase db = tdHelper.getReadableDatabase();
		Cursor c = db.query("lastRead", new String[] {"time"}, "item = ?", new String[] {name}, null, null, null);
		Long ret;
		if (c.getCount()==0)
			ret = 0L;
		else {
			c.moveToFirst();
			ret = c.getLong(0);
		}
		c.close();
		db.close();
		return ret;
	}
	
	void updateOrInsertLastRead(String item, long time) {
		ContentValues cv = new ContentValues();
		cv.put("item", item);
		cv.put("time", time);
		
		SQLiteDatabase db = tdHelper.getWritableDatabase();
		int updated = db.update("lastRead", cv, "item = ?", new String[] {item});
		if (updated==0) {
			// row not yet present
			db.insert("lastRead", null, cv);
		}
		db.close();
	}
	
	Map<String, Integer> getLists() {
		SQLiteDatabase db = tdHelper.getReadableDatabase();
		Map<String,Integer> ret = new HashMap<String,Integer>();
		Cursor c = db.query("lists", new String[] {"name","id"}, null, null, null, null, "name");
		if (c.getCount()>0){
			c.moveToFirst();
			do {
				String name = c.getString(0);
				Integer id = c.getInt(1);
				ret.put(name, id);
			} while (c.moveToNext());
		}
		c.close();
		db.close();
		return ret;
	}

	public void addList(String name, int id) {
		ContentValues cv = new ContentValues();
		cv.put("name", name);
		cv.put("id",id);
		
		SQLiteDatabase db = tdHelper.getWritableDatabase();
		db.insert("lists", null, cv);
		db.close();
		
	}

	public void removeList(Integer id) {
		SQLiteDatabase db = tdHelper.getWritableDatabase();
		db.delete("lists", "id = ?", new String[] {id.toString()});
		db.close();
	}
}
