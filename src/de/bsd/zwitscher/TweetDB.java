package de.bsd.zwitscher;

import java.lang.String;
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
import com.sun.org.apache.xpath.internal.operations.*;
import twitter4j.User;

/**
 * This class is interfacing with the SQLite3 database on the
 * handset to store statuses, lists, users and so on.
 *
 * @author Heiko W. Rupp
 */
public class TweetDB {

    private static final String TABLE_STATUSES = "statuses";
    private static final String TABLE_LAST_READ = "lastRead";
    private static final String TABLE_LISTS = "lists";
    private static final String TABLE_USERS = "users";
    private TweetDBOpenHelper tdHelper;

	public TweetDB(Context context) {
		tdHelper = new TweetDBOpenHelper(context, "TWEET_DB", null, 2);
	}


    private class TweetDBOpenHelper extends SQLiteOpenHelper {

		public TweetDBOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE "+ TABLE_STATUSES + " (" +
                    "ID LONG, " +
                    "LIST_ID LONG, " +
                    "I_REP_TO LONG, " +
                    "STATUS BLOB " +
                    ")"
            );

			db.execSQL("CREATE TABLE "+ TABLE_LAST_READ + " (" + //
					"list_id LONG, " + //
					"last_read_id LONG )" //
			);
			db.execSQL("CREATE TABLE " + TABLE_LISTS + " (" + //
					"name TEXT, " + //
					"id LONG )"
			);

            db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                    "userId LONG, " + //
                    "accountId LONG, " + //
                    "user_json STRING )"
            );
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (newVersion==2)
                db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                        "userId LONG, " + //
                        "accountId LONG, " + //
                        "user_json STRING )"
                );

		}

	}

	long getLastRead(int list_id) {
		SQLiteDatabase db = tdHelper.getReadableDatabase();
		Cursor c = db.query(TABLE_LAST_READ, new String[] {"last_read_id"}, "list_id = ?", new String[] {String.valueOf(list_id)}, null, null, null);
		Long ret;
		if (c.getCount()==0)
			ret = -1L;
		else {
			c.moveToFirst();
			ret = c.getLong(0);
		}
		c.close();
		db.close();
		return ret;
	}

	void updateOrInsertLastRead(int list_id, long last_read_id) {
		ContentValues cv = new ContentValues();
		cv.put("list_id", list_id);
		cv.put("last_read_id", last_read_id);

		SQLiteDatabase db = tdHelper.getWritableDatabase();
		int updated = db.update(TABLE_LAST_READ, cv, "list_id = ?", new String[] {String.valueOf(list_id)});
		if (updated==0) {
			// row not yet present
			db.insert(TABLE_LAST_READ, null, cv);
		}
		db.close();
	}

	Map<String, Integer> getLists() {
		SQLiteDatabase db = tdHelper.getReadableDatabase();
		Map<String,Integer> ret = new HashMap<String,Integer>();
		Cursor c = db.query(TABLE_LISTS, new String[] {"name","id"}, null, null, null, null, "name");
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
		db.insert(TABLE_LISTS, null, cv);
		db.close();

	}

	public void removeList(Integer id) {
		SQLiteDatabase db = tdHelper.getWritableDatabase();
		db.delete(TABLE_LISTS, "id = ?", new String[]{id.toString()});
		db.close();
	}

    public void storeStatus(long id, long i_reply_id, long list_id, byte[] statusObj) {
        ContentValues cv = new ContentValues(4);
        cv.put("ID", id);
        cv.put("I_REP_TO", i_reply_id);
        cv.put("LIST_ID", list_id);
        cv.put("STATUS",statusObj);

        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.insert(TABLE_STATUSES, null, cv);
        db.close();
    }

    public void updateStatus(long id, byte[] statusObj) {
        ContentValues cv = new ContentValues(1);
        cv.put("STATUS",statusObj);

        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.update(TABLE_STATUSES,cv,"id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    /**
     * Return the blob of one stored status by its (unique) id.
     * @param statusId The id of the status
     * @return The blob if the status exists in the DB or null otherwise
     */
    public byte[] getStatusObjectById(long statusId) {

        SQLiteDatabase db = tdHelper.getReadableDatabase();
        byte[] ret = null;

        Cursor c;
        c= db.query(TABLE_STATUSES,new String[]{"STATUS"},"id = ?",new String[]{String.valueOf(statusId)},null,null,null);
        if (c.getCount()>0){
            c.moveToFirst();
            ret = c.getBlob(0);
        }
        c.close();
        db.close();

        return ret;
    }

    public List<byte[]> getReplies(long inRepyId) {
        SQLiteDatabase db = tdHelper.getReadableDatabase();

        List<byte[]> ret = new ArrayList<byte[]>();

        Cursor c ;
        c = db.query(TABLE_STATUSES,new String[]{"STATUS"},"i_rep_to = ?",new String[]{String.valueOf(inRepyId)},null,null,"ID DESC");
        if (c.getCount()>0) {
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

    public List<Long> getStatusesIdsOlderThan(long sinceId, int number, long list_id) {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        List<Long> ret = new ArrayList<Long>(number);

        Cursor c;
        if (sinceId>-1)
            c = db.query(TABLE_STATUSES,new String[]{"ID"},"id < ? AND list_id = ?",new String[]{String.valueOf(sinceId),String.valueOf(list_id)},null,null,"ID DESC",String.valueOf(number));
        else
            c = db.query(TABLE_STATUSES,new String[]{"ID"},"list_id = ?",new String[]{String.valueOf(list_id)},null,null,"ID DESC",String.valueOf(number));

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

    public List<byte[]> getStatusesObjsOlderThan(long sinceId, int howMany, long list_id) {
        List<byte[]> ret = new ArrayList<byte[]>();
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        Cursor c;
        if (sinceId>-1)
            c = db.query(TABLE_STATUSES,new String[]{"STATUS"},"id < ? AND list_id = ?",new String[]{String.valueOf(sinceId),String.valueOf(list_id)},null,null,"ID DESC",String.valueOf(howMany));
        else
            c = db.query(TABLE_STATUSES,new String[]{"STATUS"},"list_id = ?",new String[]{String.valueOf(list_id)},null,null,"ID DESC",String.valueOf(howMany));

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
        db.execSQL("DELETE FROM " + TABLE_STATUSES);
        db.close();
    }

    /**
     * Returns a user by its ID from the database if it exists or null.
     * @param userId Id of the user
     * @param accountId Id of the account to use
     * @return Basic JSON string of the user info or null.
     */
    public String getUserById(int userId, int accountId) {

        SQLiteDatabase db = tdHelper.getReadableDatabase();
        String ret = null;

        Cursor c;
        c = db.query(TABLE_USERS,new String[]{"user_json"},"userId = ? AND accountId = ?",new String[] { String.valueOf(userId), String.valueOf(accountId)},null, null, null);
        if (c.getCount()>0) {
            c.moveToFirst();
            ret = c.getString(0);
        }

        return ret;
    }

    /**
     * Insert a user into the database.
     * @param userId The Id of the user to insert
     * @param accountId The id of the account to use
     * @param json JSON representation of the User object
     */
    public void insertUser(int userId, int accountId, String json) {
        ContentValues cv = new ContentValues(3);
        cv.put("userId",userId);
        cv.put("accountId",accountId);
        cv.put("user_json",json);

        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.insert(TABLE_USERS,null,cv);
        db.close();
    }

    /**
     * Update an existing user in the database.
     * @param userId
     * @param accountId
     * @param json
     */
    public void updateUser(int userId, int accountId, String json) {
        ContentValues cv = new ContentValues(1);
        cv.put("user_json",json);

        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.update(TABLE_USERS,cv,"userId = ? AND accountId = ?",new String[] { String.valueOf(userId), String.valueOf(accountId)});
        db.close();
    }

}
