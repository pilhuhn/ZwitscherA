package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import twitter4j.Status;

public class TweetDB {

    private static final String STATUSES = "statuses";
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
            db.execSQL("CREATE TABLE "+ STATUSES + " (" +
                    "ID LONG, " +
                    "LIST_ID LONG, " +
                    "I_REP_TO LONG, " +
                    "STATUS BLOB " +
                    ")"
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

    public void storeStatus(long id, long i_reply_id, long list_id, byte[] statusObj) {
        ContentValues cv = new ContentValues(3);
        cv.put("ID",id);
        cv.put("I_REP_TO",i_reply_id);
        cv.put("LIST_ID", list_id);
        cv.put("STATUS",statusObj);

        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.insert(STATUSES, null, cv);
        db.close();
    }

    public byte[] getStatusObjectById(long statusId,Long listId) {

        SQLiteDatabase db = tdHelper.getReadableDatabase();
        byte[] ret = null;

        Cursor c;
        if (listId==null)
            c= db.query(STATUSES,new String[]{"STATUS"},"id = ?",new String[]{String.valueOf(statusId)},null,null,null);
        else
            c= db.query(STATUSES,new String[]{"STATUS"},"id = ? AND list_id = ?",
                       new String[]{String.valueOf(statusId),listId.toString()},null,null,null);
        if (c.getCount()>0){
            c.moveToFirst();
            ret = c.getBlob(0);
        }
        c.close();
        db.close();

        return ret;
    }

    public List<Long> getStatusesIdsOlderThan(long sinceId, int number, long list_id) {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        List<Long> ret = new ArrayList<Long>(number);

        Cursor c;
        if (sinceId>-1)
            c = db.query(STATUSES,new String[]{"ID"},"id < ? AND list_id = ?",new String[]{String.valueOf(sinceId),String.valueOf(list_id)},null,null,"ID DESC",String.valueOf(number));
        else
            c = db.query(STATUSES,new String[]{"ID"},"list_id = ?",new String[]{String.valueOf(list_id)},null,null,"ID DESC",String.valueOf(number));

        if (c.getCount()>0){
            c.moveToFirst();
            do {
                Long id = c.getLong(0);
                ret.add(id);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return ret;
    }

    public List<byte[]> getStatusesObjsOlderThan(long sinceId, int number, long list_id) {
        List<byte[]> ret = new ArrayList<byte[]>();
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        Cursor c;
        if (sinceId>-1)
            c = db.query(STATUSES,new String[]{"STATUS"},"id < ? AND list_id = ?",new String[]{String.valueOf(sinceId),String.valueOf(list_id)},null,null,"ID DESC",String.valueOf(number));
        else
            c = db.query(STATUSES,new String[]{"STATUS"},"list_id = ?",new String[]{String.valueOf(list_id)},null,null,"ID DESC",String.valueOf(number));

        if (c.getCount()>0){
            c.moveToFirst();
            do {
                byte[] bytes = c.getBlob(0);
                ret.add(bytes);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return ret;


    }


    public void resetLastRead() {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.execSQL("DELETE FROM lastRead");
        db.close();
    }

    public void cleanTweets() {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.execSQL("DELETE FROM " + STATUSES);
        db.close();
    }

}
