package br.com.estudio89.syncing.extras;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import br.com.estudio89.syncing.DataSyncHelper;
import br.com.estudio89.syncing.exceptions.Http403Exception;
import br.com.estudio89.syncing.exceptions.Http408Exception;

import java.io.IOException;

public class SyncService extends Service {
	// Storage for an instance of the sync adapter
    private static SyncAdapter sSyncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();
    /*
     * Instantiate the sync adapter object.
     */
    @Override
    public void onCreate() {
    	/*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
            }

        }
    }
    
    /**
     * Return an object that allows the system to invoke
     * the sync adapter.
     *
     */
    @Override
    public IBinder onBind(Intent intent) {
        /*
         * Get the object that allows external processes
         * to call onPerformSync(). The object is created
         * in the base class code when the SyncAdapter
         * constructors call super()
         */
        return sSyncAdapter.getSyncAdapterBinder();
    }
    
	private static class SyncAdapter extends AbstractThreadedSyncAdapter {
		// Global variables
	    // Define a variable to contain a content resolver instance
	    ContentResolver mContentResolver;
	    Context mContext;
		private String TAG = "Syncing";
	    /**
	     * Set up the sync adapter
	     */
	    public SyncAdapter(Context context, boolean autoInitialize) {
	        super(context, autoInitialize);
			
	        /*
	         * If your app uses a content resolver, get an instance of it
	         * from the incoming Context
	         */
	        mContentResolver = context.getContentResolver();
	        mContext = context;
	    }
	    /**
	     * Set up the sync adapter. This form of the
	     * constructor maintains compatibility with Android 3.0
	     * and later platform versions
	     */
	    @SuppressLint("NewApi")
        @TargetApi(11)
		public SyncAdapter(
	            Context context,
	            boolean autoInitialize,
	            boolean allowParallelSyncs) {
	        super(context, autoInitialize, allowParallelSyncs);
	        /*
	         * If your app uses a content resolver, get an instance of it
	         * from the incoming Context
	         */
	        mContentResolver = context.getContentResolver();
	        mContext = context;
	    }
	    
	    /*
	     * Specify the code you want to run in the sync adapter. The entire
	     * sync adapter runs in a background thread, so you don't have to set
	     * up your own background processing.
	     */
		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {

			Log.d(TAG,"********** RUNNING SYNC *********");
			DataSyncHelper dataSyncHelper = DataSyncHelper.getInstance();
			try {
				dataSyncHelper.fullSynchronousSync();
			} catch (Http408Exception e) {
				dataSyncHelper.syncConfig.requestSync();
			} catch (Http403Exception e) {

			} catch (IOException e) {
				dataSyncHelper.postBackgroundSyncError(e);
			}
			
		}
	}
	
	
}
