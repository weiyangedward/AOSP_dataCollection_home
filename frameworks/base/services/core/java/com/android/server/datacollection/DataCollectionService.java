package com.android.server.datacollection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Slog;
import android.datacollection.IDataCollection;

import com.android.server.SystemService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DataCollectionService extends SystemService {
    private static final String TAG = "DataCollectionService";
    private static final boolean DEBUG = true;
    private final Context mContext;

    private DataCollectionDatabaseHelper mDataCollectionDatabaseHelper;
    private SQLiteDatabase db;

    public DataCollectionService(Context context){
        super(context);
        if (DEBUG){
            Slog.d(TAG, "Data-Driven: DataCollectionService constructor");
        }
        mContext = context;
        try {
            publishBinderService(Context.DATA_COLLECTION_SERVICE, mService);
            startDB();
        }catch (Exception e){
            Slog.e(TAG, e.toString());
        }
    }

    @Override
    public void onStart(){
        if (DEBUG) {
            Slog.d(TAG, "Data-Driven: onStart()");
        }
    }

    private void startDB(){
        mDataCollectionDatabaseHelper = new DataCollectionDatabaseHelper(mContext);
        db = mDataCollectionDatabaseHelper.getWritableDatabase();
        mDataCollectionDatabaseHelper.resetTable(db);
    }

    /*
    * service interface
    * */
    private final IBinder mService = new IDataCollection.Stub() {

        @Override
        public void enableDataCollection() throws RemoteException {
            if (DEBUG) {
                Slog.d(TAG, "Data-Driven: Call enableDataCollection");
            }
        }

        @Override
        public void disableDataCollection() throws RemoteException {
            if (DEBUG) {
                Slog.d(TAG, "Data-Driven: Call disableDataCollection");
            }
        }

        /*
        * insert a record to table
        * */
        @Override
        public void collectPkgName(String securityType, String pkgName) throws RemoteException {
            if (DEBUG){
                Slog.d(TAG, "Data-Driven: collectPkgName() " + securityType + " " + pkgName);
            }
            try {
                mDataCollectionDatabaseHelper.insertPkgName(db,
                        FeedEntry.TABLE_NAME,
                        securityType,
                        pkgName,
                        "enabled");
                mDataCollectionDatabaseHelper.dump(db, FeedEntry.TABLE_NAME);
            }catch (Exception e){
                Slog.e(TAG, e.toString());
            }
        }

    };

    /*
    * contract class for table names and attributes
    * */
    public static class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "pkg_names_list";
        public static final String SECURITY_TYPE = "types";
        public static final String PKGNAME = "pkgs";
        public static final String ENABLED = "enabled";
        public static final String CREATED_AT = "created_at";
    }

    /*
   * SQLite database
   * */
    class DataCollectionDatabaseHelper extends SQLiteOpenHelper {

        private static final String DB_TAG = "DataCollectionDatabaseHelper";
        private static final String DB_NAME = "datacollection";
        private static final int DB_VERSION = 2;

        DataCollectionDatabaseHelper(Context context){
            super(context, DB_NAME, null, DB_VERSION);
            if (DEBUG){
                Slog.d(DB_TAG, "Data-Driven: constructor");
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db){
            updateMyDatabase(db, 0, DB_VERSION);
            if (DEBUG){
                Slog.d(DB_TAG, "Data-Driven: onCreate()");
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVerion){
            updateMyDatabase(db, 0, DB_VERSION);
            if (DEBUG){
                Slog.d(DB_TAG, "Data-Driven: onUpgrade()");
            }
        }

        /*
        * add table to database
        * */
        private void updateMyDatabase(SQLiteDatabase db, int oldVersion, int newVersion){
            if (oldVersion < 1){
                createTable(db);
            }
        }

        /*
        * create a new table
        * */
        private void createTable(SQLiteDatabase db){
            db.execSQL("CREATE TABLE " +  FeedEntry.TABLE_NAME
                    + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + FeedEntry.SECURITY_TYPE + " TEXT, "
                    + FeedEntry.PKGNAME + " TEXT, "
                    + FeedEntry.ENABLED + " TEXT, "
                    + FeedEntry.CREATED_AT +");");
        }

        public void resetTable(SQLiteDatabase db){
            db.execSQL("delete from "+ FeedEntry.TABLE_NAME);
//            db.delete(FeedEntry.TABLE_NAME, null, null);
//            createTable(db);
        }

        /*
        * return true if a record existed in table
        * */
        private boolean recordExisted(SQLiteDatabase db, String table, String[] returnColumns,
                                      String conditions, String[] conditionValues){
            try{
                Cursor cursor = db.query(
                        table,
                        returnColumns,
                        conditions,
                        conditionValues,
                        null, null, null);
                if (cursor.moveToFirst()){
                    cursor.close();
                    return true;
                }
                cursor.close();
                return false;
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }

        /*
        * print all records in a table
        * */
        public void dump(SQLiteDatabase db, String table){
            Slog.d(DB_TAG, "Data-Driven: dump()");
            try{
                Cursor cursor = db.query(
                        table,
                        null, null, null, null, null, null);
                if (cursor.getCount() <= 0){
                    Slog.d(DB_TAG, "Data-Driven: cursor can't read anything!");
                }
                while (cursor.moveToNext()){
                    StringBuilder sb = new StringBuilder();
                    sb.append(cursor.getString(1) + " ");
                    sb.append(cursor.getString(2) + " ");
                    sb.append(cursor.getString(3) + " ");
                    sb.append(cursor.getString(4));
                    Slog.d(DB_TAG, "Data-Driven: " + sb.toString());
                }
                cursor.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        /*
        * return current data and time
        * */
        public String getDateTime() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            return dateFormat.format(date);
        }

        /*
        * insert a record to table
        * */
        private void insertPkgName(SQLiteDatabase db, String table, String securityType, String
                pkgName, String enabled){
            if (DEBUG){
                Slog.d(DB_TAG, "Data-Driven: insertPkgName() " + securityType + " " + pkgName);
            }

            try {
                ContentValues dataValues = new ContentValues();
                dataValues.put(FeedEntry.SECURITY_TYPE, securityType);
                dataValues.put(FeedEntry.PKGNAME, pkgName);
                dataValues.put(FeedEntry.ENABLED, enabled);
                dataValues.put(FeedEntry.CREATED_AT, getDateTime());
                long newRowId = db.insert(table, null, dataValues);
                Slog.d(DB_TAG, "Data-Driven: insert to " + Integer.toString((int)newRowId));
            }catch (Exception e){
                Slog.e(DB_TAG, "Data-Driven: " + e.toString());
            }
        }
    }
}