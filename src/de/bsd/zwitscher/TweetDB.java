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

/**
 * This class is interfacing with the SQLite3 database on the
 * handset to store statuses, lists, users and so on.
 *
 * @author Heiko W. Rupp
 */
public class TweetDB {

    public static final String TABLE_STATUSES = "statuses";
    private static final String TABLE_LAST_READ = "lastRead";
    private static final String TABLE_LISTS = "lists";
    private static final String TABLE_USERS = "users";
    private static final String TABLE_SEARCHES = "searches";
    public static final String TABLE_DIRECTS = "directs";
    static final String STATUS = "STATUS";
    static final String ACCOUNT_ID = "ACCOUNT_ID";
    static final String ACCOUNT_ID_IS = ACCOUNT_ID + "=?";
    private TweetDBOpenHelper tdHelper;
    private final String account;

	public TweetDB(Context context, int accountId) {
		tdHelper = new TweetDBOpenHelper(context, "TWEET_DB", null, 3);
        account = String.valueOf(accountId);

	}


    private static class TweetDBOpenHelper extends SQLiteOpenHelper {
        static final String CREATE_TABLE = "CREATE TABLE ";

		public TweetDBOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE + TABLE_STATUSES + " (" +
                    "ID LONG, " +
                    ACCOUNT_ID + " LONG, " +
                    "LIST_ID LONG, " +
                    "I_REP_TO LONG, " +
                    "STATUS STRING, " +
                    "UNIQUE (ID, LIST_ID, " + ACCOUNT_ID + ")" +
                    ")"

            );

            db.execSQL(CREATE_TABLE + TABLE_DIRECTS + " (" +
                    "ID LONG, " +
                    "created_at LONG, " +
                    ACCOUNT_ID + " LONG, " +
                    "MESSAGE_JSON STRING, " +
                    "UNIQUE (ID, " + ACCOUNT_ID + ")" +
                    ")"
            );

			db.execSQL(CREATE_TABLE + TABLE_LAST_READ + " (" + //
					"list_id LONG, " + //
					"last_read_id LONG, " +  // Last Id read by the user
                    "last_fetched_id LONG, " +  // last Id fetched from the server
                    ACCOUNT_ID + " LONG, " +
                    "UNIQUE (LIST_ID, " + ACCOUNT_ID + ")" +
                    ")"
			);
			db.execSQL(CREATE_TABLE + TABLE_LISTS + " (" + //
					"name TEXT, " + //
					"id LONG, " +
                    ACCOUNT_ID + " LONG, " +
                    "list_json TEXT, " +
                    "UNIQUE (ID, " + ACCOUNT_ID + ")" +
                    " )"
			);

            db.execSQL(CREATE_TABLE + TABLE_USERS + " (" +
                    "userId LONG, " + //
                    ACCOUNT_ID + " LONG, " +
                    "user_json STRING ," +
                    "screenname STRING, " +
                    "UNIQUE (USERID, " + ACCOUNT_ID + ")" +
                ")"
            );
            db.execSQL(CREATE_TABLE + TABLE_SEARCHES + " ("+
                    "name STRING, "+
                    "id LONG, " +
                    ACCOUNT_ID + " LONG, " +
                    "query STRING, " +
                    "json STRING,"+
                    "UNIQUE (ID, " + ACCOUNT_ID + ")" +
                ")"
            );
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion==1) {
                db.execSQL("DELETE FROM " + TABLE_USERS);
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN screenname STRING");
            }
            if (oldVersion<3) {
                db.execSQL("CREATE UNIQUE INDEX STATUS_IDX ON " + TABLE_STATUSES + "(ID, LIST_ID, " + ACCOUNT_ID +")");
                db.execSQL("CREATE UNIQUE INDEX STATUS_IDX ON " + TABLE_DIRECTS + "(ID, " + ACCOUNT_ID +")");
                db.execSQL("CREATE UNIQUE INDEX STATUS_IDX ON " + TABLE_LAST_READ + "(list_ID, " + ACCOUNT_ID +")");
                db.execSQL("CREATE UNIQUE INDEX STATUS_IDX ON " + TABLE_LISTS + "(ID, " + ACCOUNT_ID +")");
                db.execSQL("CREATE UNIQUE INDEX STATUS_IDX ON " + TABLE_USERS + "(userID, " + ACCOUNT_ID +")");
                db.execSQL("CREATE UNIQUE INDEX STATUS_IDX ON " + TABLE_SEARCHES + "(ID, " + ACCOUNT_ID +")");
            }

		}

	}

    /**
     * Return the id of the status that was last read
     * @param list_id id of the list
     * @return
     */
	long getLastRead(int list_id) {
		SQLiteDatabase db = tdHelper.getReadableDatabase();
		Cursor c = db.query(TABLE_LAST_READ, new String[] {"last_read_id"}, "list_id = ? AND " + ACCOUNT_ID_IS, new String[] {String.valueOf(list_id),account}, null, null, null);
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

    /**
     * Update (or initially store) the last read information of the passed list
     * @param list_id List to mark as read
     * @param last_read_id Id of the last read status
     */
	void updateOrInsertLastRead(int list_id, long last_read_id) {
		ContentValues cv = new ContentValues();
		cv.put("list_id", list_id);
		cv.put("last_read_id", last_read_id);
        cv.put(ACCOUNT_ID,account);

		SQLiteDatabase db = tdHelper.getWritableDatabase();
		int updated = db.update(TABLE_LAST_READ, cv, "list_id = ? AND " + ACCOUNT_ID_IS, new String[] {String.valueOf(list_id),account});
		if (updated==0) {
			// row not yet present
			db.insert(TABLE_LAST_READ, null, cv);
		}
		db.close();
	}


    /**
     * Return Infos about all lists in the DB
     * @return
     * @todo return the json object
     */
	Map<String, Integer> getLists() {
		SQLiteDatabase db = tdHelper.getReadableDatabase();
		Map<String,Integer> ret = new HashMap<String,Integer>();
		Cursor c = db.query(TABLE_LISTS, new String[] {"name","id"}, ACCOUNT_ID_IS, new String[]{account}, null, null, "name");
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

    /**
     * Add a new list to the database
     * @param name Name of the lise
     * @param id Id of the list
     * @param json Full json string object of the list
     */
	public void addList(String name, int id, String json) {
		ContentValues cv = new ContentValues();
		cv.put("name", name);
		cv.put("id",id);
        cv.put(ACCOUNT_ID,account);
        cv.put("list_json", json);

		SQLiteDatabase db = tdHelper.getWritableDatabase();
		db.insert(TABLE_LISTS, null, cv);
		db.close();

	}

    /**
     * Delete the list with the passed ID in the DB
     * @param id Id of the list to delete
     * @todo Also remove statuses for the passed list
     */
	public void removeList(Integer id) {
		SQLiteDatabase db = tdHelper.getWritableDatabase();
		db.delete(TABLE_LISTS, "id = ? AND " + ACCOUNT_ID_IS, new String[]{id.toString(), account});
		db.close();
	}


    /**
     * Update the stored TwitterResponse object. This may be necessary when e.g. the
     * favorite status has been changed on it.
     * @param id Id of the object
     * @param status_json Json representation of it.
     */
    public void updateStatus(long id, String status_json) {
        ContentValues cv = new ContentValues(1);
        cv.put(STATUS, status_json);
        cv.put(ACCOUNT_ID,account);
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.update(TABLE_STATUSES, cv, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    /**
     * Return the blob of one stored status by its id and list_id.
     * A status with the same id can occur multiple times with various
     * listIds.
     *
     * @param statusId The id of the status
     * @param listId The id of the list this status appears
     * @return The json_string if the status exists in the DB or null otherwise
     */
    public String getStatusObjectById(long statusId, Long listId) {

        SQLiteDatabase db = tdHelper.getReadableDatabase();
        String ret = null;
        Cursor c;
        String statusIdS = String.valueOf(statusId);
        if (listId!=null) {
            c= db.query(TABLE_STATUSES, // Table
                    new String[]{STATUS}, // returned column
                    "id = ? AND list_id = ? AND " + ACCOUNT_ID_IS, // selection
                    new String[]{statusIdS,String.valueOf(listId),account}, // selection param
                    null, // groupBy
                    null, // having
                    null // order by
            );
        }
        else { // We don't care here - just take one if present
            c= db.query(TABLE_STATUSES, // Table
                    new String[]{STATUS}, // returned column
                    "id = ? AND " + ACCOUNT_ID_IS, // selection
                    new String[]{statusIdS,account}, // selection param
                    null, // groupBy
                    null, // having
                    null // order by
            );

        }
        if (c.getCount()>0){
            c.moveToFirst();
            ret = c.getString(0);
        }
        c.close();
        db.close();

        return ret;
    }

    /**
     * Get all statuses that are marked as a reply to the passed one.
     * @param inRepyId Id of the original status
     * @return  List of Json_objects that represent the replies
     */
    public List<String> getReplies(long inRepyId) {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        List<String> ret = new ArrayList<String>();

        Cursor c ;
        c = db.query(TABLE_STATUSES,new String[]{STATUS}, "i_rep_to = ? & " + ACCOUNT_ID_IS
                ,new String[]{String.valueOf(inRepyId),account},null,null,"ID DESC");
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                String json = c.getString(0);
                ret.add(json);
            } while (c.moveToNext());
        }
        c.close();
        db.close();

        return ret;
    }


    /**
     * Return a list of Responses along for the passed list id.
     * @param sinceId What is the oldest status to look after
     * @param howMany How many entries shall be returned
     * @param list_id From which list?
     * @return List of JResponse objects
     */
    public List<String> getStatusesObjsOlderThan(long sinceId, int howMany, long list_id) {
        List<String> ret = new ArrayList<String>();
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        Cursor c;
        String listIdS = String.valueOf(list_id);
        if (sinceId>-1)
            c = db.query(TABLE_STATUSES, // Table
                    new String[]{STATUS}, // Columns returned
                    "id < ? AND list_id = ? AND " +ACCOUNT_ID_IS, // selection
                    new String[]{String.valueOf(sinceId), listIdS,account}, // selection values
                    null, // group by
                    null, // having
                    "ID DESC", // order by
                    String.valueOf(howMany) // limit
            );
        else // since id = -1 -> just get the n newest
            c = db.query(TABLE_STATUSES, // Table
                    new String[]{STATUS}, // Columns returned
                    "list_id = ? AND " + ACCOUNT_ID_IS, // selection
                    new String[]{listIdS,account},  // selection values
                    null, // group by
                    null, // having
                    "ID DESC", // order by
                    String.valueOf(howMany) // limit
            );

        if (c.getCount()>0){
            c.moveToFirst();
            do {
                String json = c.getString(0);
                ret.add(json);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return ret;
    }

    public List<String> getDirectsOlderThan(long sinceId, int howMany) {
        List<String> ret = new ArrayList<String>();
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        Cursor c;
        if (sinceId>-1)
            c = db.query(TABLE_DIRECTS,new String[]{"MESSAGE_JSON"},"id < ? AND " +ACCOUNT_ID_IS,new String[]{String.valueOf(sinceId),account},null,null,"CREATED_AT DESC",String.valueOf(howMany));
        else
            c = db.query(TABLE_DIRECTS,new String[]{"MESSAGE_JSON"},  ACCOUNT_ID_IS,new String[]{account},null,null,"CREATED_AT DESC",String.valueOf(howMany));

        if (c.getCount()>0){
            c.moveToFirst();
            do {
                String json = c.getString(0);
                ret.add(json);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return ret;

    }


    /**
     * Purge the last read table.
     */
    public void resetLastRead() {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_LAST_READ);
        db.close();
    }

    /**
     * Purge the statuses table.
     */
    public void cleanTweets() {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_STATUSES);
        db.execSQL("DELETE FROM " + TABLE_DIRECTS);
        db.execSQL("DELETE FROM " + TABLE_USERS);
        db.execSQL("DELETE FROM " + TABLE_LAST_READ);
        db.close();
    }

    /**
     * Returns a user by its ID from the database if it exists or null.
     *
     * @param userId Id of the user
     * @return Basic JSON string of the user info or null.
     */
    public String getUserById(int userId) {

        SQLiteDatabase db = tdHelper.getReadableDatabase();
        String ret = null;

        Cursor c;
        c = db.query(TABLE_USERS,new String[]{"user_json"},"userId = ? AND  " + ACCOUNT_ID_IS,new String[] { String.valueOf(userId), account},null, null, null);
        if (c.getCount()>0) {
            c.moveToFirst();
            ret = c.getString(0);
        }
        c.close();
        db.close();
        return ret;
    }

    /**
     * Returns a user by its screenname from the database if it exists or null.
     *
     * @param screenName screenname of the user
     * @return Basic JSON string of the user info or null.
     */
    public String getUserByName(String screenName) {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        String ret = null;

        Cursor c;
        c = db.query(TABLE_USERS,new String[]{"user_json"},"screenname = ? AND  " + ACCOUNT_ID_IS ,new String[] { screenName, account},null, null, null);
        if (c.getCount()>0) {
            c.moveToFirst();
            ret = c.getString(0);
        }
        c.close();
        db.close();
        return ret;
    }


    /**
     * Return a list of all users stored
     * @return
     */
    public List<String> getUsers() {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        List<String> ret = new ArrayList<String>();

        Cursor c;
        c = db.query(TABLE_USERS,new String[]{"user_json"}, ACCOUNT_ID_IS ,new String[] { String.valueOf(account)},null, null, null);
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                String json = c.getString(0);
                ret.add(json);
            } while (c.moveToNext());
        }
        c.close();
        db.close();

        return ret;
    }


    /**
     * Insert a user into the database.
     * @param userId The Id of the user to insert
     * @param json JSON representation of the User object
     * @param screenName
     */
    public void insertUser(int userId, String json, String screenName) {
        ContentValues cv = new ContentValues(4);
        cv.put("userId",userId);
        cv.put(ACCOUNT_ID,account);
        cv.put("user_json",json);
        cv.put("screenname",screenName);

        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.insertWithOnConflict(TABLE_USERS, null, cv,SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    /**
     * Update an existing user in the database.
     * @param userId
     * @param json
     * @todo Still needed with conflict resolution on insert?
     */
    public void updateUser(int userId, String json) {
        ContentValues cv = new ContentValues(1);
        cv.put("user_json",json);

        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.update(TABLE_USERS, cv, "userId = ? AND " + ACCOUNT_ID + " = ?",
                new String[]{String.valueOf(userId), account});
        db.close();
    }


    /**
     * Insert Lists of ContentValues into the DB table <i>table</i>.
     * The insert uses the v8 method insertWithOnConflict() with a parameter
     * of CONFLICT_IGNORE meaning, that inserts that violate the (uniqueness)
     * constraints are just ignored and do not cause a rollback. This is ok, as
     * this method is called on new inserts of data received from the server.
     * @param table Table to insert into
     * @param values ContentValues that describe the content
     */
    public void storeValues(String table, List<ContentValues> values) {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        for (ContentValues val : values) {
            db.insertWithOnConflict(table,null,val,SQLiteDatabase.CONFLICT_IGNORE);
        }
        db.close();
    }

    /**
     * Get a direct message from th DB
     *
     * @param id ID of the message to look up
     * @return JSON string of the message or null if not found
     */
    public String getDirectById(long id) {

        SQLiteDatabase db = tdHelper.getReadableDatabase();
        String ret = null;

        Cursor c;
        c = db.query(TABLE_DIRECTS,new String[]{"message_json"},"id = ? AND " + ACCOUNT_ID_IS,new String[] { String.valueOf(id), account},null, null, null);
        if (c.getCount()>0) {
            c.moveToFirst();
            ret = c.getString(0);
        }
        return ret;
    }

    /**
     * Get the last <i>number</i> direct messages from the DB
     * @param number Numer of messages to get
     * @return List of messages or empyt list
     */
    public List<String> getDirects(int number) {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        List<String > ret = new ArrayList<String>();

        Cursor c;
        c = db.query(TABLE_DIRECTS,new String[]{"message_json"},ACCOUNT_ID_IS,new String[] { account},null, null, "ID DESC",String.valueOf(number));
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                String json = c.getString(0);
                ret.add(json);
            } while ((c.moveToNext()));
        }

        c.close();
        db.close();

        return ret;
    }


    public void storeSavedSearch(String name, String query, int id, String json) {
        ContentValues cv = new ContentValues(5);
        cv.put("id",id);
        cv.put("name", name);
        cv.put(ACCOUNT_ID,account);
        cv.put("query",query);
        cv.put("json",json);

        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.insert(TABLE_SEARCHES,null,cv);
        db.close();

    }

    public List<String> getSavedSearches() {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        List<String > ret = new ArrayList<String>();

        Cursor c;
        c = db.query(TABLE_SEARCHES,new String[]{"json"},ACCOUNT_ID_IS,new String[] { account},null, null, "ID DESC");
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                String json = c.getString(0);
                ret.add(json);
            } while ((c.moveToNext()));
        }
        c.close();
        db.close();

        return ret;
    }

    public void deleteSearch(int id) {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.delete(TABLE_SEARCHES,ACCOUNT_ID_IS + " AND id = ?",new String[]{account,String.valueOf(id)});
        db.close();
    }


}
