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
import android.util.Log;
import android.util.Pair;
import de.bsd.zwitscher.account.Account;

/**
 * This class is interfacing with the SQLite3 database on the
 * handset to store statuses, lists, users and so on.
 *
 * @author Heiko W. Rupp
 */
public class TweetDB {

    private static final String TABLE_ACCOUNTS = "accounts";
    public static final String TABLE_STATUSES = "statuses";
    private static final String TABLE_LAST_READ = "lastRead";
    private static final String TABLE_LISTS = "lists";
    private static final String TABLE_USERS = "users";
    private static final String TABLE_SEARCHES = "searches";
    public static final String TABLE_DIRECTS = "directs";
    public static final String TABLE_URLS = "urls";
    public static final String TABLE_UPDATES = "updates";
    private static final String[] DATA_TABLES = {TABLE_STATUSES,
        TABLE_LAST_READ,TABLE_LISTS,TABLE_USERS,TABLE_SEARCHES,TABLE_DIRECTS};
    static final String STATUS = "STATUS";
    static final String ACCOUNT_ID = "ACCOUNT_ID";
    static final String ACCOUNT_ID_IS = ACCOUNT_ID + "=?";
    private TweetDBOpenHelper tdHelper;
    private final String account;

	public TweetDB(Context context, int accountId) {
		tdHelper = new TweetDBOpenHelper(context, "TWEET_DB", null, 7);
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
                    "ctime LONG,  " +
                    "UNIQUE (ID, LIST_ID, " + ACCOUNT_ID + ")" +
                    ")"

            );
            db.execSQL("CREATE INDEX STATUS_CTIME_IDX ON " + TABLE_STATUSES + "(ctime)");
            db.execSQL("CREATE UNIQUE INDEX STATUS_IDX ON " + TABLE_STATUSES + "(ID, LIST_ID, " + ACCOUNT_ID +")");

            db.execSQL(CREATE_TABLE + TABLE_DIRECTS + " (" +
                    "ID LONG, " +
                    "created_at LONG, " +
                    ACCOUNT_ID + " LONG, " +
                    "MESSAGE_JSON STRING, " +
                    "UNIQUE (ID, " + ACCOUNT_ID + ")" +
                    ")"
            );
            db.execSQL("CREATE UNIQUE INDEX DIRECTS_ID_IDX ON " + TABLE_DIRECTS + "(ID, " + ACCOUNT_ID +")");

			db.execSQL(CREATE_TABLE + TABLE_LAST_READ + " (" + //
					"list_id LONG, " + //
					"last_read_id LONG, " +  // Last Id read by the user
                    "last_fetched_id LONG, " +  // last Id fetched from the server
                    ACCOUNT_ID + " LONG, " +
                    "UNIQUE (LIST_ID, " + ACCOUNT_ID + ")" +
                    ")"
			);
            db.execSQL("CREATE UNIQUE INDEX LAST_R_ID_IDX ON " + TABLE_LAST_READ + "(list_ID, " + ACCOUNT_ID +")");

			db.execSQL(CREATE_TABLE + TABLE_LISTS + " (" + //
					"name TEXT, " + //
					"id LONG, " +
                    ACCOUNT_ID + " LONG, " +
                    "list_json TEXT, " +
                    "UNIQUE (ID, " + ACCOUNT_ID + ")" +
                    " )"
			);
            db.execSQL("CREATE UNIQUE INDEX LISTS_ID_IDX ON " + TABLE_LISTS + "(ID, " + ACCOUNT_ID +")");

            db.execSQL(CREATE_TABLE + TABLE_USERS + " (" +
                    "userId LONG, " + //
                    ACCOUNT_ID + " LONG, " +
                    "user_json STRING ," +
                    "screenname STRING, " +
                    "last_modified LONG, " +
                    "UNIQUE (USERID, " + ACCOUNT_ID + ")" +
                ")"
            );
            db.execSQL("CREATE INDEX L_M_IDX ON " + TABLE_USERS + " (last_modified)");
            db.execSQL("CREATE UNIQUE INDEX USERS_ID_IDX ON " + TABLE_USERS + "(userID, " + ACCOUNT_ID +")");

            db.execSQL(CREATE_TABLE + TABLE_ACCOUNTS + " (" +
                    "id INTEGER, " + // 0
                    "name TEXT, " + // 1
                    "tokenKey TEXT, "+ // 2
                    "tokenSecret TEXT, "+ // 3
                    "serverUrl TEXT, " + // 4
                    "serverType TEXT, " + // 5
                    "isDefault INTEGER, " + // 6
                    "password TEXT, " + // 7
                    "UNIQUE (id)" + //
                    "UNIQUE (name, serverUrl ) " +// TODO add index in default
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
            db.execSQL("CREATE UNIQUE INDEX SEARCH_ID_IDX ON " + TABLE_SEARCHES + "(ID, " + ACCOUNT_ID +")");

            db.execSQL(CREATE_TABLE + TABLE_URLS + " ("+
                    "src TEXT, " +
                    "target TEXT, " +
                    "last_modified LONG " +
                    ")"
            );
            db.execSQL("CREATE UNIQUE INDEX URL_SRC_IDX ON " + TABLE_URLS + "(src)");

            db.execSQL(CREATE_TABLE + TABLE_UPDATES + " (" +
                    "id INTEGER PRIMARY KEY, " +
                    ACCOUNT_ID + " LONG, " +
                    "content BLOB " +
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
                db.execSQL("CREATE UNIQUE INDEX DIRECTS_ID_IDX ON " + TABLE_DIRECTS + "(ID, " + ACCOUNT_ID +")");
                db.execSQL("CREATE UNIQUE INDEX LAST_R_ID_IDX ON " + TABLE_LAST_READ + "(list_ID, " + ACCOUNT_ID +")");
                db.execSQL("CREATE UNIQUE INDEX LISTS_ID_IDX ON " + TABLE_LISTS + "(ID, " + ACCOUNT_ID +")");
                db.execSQL("CREATE UNIQUE INDEX USERS_ID_IDX ON " + TABLE_USERS + "(userID, " + ACCOUNT_ID +")");
                db.execSQL("CREATE UNIQUE INDEX SEARCH_ID_IDX ON " + TABLE_SEARCHES + "(ID, " + ACCOUNT_ID +")");
            }
            if (oldVersion<4) {
                db.execSQL(CREATE_TABLE + TABLE_ACCOUNTS + " (" +
                        "id INTEGER, " + // 0
                        "name TEXT, " + // 1
                        "tokenKey TEXT, "+ // 2
                        "tokenSecret TEXT, "+ // 3
                        "serverUrl TEXT, " + // 4
                        "serverType TEXT, " + // 5
                        "isDefault INTEGER, " + // 6
                        "password TEXT, " + // 7
                        "UNIQUE (id)" + //
                        "UNIQUE (name, serverUrl ) " +// TODO add index in default
                    ")"
                );
                db.execSQL("ALTER TABLE " + TABLE_STATUSES + " ADD COLUMN ctime LONG");
            }
            if (oldVersion<5) {
                db.execSQL("CREATE INDEX STATUS_CTIME_IDX ON " + TABLE_STATUSES + "(ctime)");
            }
            if (oldVersion<6) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN last_modified LONG DEFAULT 0");
                db.execSQL("CREATE INDEX L_M_IDX ON " + TABLE_USERS + " (last_modified)");
            }
            if (oldVersion<7) {
                db.execSQL(CREATE_TABLE + TABLE_URLS + " ("+
                        "src TEXT, " +
                        "target TEXT, " +
                        "last_modified LONG " +
                        ")"
                );
                db.execSQL("CREATE UNIQUE INDEX URL_SRC_IDX ON " + TABLE_URLS + "(src)");

                db.execSQL(CREATE_TABLE + TABLE_UPDATES + " (" +
                        "_id INTEGER PRIMARY KEY, " + // PK -> auto increment when pk==null
                        ACCOUNT_ID + " LONG, " +
                        "content BLOB " +
                        ")"
                );
            }
		}

	}

    /**
     * Return the id of the status that was last read
     * @param list_id id of the list
     * @return id of the status that was last read
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

        try {
            SQLiteDatabase db;
            db= tdHelper.getWritableDatabase();
            int updated = db.update(TABLE_LAST_READ, cv, "list_id = ? AND " + ACCOUNT_ID_IS, new String[] {String.valueOf(list_id),account});
            if (updated==0) {
                // row not yet present
                db.insert(TABLE_LAST_READ, null, cv);
            }
            db.close();
        } catch (Exception e) {
            // Situation is not too bad, as it just means more network traffic next time // TODO find better solution
            e.printStackTrace();
        }
    }


    /**
     * Return Infos about all lists in the DB
     * @return Map with Listname,id pairs
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
        ContentValues cv = new ContentValues(2);
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

    public List<String> searchStatuses(String query) {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        List<String> ret = new ArrayList<String>();

        Cursor c ;
        c = db.query(TABLE_STATUSES,new String[]{STATUS}, "status LIKE '%" + query + "%' AND " + ACCOUNT_ID_IS
                ,new String[]{account},null,null,"ID DESC","100"); // only 100 results -> may get filtered down later
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
    public void cleanTweetDB() {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_STATUSES);
        db.execSQL("DELETE FROM " + TABLE_DIRECTS);
        db.execSQL("DELETE FROM " + TABLE_USERS);
        db.execSQL("DELETE FROM " + TABLE_LAST_READ);
        db.execSQL("DELETE FROM " + TABLE_URLS);
        db.close();
    }

    public void cleanStatusesAndUsers(long cutOff) {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_STATUSES + " WHERE ctime < " + cutOff);
        db.execSQL("DELETE FROM " + TABLE_USERS + " WHERE last_modified < " + cutOff);
        db.execSQL("DELETE FROM " + TABLE_URLS + " WHERE last_modified < " + cutOff);
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
     * @return list fo json objects in string representation
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
     * @param screenName screenname of that user
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

    public Account getAccount(String name,String type) {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        Cursor c;
        Account account=null;
        c = db.query(TABLE_ACCOUNTS,null,"name = ? AND serverType = ?", new String[]{name,type},null,null,null);
        if (c.getCount()>0) {
            c.moveToFirst();
            boolean isDefault = c.getInt(6) == 1;
            account = new Account(
                    c.getInt(0), // id
                    name, // name
                    c.getString(2), // token key
                    c.getString(3), // token secret
                    c.getString(4), // url
                    type, // type /5)
                    isDefault // 6
            );
            account.setPassword(c.getString(7));
        }
        c.close();
        db.close();
        return account;
    }

    public Account getDefaultAccount(){
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        Cursor c;
        Account account=null;
        c = db.query(TABLE_ACCOUNTS,null,"isDefault=1", null,null,null,null);
        if (c.getCount()>0) {
            c.moveToFirst();
            boolean isDefault = c.getInt(6) == 1;
            account = new Account(
                    c.getInt(0), // id
                    c.getString(1), // name
                    c.getString(2), // token key
                    c.getString(3), // token secret
                    c.getString(4), // url
                    c.getString(5), // type /5)
                    isDefault // 6
            );
            account.setPassword(c.getString(7));
        }
        c.close();
        db.close();
        return account;

    }

    /**
     * Sets the account with the passed id as default.
     * This is two steps: set others to non default,
     * set the new default one
     * @param id Primary key of the account.
     */
    public void setDefaultAccount(int id) {
        if (id==-1)
            throw new IllegalStateException("Account id must not be -1");

        Log.i("TweetDB", "Setting default account to id " + id);
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        // First see if the id exists
        Cursor c;
        c= db.query(TABLE_ACCOUNTS,new String[]{"id"},"id = " +id , null, null,null,null);
        if (c.getCount()< 1) {
            throw new IllegalStateException("Account with id " + id + " not found");
        }
        c.close();
        db.execSQL("UPDATE " + TABLE_ACCOUNTS + " SET isDefault = 0 WHERE isDefault = 1");
        db.execSQL("UPDATE " + TABLE_ACCOUNTS + " SET isDefault = 1 WHERE id = " + id);
        db.close();
    }


    public List<Account> getAccountsForSelection() {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        Cursor c;
        List<Account> accounts = new ArrayList<Account>();
        c = db.query(TABLE_ACCOUNTS,null, null,null,null,null,null);
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                boolean isDefault = c.getInt(6) == 1;
                Account account = new Account(
                        c.getInt(0), // id
                        c.getString(1), // name
                        c.getString(2), // token key
                        c.getString(3), // token secret
                        c.getString(4), // url
                        c.getString(5), // type /5)
                        isDefault // 6
                );
                account.setPassword(c.getString(7));
                accounts.add(account);
            } while (c.moveToNext());
        }
        c.close();
        db.close();

        return accounts;
    }

    public Account getAccountForType(String s) {
        List<Account> accounts = getAccountsForSelection();
        for (Account account : accounts) {
            if (account.getServerType().equalsIgnoreCase(s))
                return account;
        }
        return null;
    }



    public int getNewAccountId() {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        Cursor c;
        int ret = 1;
        c = db.query(TABLE_ACCOUNTS,new String[]{"id"},null, null,null,null,"id desc","1");
        if (c.getCount()>0) {
            c.moveToFirst();
            ret = c.getInt(0);
            ret++;
        }
        c.close();
        db.close();
        return ret;
    }

    public void deleteAccount(Account account) {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        String accountString = "" + account.getId();
        String[] accounts = new String[]{accountString};
        for (String table: DATA_TABLES) {
            db.delete(table,ACCOUNT_ID_IS,accounts);
        }
        db.delete(TABLE_ACCOUNTS,"id = ?", accounts);
        db.close();
    }

    public void insertOrUpdateAccount(Account account) {
        ContentValues cv = new ContentValues(8);
        cv.put("id",account.getId());
        cv.put("name",account.getName());
        cv.put("tokenKey",account.getAccessTokenKey());
        cv.put("tokenSecret",account.getAccessTokenSecret());
        cv.put("serverUrl", account.getServerUrl());
        cv.put("serverType", account.getServerType());
        cv.put("isDefault",account.isDefaultAccount() ? 1 : 0);
        cv.put("password",account.getPassword());

        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.insertWithOnConflict(TABLE_ACCOUNTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
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
        try {
            db.beginTransaction();
            for (ContentValues val : values) {
                db.insertWithOnConflict(table,null,val,SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
        db.close();
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


    public String getTargetUrl(String input) {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        Cursor c = db.query(TABLE_URLS,new String[]{"target"},"src = ?",new String[]{input},null,null,null);
        String ret;
        if (c.getCount()>0) {
            c.moveToFirst();
            ret = c.getString(0);
        }
        else {
            ret = input;
        }
        c.close();
        db.close();

        return ret;
    }

    public void persistUpdate(byte[] request) {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        ContentValues cv = new ContentValues(3);

        cv.put("id", (Integer) null);
        cv.put(ACCOUNT_ID,account);
        cv.put("content",request);
        db.insert(TABLE_UPDATES,null,cv);
        db.close();
    }

    public List<Pair<Integer,byte[]>> getUpdatesForAccount() {
        SQLiteDatabase db = tdHelper.getReadableDatabase();
        Cursor c = db.query(TABLE_UPDATES,new String[]{"id","content"},ACCOUNT_ID_IS,new String[]{account},null,null,null);
        List<Pair<Integer,byte[]>> ret = new ArrayList<Pair<Integer, byte[]>>();
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                int id = c.getInt(0);
                byte[] content = c.getBlob(1);
                Pair p = new Pair(id,content);
                ret.add(p);
            }
            while( c.moveToNext());
        }
        c.close();
        db.close();

        return ret;
    }

    public void removeUpdate(int id) {
        SQLiteDatabase db = tdHelper.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_UPDATES + " WHERE id = " + id);
        db.close();
    }
}
