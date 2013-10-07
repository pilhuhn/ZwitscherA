package de.bsd.zwitscher;

import java.io.File;
import java.lang.String;
import java.util.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Environment;
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
    public static final String TABLE_READ_IDS = "read_ids";
    private static final String[] DATA_TABLES = {TABLE_STATUSES,
        TABLE_LAST_READ,TABLE_LISTS,TABLE_USERS,TABLE_SEARCHES,TABLE_DIRECTS};
    static final String STATUS = "STATUS";
    static final String ACCOUNT_ID = "ACCOUNT_ID";
    static final String ACCOUNT_ID_IS = ACCOUNT_ID + "=?";
    private static TweetDBOpenHelper tdHelper;
    static final String APP_BASE_DIR = "/Android/data/de.bsd.zwitscher/";

    private static TweetDB instance;

    private static SQLiteDatabase db=null;



	private TweetDB(Context context) {

        File storage = Environment.getExternalStorageDirectory();
        File dbFile = new File(storage,APP_BASE_DIR);

		tdHelper = new TweetDBOpenHelper(context, dbFile.getAbsolutePath() + File.separator + "TWEET_DB", null, 8);
	}

    public static TweetDB getInstance(Context context) {

        if (instance == null) {
            instance = new TweetDB(context);

        }
        if (db==null) {
            db= tdHelper.getWritableDatabase();
        }
        return instance;
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
                    "owner_name TEXT, " +
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

            db.execSQL(CREATE_TABLE + TABLE_READ_IDS + " (" +
                    "id LONG PRIMARY KEY, " +
                    ACCOUNT_ID + " LONG, " +
                    "tstamp LONG "+
                    ")"
            );
            db.execSQL("CREATE UNIQUE INDEX READ_ID_IDX ON " + TABLE_READ_IDS + "(id, " + ACCOUNT_ID +" )" );
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
                        "id INTEGER PRIMARY KEY, " + // PK -> auto increment when pk==null
                        ACCOUNT_ID + " LONG, " +
                        "content BLOB " +
                        ")"
                );

                db.execSQL("DELETE FROM " + TABLE_LISTS);
                db.execSQL("ALTER TABLE " + TABLE_LISTS + " RENAME COLUMN list_json TO owner_name");
            }
            if (oldVersion<8) {
                db.execSQL(CREATE_TABLE + TABLE_READ_IDS + " (" +
                        "id LONG PRIMARY KEY, " +
                        ACCOUNT_ID + " LONG, " +
                        "tstamp LONG "+
                        ")"
                );
                db.execSQL("CREATE UNIQUE INDEX READ_ID_IDX ON " + TABLE_READ_IDS + "(id, " + ACCOUNT_ID + ")");
            }
		}

	}

    void addReadIds(int account,Set<Long> ids) {
        List<ContentValues> values = new ArrayList<ContentValues>(ids.size());
        for (Long id : ids) {
            ContentValues cv = new ContentValues(3);
            cv.put("id",id);
            cv.put(ACCOUNT_ID,account);
            cv.put("tstamp",System.currentTimeMillis());
            values.add(cv);
        }
        storeValues(TABLE_READ_IDS,values);
    }

    boolean isRead(int account, long id) {
        boolean found = false;
        try {
            Cursor c = db.query(TABLE_READ_IDS,new String[] {"id"},"id = ? AND " + ACCOUNT_ID_IS,
                    new String[] {String.valueOf(id),String.valueOf(account)},null,null,null);

            if (c.getCount()>0)
                found=true;
            c.close();

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return found;
    }

    List<Long> getReads(int account, List<Long> idsToCheck) {

        if (idsToCheck==null)
            return new ArrayList<Long>(0);

        StringBuilder sb = new StringBuilder("id IN (");
        Iterator<Long> iter = idsToCheck.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next());
            if (iter.hasNext())
                sb.append(",");
        }
        sb.append(") AND " + ACCOUNT_ID_IS);
        List<Long> readIds;

        Cursor c = db.query(TABLE_READ_IDS, new String[]{"id"}, sb.toString(), new String[]{String.valueOf(account)},
                null,null,null);
        if (c.getCount()==0)
            readIds = new ArrayList<Long>(0);
        else {
            readIds = new ArrayList<Long>(c.getCount());

            c.moveToFirst();
            do {
                long id = c.getLong(0);
                readIds.add(id);
            } while (c.moveToNext());
        }
        c.close();

        return readIds;
    }

    /**
     * Return the id of the status that was last read
     *
     *
     * @param account Id of the account to use
     * @param list_id id of the list
     * @return id of the status that was last read
     */
	long getLastRead(int account, int list_id) {
        Long ret=-1L;
        try {
            Cursor c = db.query(TABLE_LAST_READ, new String[] {"last_read_id"}, "list_id = ? AND " + ACCOUNT_ID_IS, new String[] {String.valueOf(list_id), String.valueOf(
account)}, null, null, null);
            if (c.getCount()==0)
                ret = -1L;
            else {
                c.moveToFirst();
                ret = c.getLong(0);
            }
            c.close();
        } catch (Throwable e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return ret;
	}

    /**
     * Return the id of the status that was last read
     *
     *
     * @param account Id of the account to use
     * @param list_id id of the list
     * @return id of the status that was last fetched
     */
    public long getLastFetched(int account, int list_id) {
        Long ret=-1L;
        try {
            Cursor c = db.query(TABLE_LAST_READ, new String[] {"last_fetched_id"}, "list_id = ? AND " + ACCOUNT_ID_IS, new String[] {String.valueOf(list_id), String.valueOf(
account)}, null, null, null);
            if (c.getCount()==0)
                ret = -1L;
            else {
                c.moveToFirst();
                ret = c.getLong(0);
            }
            c.close();
        } catch (Throwable e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return ret;
	}

    public void markAllRead(int listId, int accountId) {
        db.execSQL("UPDATE " + TABLE_LAST_READ + " SET last_read_id = last_fetched_id WHERE list_id = " + listId + " AND ACCOUNT_ID = " +accountId);
    }

    public int getUnreadCount(int accountId, int listId) {
        int ret =0;
        Cursor c = db.query(TABLE_LAST_READ,new String[]{"last_read_id","last_fetched_id"},"list_id =? AND " + ACCOUNT_ID_IS,
                new String[]{String.valueOf(listId),String.valueOf(accountId)},null,null,null);
        if (c.getCount()>0) {
            c.moveToFirst();
            long lastRead = c.getLong(0);
            long lastFetched = c.getLong(1);

//            Log.i("getUnreadCount","lr= " + lastRead + ", lf="+lastFetched);
            c.close();

            c = db.rawQuery("SELECT COUNT(id) FROM " + TABLE_STATUSES + " WHERE list_id=? AND ACCOUNT_ID=? AND id > ?",
                    new String[]{String.valueOf(listId),String.valueOf(accountId),String.valueOf(lastRead)});
            if (c.getCount()>0) {
                c.moveToFirst();
                ret = c.getInt(0);
            }
            c.close();
        }
        return ret;
    }


    /**
     * Update (or initially store) the last read information of the passed list
     * @param account Id of the account to use
     * @param list_id List to mark as read
     * @param last_read_id Id of the last read status
     */
    public void updateOrInsertLastRead(int account, int list_id, long last_read_id) {
		ContentValues cv = new ContentValues();
		cv.put("list_id", list_id);
		cv.put("last_read_id", last_read_id);
        cv.put(ACCOUNT_ID,account);

        try {
            int updated = db.update(TABLE_LAST_READ, cv, "list_id = ? AND " + ACCOUNT_ID_IS, new String[] {String.valueOf(list_id), String.valueOf(
                    account)});
            if (updated==0) {
                // row not yet present
                db.insert(TABLE_LAST_READ, null, cv);
            }
        } catch (Exception e) {
            // Situation is not too bad, as it just means more network traffic next time // TODO find better solution
            e.printStackTrace();
        }
    }

    /**
     * Update (or initially store) the last read information of the passed list
     * @param account Id of the account to use
     * @param list_id List to mark as read
     * @param last_fetched_id Id of the last fetched status
     */
	void updateOrInsertLastFetched(int account, int list_id, long last_fetched_id) {
		ContentValues cv = new ContentValues();
		cv.put("list_id", list_id);
		cv.put("last_fetched_id", last_fetched_id);
        cv.put(ACCOUNT_ID,account);

        try {
            int updated = db.update(TABLE_LAST_READ, cv, "list_id = ? AND " + ACCOUNT_ID_IS, new String[] {String.valueOf(list_id), String.valueOf(
                    account)});
            if (updated==0) {
                // row not yet present
                db.insert(TABLE_LAST_READ, null, cv);
            }
        } catch (Exception e) {
            // Situation is not too bad, as it just means more network traffic next time // TODO find better solution
            e.printStackTrace();
        }
    }


    /**
     * Return Infos about all lists in the DB
     * @return Map with List Id and Pair (listname,list owner )
     * @param account Id of the account to use
     */
	List<ZUserList> getLists(int account) {
        List<ZUserList> ret = new ArrayList<ZUserList>();
		Cursor c = db.query(TABLE_LISTS, new String[] {"name","owner_name","id"}, ACCOUNT_ID_IS, new String[]{String.valueOf(
                account)}, null, null, "name");
		if (c.getCount()>0){
			c.moveToFirst();
			do {
				String name = c.getString(0);  // 0 = name
				String owner = c.getString(1); // 1 = list owner screen name
                Integer id = c.getInt(2);
                ZUserList zul = new ZUserList(id,name,owner);
				ret.add(zul);
			} while (c.moveToNext());
		}
		c.close();
		return ret;
	}

    /**
     * Get the lists the current account owns
     * @param account Account that owns the lists
     * @return Map with List name and list id
     */
    Map<String,Integer> getOwnedLists(Account account) {
        Map<String,Integer> ret = new HashMap<String, Integer>();
        Cursor c = db.query(TABLE_LISTS, new String[] {"name","id"}, ACCOUNT_ID_IS + " AND owner_name=?", new String[]{String.valueOf(
                      account.getId()),account.getName()}, null, null, "name");
      		if (c.getCount()>0){
      			c.moveToFirst();
      			do {
                  String name = c.getString(0);  // 0 = name
                  Integer id = c.getInt(1);
                  ret.put(name, id );
      			} while (c.moveToNext());
      		}
      		c.close();
      		return ret;
    }

    /**
     * Add a new list to the database
     * @param account Id of the account to use
     * @param name Name of the lise
     * @param id Id of the list
     * @param owner_name screen name of the list owner
     */
	public void addList(int account, String name, int id, String owner_name) {
		ContentValues cv = new ContentValues();
		cv.put("name", name);
		cv.put("id",id);
        cv.put(ACCOUNT_ID,account);
        cv.put("owner_name", owner_name);

		db.insertWithOnConflict(TABLE_LISTS, null, cv,SQLiteDatabase.CONFLICT_IGNORE);
	}

    /**
     * Delete the list with the passed ID in the DB
     * @param id Id of the list to delete
     * TODO Also remove statuses for the passed list
     * @param account Id of the account to use
     */
	public void removeList(Integer id, int account) {
		db.delete(TABLE_LISTS, "id = ? AND " + ACCOUNT_ID_IS, new String[]{id.toString(), String.valueOf(account)});
	}


    /**
     * Update the stored TwitterResponse object. This may be necessary when e.g. the
     * favorite status has been changed on it.
     * @param account Id of the account to use
     * @param id Id of the object
     * @param status_json Json representation of it.
     */
    public void updateStatus(int account, long id, String status_json) {
        ContentValues cv = new ContentValues(2);
        cv.put(STATUS, status_json);
        cv.put(ACCOUNT_ID,account);
        db.update(TABLE_STATUSES, cv, "id = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Return the blob of one stored status by its id and list_id.
     * A status with the same id can occur multiple times with various
     * listIds.
     *
     *
     * @param account Id of the account to use
     * @param statusId The id of the status
     * @param listId The id of the list this status appears. May be null if the list id does not matter.
     * @return The json_string if the status exists in the DB or null otherwise
     */
    public String getStatusObjectById(int account, long statusId, Long listId) {

        String ret = null;
        Cursor c;
        String statusIdS = String.valueOf(statusId);
        if (listId!=null) {
            c= db.query(TABLE_STATUSES, // Table
                    new String[]{STATUS}, // returned column
                    "id = ? AND list_id = ? AND " + ACCOUNT_ID_IS, // selection
                    new String[]{statusIdS,String.valueOf(listId), String.valueOf(account)}, // selection param
                    null, // groupBy
                    null, // having
                    null // order by
            );
        }
        else { // We don't care here - just take one if present
            c= db.query(TABLE_STATUSES, // Table
                    new String[]{STATUS}, // returned column
                    "id = ? AND " + ACCOUNT_ID_IS, // selection
                    new String[]{statusIdS, String.valueOf(account)}, // selection param
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

        return ret;
    }

    public List<String> searchStatuses(int account, String query) {
        List<String> ret = new ArrayList<String>();

        Cursor c ;
        c = db.query(TABLE_STATUSES,new String[]{STATUS}, "status LIKE '%" + query + "%' AND " + ACCOUNT_ID_IS
                ,new String[]{String.valueOf(account)},null,null,"ID DESC","100"); // only 100 results -> may get filtered down later
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                String json = c.getString(0);
                ret.add(json);
            } while (c.moveToNext());
        }
        c.close();

        return ret;

    }


    /**
     * Get all statuses that are marked as a reply to the passed one.
     *
     * @param account Id of the account to use
     * @param inReplyId Id of the original status
     * @return  List of Json_objects that represent the replies
     */
    public List<String> getReplies(int account, long inReplyId) {
        List<String> ret = new ArrayList<String>();

        Cursor c ;
        c = db.query(TABLE_STATUSES,new String[]{STATUS}, "i_rep_to = ? AND " + ACCOUNT_ID_IS
                ,new String[]{String.valueOf(inReplyId), String.valueOf(account)},null,null,"ID DESC");
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                String json = c.getString(0);
                ret.add(json);
            } while (c.moveToNext());
        }
        c.close();

        return ret;
    }

    /**
     * Return the whole conversation starting at the status with id startid
     * @param account Id of the account to use
     * @param startid Status to start the search with
     * @return List of json objects representing the conversation
     */
    public List<String> getThreadForStatus(int account, long startid) {
        List<String> ret = new ArrayList<String>();
        Stack<Long> todo = new Stack<Long>();
        todo.push(startid);

        Cursor c;
        int count = 0;
        String selection = "id = ? AND ACCOUNT_ID = ?";
        while (!todo.isEmpty()) {
            long x = todo.pop();

            count++;
            c = db.query(TABLE_STATUSES,new String[]{"i_rep_to",STATUS}, selection
                    , new String[]{String.valueOf(x), String.valueOf(account)},null,null,null);
            if (c.getCount()>0) {
                c.moveToFirst();
                do {
                    String json = c.getString(1);
                    ret.add(json);
                    long irt = c.getLong(0);
                    if (irt!=-1 ) {
                        todo.push(irt);
                    }
                } while (c.moveToNext());
            }
            c.close();
        }

//        Log.i("getThreadForStatus","Num queries: " + count + ", num results: " + ret.size());

        // also include later replies to thread
        todo.push(startid);
        String selection1 = "i_rep_to = ?  AND ACCOUNT_ID = ?";
        while (!todo.isEmpty()) {
            long x = todo.pop();

            count++;
            c = db.query(TABLE_STATUSES,new String[]{"id",STATUS}, selection1
                    ,new String[]{String.valueOf(x), String.valueOf(account)},null,null,null);
            if (c.getCount()>0) {
                c.moveToFirst();
                do {
                    String json = c.getString(1);
                    ret.add(json);
                    long id = c.getLong(0);
                    if (id!=-1 ) {
                        todo.push(id);
                    }
                } while (c.moveToNext());
            }
            c.close();
        }

//        Log.i("getThreadForStatus","Num queries: " + count + ", num results: " + ret.size());

        return ret;
    }


    /**
     * Return a list of Responses along for the passed list id.
     *
     * @param account Id of the account to use
     * @param sinceId What is the oldest status to look after
     * @param howMany How many entries shall be returned
     * @param list_id From which list?
     * @return List of JResponse objects
     */
    public List<String> getStatusesObjsOlderThan(int account, long sinceId, int howMany, long list_id) {
        List<String> ret = new ArrayList<String>();
        Cursor c;
        String listIdS = String.valueOf(list_id);
        if (sinceId>-1)
            c = db.query(TABLE_STATUSES, // Table
                    new String[]{STATUS}, // Columns returned
                    "id < ? AND list_id = ? AND " +ACCOUNT_ID_IS, // selection
                    new String[]{String.valueOf(sinceId), listIdS, String.valueOf(account)}, // selection values
                    null, // group by
                    null, // having
                    "ID DESC", // order by
                    String.valueOf(howMany) // limit
            );
        else // since id = -1 -> just get the n newest
            c = db.query(TABLE_STATUSES, // Table
                    new String[]{STATUS}, // Columns returned
                    "list_id = ? AND " + ACCOUNT_ID_IS, // selection
                    new String[]{listIdS, String.valueOf(account)},  // selection values
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
        return ret;
    }

    public List<String> getDirectsOlderThan(int account, long sinceId, int howMany) {
        List<String> ret = new ArrayList<String>();
        Cursor c;
        if (sinceId>-1)
            c = db.query(TABLE_DIRECTS,new String[]{"MESSAGE_JSON"},"id < ? AND " +ACCOUNT_ID_IS,new String[]{String.valueOf(sinceId), String.valueOf(
                    account)},null,null,"CREATED_AT DESC",String.valueOf(howMany));
        else
            c = db.query(TABLE_DIRECTS,new String[]{"MESSAGE_JSON"},  ACCOUNT_ID_IS,new String[]{String.valueOf(
                    account)},null,null,"CREATED_AT DESC",String.valueOf(howMany));

        if (c.getCount()>0){
            c.moveToFirst();
            do {
                String json = c.getString(0);
                ret.add(json);
            } while (c.moveToNext());
        }
        c.close();
        return ret;

    }


    /**
     * Purge the last read table.
     */
    public void resetLastRead() {
        db.execSQL("DELETE FROM " + TABLE_LAST_READ);
    }

    /**
     * Purge the statuses table.
     */
    public void cleanTweetDB() {
        db.execSQL("DELETE FROM " + TABLE_STATUSES);
        db.execSQL("DELETE FROM " + TABLE_DIRECTS);
        db.execSQL("DELETE FROM " + TABLE_USERS);
        db.execSQL("DELETE FROM " + TABLE_LAST_READ);
        db.execSQL("DELETE FROM " + TABLE_URLS);
    }

    public void cleanStatusesAndUsers(long cutOff) {
        long now = System.currentTimeMillis();
        db.execSQL("DELETE FROM " + TABLE_STATUSES + " WHERE ctime < " + cutOff);
        long t2 = System.currentTimeMillis();
        Log.d("TDB","clean statuses " + (t2-now) + " ms");

        now = System.currentTimeMillis();
        db.execSQL("DELETE FROM " + TABLE_USERS + " WHERE last_modified < " + cutOff);
        t2 = System.currentTimeMillis();
        Log.d("TDB","clean users    " + (t2-now) + " ms");

        now = System.currentTimeMillis();
        db.execSQL("DELETE FROM " + TABLE_URLS + " WHERE last_modified < " + cutOff);
        t2 = System.currentTimeMillis();
        Log.d("TDB","clean urls     " + (t2-now) + " ms");

        now = System.currentTimeMillis();
        db.execSQL("DELETE FROM " + TABLE_READ_IDS + " WHERE tstamp < " + cutOff);
        t2 = System.currentTimeMillis();
        Log.d("TDB","clean read Ids " + (t2-now) + " ms");

    }

    /**
     * Returns a user by its ID from the database if it exists or null.
     *
     *
     * @param account Id of the account to use
     * @param userId Id of the user
     * @return Basic JSON string of the user info or null.
     */
    public String getUserById(int account, long userId) {

        String ret = null;

        Cursor c;
        c = db.query(TABLE_USERS,new String[]{"user_json"},"userId = ? AND  " + ACCOUNT_ID_IS,new String[] { String.valueOf(userId), String.valueOf(
                account)},null, null, null);
        if (c.getCount()>0) {
            c.moveToFirst();
            ret = c.getString(0);
        }
        c.close();
        return ret;
    }

    /**
     * Returns a user by its screenname from the database if it exists or null.
     *
     *
     * @param account Id of the account to use
     * @param screenName screenname of the user
     * @return Basic JSON string of the user info or null.
     */
    public String getUserByName(int account, String screenName) {
        String ret = null;

        Cursor c;
        c = db.query(TABLE_USERS,new String[]{"user_json"},"screenname = ? AND  " + ACCOUNT_ID_IS ,new String[] { screenName, String.valueOf(
                account)},null, null, null);
        if (c.getCount()>0) {
            c.moveToFirst();
            ret = c.getString(0);
        }
        c.close();
        return ret;
    }


    /**
     * Return a list of all users stored
     * @return list fo json objects in string representation
     * @param account Id of the account to use
     */
    public List<String> getUsers(int account) {
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

        return ret;
    }


    /**
     * Insert a user into the database.
     * @param account Id of the account to use
     * @param userId The Id of the user to insert
     * @param json JSON representation of the User object
     * @param screenName screenname of that user
     */
    public void insertUser(int account, long userId, String json, String screenName) {
        ContentValues cv = new ContentValues(4);
        cv.put("userId",userId);
        cv.put(ACCOUNT_ID,account);
        cv.put("user_json",json);
        cv.put("screenname",screenName);

        db.insertWithOnConflict(TABLE_USERS, null, cv,SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Update an existing user in the database.
     * @param account Id of the account to use
     * @param userId Id of the user to update
     * @param json Json version of the User object
     */
    public void updateUser(int account, long userId, String json) {
        ContentValues cv = new ContentValues(1);
        cv.put("user_json",json);

        db.update(TABLE_USERS, cv, "userId = ? AND " + ACCOUNT_ID + " = ?",
                new String[]{String.valueOf(userId), String.valueOf(account)});
    }

    /**
     * Return the default account or null if no (default) account is found.
     * As one account is always supposed to be default, null is in fact only
     * returned if no account is stored in the database.
     * @return The default account or null
     */
    public Account getDefaultAccount(){
        List<Account> accounts = getAccountsForSelection(true);
        if (accounts.size()==0)
            return null;
        return accounts.get(0);
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
        // First see if the id exists
        Cursor c;
        c= db.query(TABLE_ACCOUNTS,new String[]{"id"},"id = " +id , null, null,null,null);
        if (c.getCount()< 1) {
            throw new IllegalStateException("Account with id " + id + " not found");
        }
        c.close();
        db.execSQL("UPDATE " + TABLE_ACCOUNTS + " SET isDefault = 0 WHERE isDefault = 1");
        db.execSQL("UPDATE " + TABLE_ACCOUNTS + " SET isDefault = 1 WHERE id = " + id);
    }


    public List<Account> getAccountsForSelection(boolean defaultOnly) {
        String selection = null;
        if (defaultOnly) {
            selection="isDefault=1";
        }

        Cursor c;
        List<Account> accounts = new ArrayList<Account>();
        c = db.query(TABLE_ACCOUNTS,null, selection,null,null,null,null);
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                boolean isDefault = c.getInt(6) == 1;
                String accString = c.getString(5);
                accString = accString.replaceAll("\\.","");
                accString = accString.toUpperCase();

                Account account = new Account(
                        c.getInt(0), // id
                        c.getString(1), // name
                        c.getString(2), // token key
                        c.getString(3), // token secret
                        c.getString(4), // url
                        Account.Type.valueOf(accString), // type /5)
                        isDefault // 6
                        );
                account.setPassword(c.getString(7));
                accounts.add(account);
            } while (c.moveToNext());
        }
        c.close();

        return accounts;
    }

    public Account getAccountForType(Account.Type type) {
        List<Account> accounts = getAccountsForSelection(false);
        for (Account account : accounts) {
            if (account.getServerType()==type)
                return account;
        }
        return null;
    }



    public int getNewAccountId() {
        Cursor c;
        int ret = 1;
        c = db.query(TABLE_ACCOUNTS,new String[]{"id"},null, null,null,null,"id desc","1");
        if (c.getCount()>0) {
            c.moveToFirst();
            ret = c.getInt(0);
            ret++;
        }
        c.close();
        return ret;
    }

    public void deleteAccount(Account account) {
        String accountString = "" + account.getId();
        String[] accounts = new String[]{accountString};
        for (String table: DATA_TABLES) {
            db.delete(table,ACCOUNT_ID_IS,accounts);
        }
        db.delete(TABLE_ACCOUNTS,"id = ?", accounts);
    }

    public void insertOrUpdateAccount(Account account) {
        ContentValues cv = new ContentValues(8);
        cv.put("id",account.getId());
        cv.put("name",account.getName());
        cv.put("tokenKey",account.getAccessTokenKey());
        cv.put("tokenSecret",account.getAccessTokenSecret());
        cv.put("serverUrl", account.getServerUrl());
        cv.put("serverType", account.getServerType().name());
        cv.put("isDefault",account.isDefaultAccount() ? 1 : 0);
        cv.put("password",account.getPassword());

        db.insertWithOnConflict(TABLE_ACCOUNTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

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
    }


    public void storeSavedSearch(int account, String name, String query, int id, String json) {
        ContentValues cv = new ContentValues(5);
        cv.put("id",id);
        cv.put("name", name);
        cv.put(ACCOUNT_ID,account);
        cv.put("query",query);
        cv.put("json",json);

        db.insertWithOnConflict(TABLE_SEARCHES,null,cv,SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<String> getSavedSearches(int account) {
        List<String > ret = new ArrayList<String>();

        Cursor c;
        c = db.query(TABLE_SEARCHES,new String[]{"json"},ACCOUNT_ID_IS,new String[] {String.valueOf(account)},null, null, "ID DESC");
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                String json = c.getString(0);
                ret.add(json);
            } while ((c.moveToNext()));
        }
        c.close();

        return ret;
    }

    public void deleteSearch(int account, int id) {
        db.delete(TABLE_SEARCHES,ACCOUNT_ID_IS + " AND id = ?",new String[]{String.valueOf(account),String.valueOf(id)});
    }


    public void persistUpdate(int account, byte[] request) {
        ContentValues cv = new ContentValues(3);

        cv.put("id", (Integer) null);
        cv.put(ACCOUNT_ID,account);
        cv.put("content",request);
        db.insert(TABLE_UPDATES,null,cv);
    }

    public List<Pair<Integer,byte[]>> getUpdatesForAccount(int account) {
        Cursor c = db.query(TABLE_UPDATES,new String[]{"id","content"},ACCOUNT_ID_IS,new String[]{String.valueOf(
                account)},null,null,null);
        List<Pair<Integer,byte[]>> ret = new ArrayList<Pair<Integer, byte[]>>();
        if (c.getCount()>0) {
            c.moveToFirst();
            do {
                int id = c.getInt(0);
                byte[] content = c.getBlob(1);
                Pair<Integer,byte[]> p = new Pair<Integer,byte[]>(id,content);
                ret.add(p);
            }
            while( c.moveToNext());
        }
        c.close();

        return ret;
    }

    public void removeUpdate(int id) {
        db.execSQL("DELETE FROM " + TABLE_UPDATES + " WHERE id = " + id);
    }
}
