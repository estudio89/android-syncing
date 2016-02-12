package br.com.estudio89.syncing;

import android.accounts.*;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import br.com.estudio89.grabber.Grabber;
import br.com.estudio89.grabber.annotation.GrabberFactory;
import br.com.estudio89.grabber.annotation.InstantiationListener;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.extras.AccountAuthenticatorService;
import br.com.estudio89.syncing.extras.SyncManagerExpiredToken;
import br.com.estudio89.syncing.extras.SyncManagerLogout;
import br.com.estudio89.syncing.injection.SyncingInjection;
import br.com.estudio89.syncing.models.DatabaseReflectionUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * This classes parses the configuration file and holds all configuration items.
 * 
 * @author luccascorrea
 *
 */
@SuppressWarnings({"unchecked", "WeakerAccess", "unused"})
public class SyncConfig {
	private static final String TAG = "Syncing";
	private static String SYNC_PREFERENCES_FILE = "br.com.estudio89.syncing.preferences";
	private static final String TIMESTAMP_KEY = "timestamp";
	private static String AUTH_TOKEN_KEY = "token";
	private static final String INVALID_TOKEN_KEY = "invalid_token";
	private static String USER_ID_KEY = "user_id";
	private static final String DEVICE_ID_KEY = "device_id";
	private static String USERNAME_KEY = "username";

	private final Context context;
	private AsyncBus bus;
	private DataSyncHelper dataSyncHelper;
	private final DatabaseReflectionUtil databaseReflectionUtil;

	private static String configFile;
	private static LinkedHashMap<String,SyncManager> syncManagersByIdentifier = new LinkedHashMap<>();
	private static LinkedHashMap<String,SyncManager> syncManagersByResponseIdentifier = new LinkedHashMap<>();
	private static String mGetDataUrl;
	private static String mSendDataUrl;
	private static String mAuthenticateUrl;
	private static String mCentralAuthenticateUrl;
	private static String accountType;
    private static String mEncryptionPassword;
    private static boolean mEncryptionActive;
	private static String mContentAuthority;
	private static String loginActivity;
	
	public SyncConfig(Context context, AsyncBus bus, DatabaseReflectionUtil databaseReflectionUtil) {
		this.context = context;
		this.bus = bus;
		this.databaseReflectionUtil = databaseReflectionUtil;
	}

	/**
	 * This method is called during injection;
	 * @param dataSyncHelper {@link DataSyncHelper} instance
	 */
	public void setDataSyncHelper(DataSyncHelper dataSyncHelper) {
		this.dataSyncHelper = dataSyncHelper;
	}


	public static SyncConfig getInstance() {

		return SyncingInjection.get(SyncConfig.class);
	}

	public String getContentAuthority() {
		return mContentAuthority;
	}
	public void setConfigFile(String filename) {
		configFile = filename;
		this.loadSettings();
		this.loadDefaultSyncManagers();
		this.setupSyncing();
	}


	/**
	 * Returns the process in which an operation is running.
	 * This is used to ensure that sync operations don't run in a parallel process
	 *
	 * @param context context
	 * @return the process name
	 */
	public static String getProcessName(Context context) {
		int pid = android.os.Process.myPid();
		ActivityManager manager
				= (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		for(ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()){
			if(processInfo.pid == pid){
				return processInfo.processName;
			}
		}
		return "";
	}
	/**
	 * Sets up syncing so it happens automatically (whenever there is a network connection)
	 */
	private void setupSyncing() {
		Account account = getUserAccount();
		String contentAuthority = getContentAuthority();
		if (account != null) {
			Log.d(TAG,"CONFIGURANDO SINCRONIZACAO");
			ContentResolver.setSyncAutomatically(account, contentAuthority, true);
		} else {
			Log.d(TAG,"SINCRONIZACAO NAO CONFIGURADA - CONTA INEXISTENTE");
		}
	}

	public boolean checkingLogin = false;

	/**
	 * @deprecated Verify if the user is logged in ({@link #userIsLoggedIn()}) and launch
	 * the authentication activity manually.

	 * @param activity current activity
	 */
	@Deprecated
	public void showLoginIfNeeded(final Activity activity) {
		if (checkingLogin) {
			return;
		}

		AccountManager am = AccountManager.get(activity);
		checkingLogin = true;
		AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					@SuppressWarnings("UnusedAssignment") Bundle bundle = future.getResult();
				} catch (OperationCanceledException | IOException | AuthenticatorException e) {
					activity.finish();
				} finally {
					checkingLogin = false;
				}
			}
		};
		am.getAuthTokenByFeatures(getAccountType(), "", null, activity, null, null, callback, null);
	}
	/**
	 * Indicates if the user is logged in.
	 *
	 * @return boolean indicating if logged in
	 */
	public boolean userIsLoggedIn() {
		Account account = getUserAccount();
		return account != null && isValidToken();
	}
	/**
	 * Indicates if the user has synced at least once.
	 *
	 * @return boolean indicating if the user has synced
	 */
	public boolean userNeverSynced() {
		JSONObject timestamps = getTimestamps();
		Iterator<?> keys = timestamps.keys();
		while (keys.hasNext()) {
			try {
				String key = (String) keys.next();
				String timestamp = timestamps.getString(key);
				if (("".equals(timestamp) || timestamp == null) && getSyncManager(key).hasTimestamp()) {
					return true;
				}
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}

		return false;
	}
	/**
	 * Returns the authentication token received at the moment the user
	 * authenticated.
	 *
	 * @return the authentication token or null if the user is not logged in.
	 */
	public String getAuthToken() {
		SharedPreferences sharedPref = context.getSharedPreferences(
				SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);

		return sharedPref.getString(AUTH_TOKEN_KEY, null);
	}
	
	/**
	 * Stores the user's authentication token.
	 * 
	 */
	public void setAuthToken(String authToken) {
		SharedPreferences sharedPref = context.getSharedPreferences(
		        SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(AUTH_TOKEN_KEY, authToken);
		editor.commit();

		this.markValidToken();
		this.setupSyncing(); // Já existe uma conta logada, portanto configura a sincronização

	}

	/**
	 * Stores the user's unique identifier.
	 *
	 */
	public void setUserId(String userId) {
		SharedPreferences sharedPref = context.getSharedPreferences(
				SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(USER_ID_KEY, userId);
		editor.commit();

	}

	public String getUserId() {
		SharedPreferences sharedPref = context.getSharedPreferences(
				SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);

		return sharedPref.getString(USER_ID_KEY, "");
	}

	public void markInvalidToken() {
		SharedPreferences sharedPref = context.getSharedPreferences(
				SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(INVALID_TOKEN_KEY, true);
		editor.commit();
	}

	public void markValidToken() {
		SharedPreferences sharedPref = context.getSharedPreferences(
				SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(INVALID_TOKEN_KEY, false);
		editor.commit();
	}

	public boolean isValidToken() {
		SharedPreferences sharedPref = context.getSharedPreferences(
				SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);

		boolean isNotValid = sharedPref.getBoolean(INVALID_TOKEN_KEY, false);
		return !isNotValid;
	}

	public boolean userChanged(String newUserId) {
		return !getUserId().equals(newUserId);
	}

	public SharedPreferences getPreferences() {
		return context.getSharedPreferences(
				SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
	}
	
	/**
	 * Returns a unique identifier for the device.
	 * In case there is no identifier, a new one (UUID) is generated and stored.
	 * 
	 * @return the device's unique identifier
	 */
	protected String getDeviceId() {
		SharedPreferences sharedPref = context.getSharedPreferences(SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		String id = sharedPref.getString(DEVICE_ID_KEY, null);
		if (id == null) {
			id = UUID.randomUUID().toString().replaceAll("-", "");
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString(DEVICE_ID_KEY, id);
			editor.commit();
		}
		return id;
	}

	/**
	 * Sets the device's unique identifier. This method overwrited the unique identifier generated automatically (UUID).
	 * This is used for maintaining compatibility with the registration id used in push notifications.
	 *
	 * @param newId new identifier
	 */
	public void setDeviceId(String newId) {
		SharedPreferences sharedPref = context.getSharedPreferences(SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(DEVICE_ID_KEY, newId);
		editor.commit();
	}

	protected JSONObject getTimestamps() {
		JSONObject timestampsObject = new JSONObject();
		for (SyncManager syncManager : getSyncManagers()) {
			String identifier = syncManager.getIdentifier();
			JSONObject smTimestamp = getTimestamp(identifier);
			try {
				timestampsObject.put(identifier, smTimestamp.getString(identifier));
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
		return timestampsObject;
	}

	protected JSONObject getTimestamp(String identifier) {
		SharedPreferences sharedPref = context.getSharedPreferences(SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		String timestamp = sharedPref.getString(TIMESTAMP_KEY + "_" + identifier,"");
		JSONObject obj = new JSONObject();
		try {
			obj.put(identifier, timestamp);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		return obj;
	}


	protected void setTimestamps(JSONObject timestamps) {
		Iterator<?> keys = timestamps.keys();
		while(keys.hasNext()) {
			String key = (String) keys.next();
			String timestamp;
			try {
				timestamp = timestamps.getString(key);
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}

			SharedPreferences sharedPref = context.getSharedPreferences(SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString(TIMESTAMP_KEY + "_" + key, timestamp);
			editor.commit();
		}
	}

	/**
	 * Stores the username of the logged in user.
	 * This method is used internally and is called right after authentication and should not be called directly.
	 *
	 * @param username the username provided
	 */
	public void setUsername(String username) {
		SharedPreferences sharedPref = context.getSharedPreferences(SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(USERNAME_KEY, username);
		editor.commit();
	}

	/**
	 * Returns the username of the logged in user.
	 *
	 * Useful when reporting exceptions
	 *
	 * @return the username of the logged in user.
	 */
	public String getUsername() {
		SharedPreferences sharedPref = context.getSharedPreferences(SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		return sharedPref.getString(USERNAME_KEY, "");
	}
	/**
	 * Erases all sync preferences.
	 * This is called when logging out.
	 */
	public void eraseSyncPreferences() {
		SharedPreferences sharedPref = context.getSharedPreferences(SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.clear();
		editor.commit();
	}
	
	/**
	 * Returns all the {@link SyncManager}s listed
	 * in the configuration file.
	 * 
	 * @return list of {@link SyncManager}s
	 */
	protected List<SyncManager> getSyncManagers() {
		
		return new ArrayList<>(syncManagersByIdentifier.values());
	}
	
	/**
	 * Returns the {@link SyncManager} with the identifier
	 * given or null if not found.
	 * 
	 * @param identifier the identifier of a {@link SyncManager}
	 * @return the {@link SyncManager} or null if not found
	 */
	protected SyncManager getSyncManager(String identifier) {
		return syncManagersByIdentifier.get(identifier);
	}

	/**
	 * Returns the {@link SyncManager} with the response id given
	 * or null if not found.
	 *
	 * @param responseId the response id of a sync manager.
	 * @return the {@link SyncManager} or null if not found
	 */
	protected SyncManager getSyncManagerByResponseId(String responseId) {
		return syncManagersByResponseIdentifier.get(responseId);
	}
	
	/**
	 * Returns the url used for fetching data from the server.
	 * 
	 * @return the url
	 */
	protected String getGetDataUrl() {
		return mGetDataUrl;
	}

	/**
	 * Returns the url used for authenticating in the server.
	 *
	 * @return the url
	 */
	public String getAuthenticateUrl() {return mAuthenticateUrl;}


	/**
	 * Returns the url used for authenticating in the central server.
	 *
	 * @return the url
	 */
	public String getCentralAuthenticateUrl() {return mCentralAuthenticateUrl;}

	/**
	 * Returns the url used for fetching data from the server for a specific identifier.
	 * 
	 * @param identifier the identifier
	 * @return the url
	 * @throws NoSuchFieldException if there is no url defined for this identifier
	 */
	protected String getGetDataUrlForModel(String identifier) throws NoSuchFieldException {
		return mGetDataUrl + identifier + "/";
	}
	
	/**
	 * Returns the url used for sending data to the server.
	 *
	 * @return the url
	 */
	protected String getSendDataUrl() {
		return mSendDataUrl;
	}
	
	/**
	 * Returns the database. Used inside the {@link CustomTransactionManager}
	 * @return the database
	 */
	protected SQLiteDatabase getDatabase() {
		DatabaseProvider provider = (DatabaseProvider) this.context;
		return provider.getApplicationDatabase();
	}


	/**
	 * Returns the identifier for the user's account.
	 *
	 * @return the identifier
	 */
	public String getAccountType(){ return accountType; }

    /**
     * Returns the encryption key or null if it was not specified.
     * @return the password
     */
    public String getEncryptionPassword() {
        return mEncryptionPassword;
    }

    /**
     * Returns whether encryption was enabled.
     * @return true if enabled false otherwise
     */
    public boolean isEncryptionActive() {
        return mEncryptionActive;
    }
	/**
	 * Returns the user's account.
	 *
	 * @return the account of null if not authenticated.
	 */
	private Account getUserAccount() {
		AccountManager am = AccountManager.get(context);
		Account[] accounts = am.getAccountsByType(getAccountType());
		if(accounts != null && accounts.length > 0)
			return accounts[0];
		else
			return null;
	}

	/**
	 * Requests that a sync is scheduled.
	 *
	 */
	public void requestSync() {
		Log.d(TAG,"********** SOLICITANDO SYNC *********");
		requestSync(false);
	}

	@SuppressWarnings("SameParameterValue")
	public void requestSync(boolean immediate) {
		Account account = getUserAccount();
		String contentAuthority = getContentAuthority();
		Bundle bundle = new Bundle();
		if (immediate) {
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
		}

		ContentResolver.requestSync(account,contentAuthority,bundle);
	}

	/**
	 * Returns the class of the activity used for login.
	 * This is used in the {@link AccountAuthenticatorService}.
	 *
	 * @return the activity class
	 * @throws ClassNotFoundException if the class listed in the configuration file is not found
	 */
	public Class<Activity> getLoginActivityClass() throws ClassNotFoundException {
		return (Class<Activity>) Class.forName(loginActivity);
	}

	/**
	 * Parses the configurations defined in the configuration file.
	 */
	private void loadSettings() {
		try {
			InputStream inputStream = context.getAssets().open(configFile);
			int size = inputStream.available();
			byte[] buffer = new byte[size];
			//noinspection ResultOfMethodCallIgnored
			inputStream.read(buffer);
			inputStream.close();
			
			String jsonString = new String(buffer, "UTF-8");
			JSONObject jsonConfig = new JSONObject(jsonString).getJSONObject("syncing");
			
			mGetDataUrl = StringUtil.appendSlash(jsonConfig.getString("getDataUrl"));
			mSendDataUrl = StringUtil.appendSlash(jsonConfig.getString("sendDataUrl"));
			mAuthenticateUrl = StringUtil.appendSlash(jsonConfig.optString("authenticateUrl"));
			mCentralAuthenticateUrl = StringUtil.appendSlash(jsonConfig.optString("centralAuthenticateUrl"));
			loginActivity = jsonConfig.optString("loginActivity");
			accountType = jsonConfig.optString("accountType");
            mEncryptionPassword = jsonConfig.optString("encryptionPassword");
            mEncryptionActive = jsonConfig.optBoolean("encryptionActive",false);
			mContentAuthority = jsonConfig.getString("contentAuthority");

			GrabberFactory<SyncManager> syncManagerGrabberFactory;
			try {
				syncManagerGrabberFactory = Grabber.getFactory(SyncManager.class);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			syncManagerGrabberFactory.listAll(new InstantiationListener<SyncManager>() {
				@Override
				public void onNewInstance(SyncManager syncManager) {
					String identifier = syncManager.getIdentifier();
					String responseIdentifier = syncManager.getResponseIdentifier();
					syncManager.setDataSyncHelper(dataSyncHelper);
					syncManagersByIdentifier.put(identifier,syncManager);
					syncManagersByResponseIdentifier.put(responseIdentifier, syncManager);
				}
			});

		} catch (IOException | JSONException e) {
			throw new RuntimeException(e);
		}
	}

	private void loadDefaultSyncManagers() {
		List<SyncManager> defaultManagers = new ArrayList<>();
		defaultManagers.add(new SyncManagerExpiredToken());
		defaultManagers.add(new SyncManagerLogout());

		String identifier;
		String responseIdentifier;
		for (SyncManager syncManager: defaultManagers) {
			identifier = syncManager.getIdentifier();
			responseIdentifier = syncManager.getResponseIdentifier();
			syncManager.setDataSyncHelper(this.dataSyncHelper);
			syncManagersByIdentifier.put(identifier,syncManager);
			syncManagersByResponseIdentifier.put(responseIdentifier, syncManager);
		}
	}

	public void logout() {
		logout(true);
	}

	public void logout(boolean postEvent) {
		logout(postEvent, false);
	}
	/**
	 * Removes the users account, erases sync preferences, erases the database and at the end
	 * posts a {@link UserLoggedOutEvent}.
	 *
	 */
	@SuppressWarnings({"deprecation", "UnusedAssignment"})
	public void logout(final boolean postEvent, final boolean invalidToken) {
		String authToken = getAuthToken();
		Account account = getUserAccount();

		if (!invalidToken) {
			eraseSyncPreferences();
		}
		DataSyncHelper.getInstance().stopSyncThreads();

		if (account == null) { // User is not logged in
			return;
		}
		AccountManager am = AccountManager.get(context);
		am.invalidateAuthToken(getAccountType(),authToken);
		AccountManagerCallback<Boolean> callback = new AccountManagerCallback<Boolean>() {
			@Override
			public void run(AccountManagerFuture<Boolean> accountManagerFuture) {
				try {
					if (accountManagerFuture.getResult()) {
						if (!invalidToken) {
							databaseReflectionUtil.eraseData();
						} else {
							markInvalidToken();
						}

						if (postEvent) {
							bus.post(new UserLoggedOutEvent());
						}
						Log.d(TAG,"Postou evento UserLoggedOutEvent hashcode " + bus.hashCode());
					} else {
						throw new RuntimeException("Error when logging out. The account removal was not allowed by the Authenticator.");
					}
				} catch (OperationCanceledException | AuthenticatorException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		};
		if (Looper.myLooper() == Looper.getMainLooper()) {
			AccountManagerFuture<Boolean> accountManagerFuture = am.removeAccount(account, callback, new Handler());
		} else if(Looper.myLooper() == null){
			AccountManagerFuture<Boolean> accountManagerFuture = am.removeAccount(account, callback, null);
		}
		else {
			AccountManagerFuture<Boolean> accountManagerFuture = am.removeAccount(account, null, new Handler());
			callback.run(accountManagerFuture);
		}
	}

	public static class UserLoggedOutEvent {

	}
	
}
