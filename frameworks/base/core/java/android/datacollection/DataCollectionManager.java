package android.datacollection;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import android.datacollection.IDataCollection;

import java.util.ArrayList;

public class DataCollectionManager {
    private static final boolean DEBUG = true;
    private static final String TAG = "DataCollectionManager";
    private static Context mContext;
    private static IDataCollection mService;

    public DataCollectionManager(Context context, IDataCollection service) {
        if (DEBUG){
            Slog.d(TAG, "Data-Driven: DataCollectionManager()");
            if (service == null){
                Slog.d(TAG, "Data-Driven: DataCollectionManager() service is null!");
            }
        }
        mContext = context;
        mService = service;
    }

    public DataCollectionManager(Context context){
        this(context, IDataCollection.Stub.asInterface(ServiceManager.getService(Context.DATA_COLLECTION_SERVICE)));
    }

    public void enableDataCollection() {
        try {
            Slog.d(TAG, "Data-Driven: Call enableDataCollection");
            if (systemBooted()){
                Slog.d(TAG, "Data-Driven: system not booted yet!");
            }else if (mService == null){
                Slog.d(TAG, "Data-Driven: mService is null!");
            }else{
                mService.enableDataCollection();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void disableDataCollection() {
        try {
            Slog.d(TAG, "Call disableDataCollection");
            mService.disableDataCollection();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void collectPkgName(int securityType, String pkgName){
        try {
            if (!systemBooted()) {
                Slog.d(TAG, "Data-Driven: system not boot complete!");
            } else if (mService != null) {
                Slog.d(TAG, "Data-Driven: collectPkgName()");
                mService.collectPkgName(securityType, pkgName);
            }
            else{
                Slog.d(TAG, "Data-Driven: not able to collect pkgName!");
            }
        }catch(Exception e){
            Slog.e(TAG, e.getMessage());
        }
    }

    public static class Contractor {
        public static final String EVENT_TYPE = "knox.metric.event.type";

        public static final int DEVICE_ADMIN_EVENT = 0;
        public static final int ACCESSIBILITY_EVENT = 1;
        public static final int USAGE_STATS_EVENT = 2;

        public static final String ENABLED_ACCESSIBILITY_SERVICES = "knox.metric.accessibility.enabled";
    }

    public void notifyAccessibilityServiceChanged(ArrayList<String> enabledServiceList) {
        if (DEBUG) Slog.d(TAG, "Data-Driven: notifyAccessibilityServiceChanged()");
        Bundle bundle = new Bundle();
        bundle.putInt(Contractor.EVENT_TYPE, Contractor.ACCESSIBILITY_EVENT);
        bundle.putStringArrayList(Contractor.ENABLED_ACCESSIBILITY_SERVICES, enabledServiceList);

        try {
            mService.notifyDataEvent(bundle);
        } catch (RemoteException e) {
            Slog.e(TAG, "Data-Driven: Fail to send data event to DataCollectionService. " +
                    "Error: " + e.getMessage());
        }
    }

    private boolean systemBooted(){
        if (SystemProperties.get("sys.boot_completed").equals("1")) return true;
        return false;
    }
}