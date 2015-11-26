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
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.injection.SyncingInjection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Classe que decodifica e armazena os parâmetros de configuração do módulo.
 * 
 * @author luccascorrea
 *
 */
public class SyncConfig {
	private static String TAG = "Syncing";
	private static String SYNC_PREFERENCES_FILE = "br.com.estudio89.syncing.preferences";
	private static String TIMESTAMP_KEY = "timestamp";
	private static String AUTH_TOKEN_KEY = "token";
	private static String DEVICE_ID_KEY = "device_id";
	private static String USERNAME_KEY = "username";
	private static String CONTENT_AUTHORITY = "br.com.estudio89.syncing.provider";

	private Context context;
	private AsyncBus bus;
	private DataSyncHelper dataSyncHelper;

	private static String configFile;
	private static LinkedHashMap<String,SyncManager> syncManagersByIdentifier = new LinkedHashMap<String, SyncManager>();
	private static LinkedHashMap<String,SyncManager> syncManagersByResponseIdentifier = new LinkedHashMap<String, SyncManager>();
	private static String mGetDataUrl;
	private static String mSendDataUrl;
	private static String mAuthenticateUrl;
	private static String mCentralAuthenticateUrl;
	private static String accountType;
    private static String mEncryptionPassword;
    private static boolean mEncryptionActive;
	private static String mContentAuthority;
	private static HashMap<String,String> mModelGetDataUrls = new HashMap<String, String>();
	private static String loginActivity;
	
	public SyncConfig(Context context, AsyncBus bus) {
		this.context = context;
		this.bus = bus;
	}

	/**
	 * This method is called during injection;
	 * @param dataSyncHelper
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
		this.setupSyncing();
	}


	/**
	 * Retorna o processo em que uma determinada ação está ocorrendo.
	 * Utilizado para garantir que a sincronização não ocorra em um processo paralelo.
	 *
	 * @param context
	 * @return
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
	 * Configura a sincronização para ocorrer de forma automática
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
	public void showLoginIfNeeded(final Activity activity) {
		if (checkingLogin) {
			return;
		}

		AccountManager am = AccountManager.get(activity);
		checkingLogin = true;
		AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					Bundle bundle = future.getResult();
				} catch (OperationCanceledException e) {
					activity.finish();
				} catch (IOException e) {
					activity.finish();
				} catch (AuthenticatorException e) {
					activity.finish();
				} finally {
					checkingLogin = false;
				}
			}
		};
		am.getAuthTokenByFeatures(getAccountType(), "", null, activity, null, null, callback, null);
	}
	/**
	 * Indica se o usuário está logado.
	 *
	 * @return
	 */
	public boolean userIsLoggedIn() {
		return getUserAccount() != null;
	}
	/**
	 * Indica se o usuário já realizou uma sincronização alguma vez.
	 * Caso não, é necessário chamar o método
	 * fullAsynchronousSync do DataSyncHelper.
	 *
	 * @return
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
	 * Retorna o token de identificação recebido
	 * no momento em que foi feito login.
	 * 
	 * @return
	 */
	public String getAuthToken() {
		SharedPreferences sharedPref = context.getSharedPreferences(
				SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		
		String timestamp = sharedPref.getString(AUTH_TOKEN_KEY, null);
		return timestamp;
	}
	
	/**
	 * Armazena o token de identificação
	 * do usuário.
	 * 
	 */
	public void setAuthToken(String authToken) {
		SharedPreferences sharedPref = context.getSharedPreferences(
		        SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(AUTH_TOKEN_KEY, authToken);
		editor.commit();

		this.setupSyncing(); // Já existe uma conta logada, portanto configura a sincronização
	}

	public SharedPreferences getPreferences() {
		SharedPreferences sharedPref = context.getSharedPreferences(
				SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		return sharedPref;
	}
	
	/**
	 * Retorna e armazena um identificador para o
	 * device (UUID).
	 * 
	 * @return
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
	 * Seta o valor do device id. Esse método sobrescreve a geração automática de um UUID.
	 * Utilizado para ser compatível com o registration id de notificações push.
	 *
	 * @param newId
	 * @return
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
			String timestamp = "";
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
	 * Armazena o username do usuário logado.
	 * Não deve ser acessado diretamente.
	 *
	 * @param username
	 */
	public void setUsername(String username) {
		SharedPreferences sharedPref = context.getSharedPreferences(SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(USERNAME_KEY, username);
		editor.commit();
	}

	/**
	 * Retorna o username do usuário logado.
	 * Utilizado para envio ao Sentry.
	 *
	 * @return
	 */
	public String getUsername() {
		SharedPreferences sharedPref = context.getSharedPreferences(SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		String username = sharedPref.getString(USERNAME_KEY, "");
		return username;
	}
	/**
	 * Apaga as preferências de sincronização (token, timestamp e id do device).
	 * Executado durante o logout.
	 */
	protected void eraseSyncPreferences() {
		SharedPreferences sharedPref = context.getSharedPreferences(SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.clear();
		editor.commit();
	}
	
	/**
	 * Retorna todos os {@link SyncManager}s listados
	 * no arquivo de configuração.
	 * 
	 * @return
	 */
	protected List<SyncManager> getSyncManagers() {
		
		return new ArrayList<SyncManager>(syncManagersByIdentifier.values());
	}
	
	/**
	 * Retorna o {@link SyncManager} com identificador
	 * correspodente ou null caso não seja encontrado.
	 * 
	 * @param identifier
	 * @return
	 */
	protected SyncManager getSyncManager(String identifier) {
		return syncManagersByIdentifier.get(identifier);
	}

	/**
	 * Retorna o {@link SyncManager} com response id
	 * correspodente ou null caso não seja encontrado.
	 *
	 * @param responseId
	 * @return
	 */
	protected SyncManager getSyncManagerByResponseId(String responseId) {
		return syncManagersByResponseIdentifier.get(responseId);
	}
	
	/**
	 * Retorna a url de recebimento de dados do servidor.
	 * 
	 * @return
	 */
	protected String getGetDataUrl() {
		return mGetDataUrl;
	}

	/**
	 * Retorna a url de autenticação no servidor.
	 *
	 * @return
	 */
	public String getAuthenticateUrl() {return mAuthenticateUrl;}


	/**
	 * Retorna a url de autenticação no servidor central.
	 *
	 * @return
	 */
	public String getCentralAuthenticateUrl() {return mCentralAuthenticateUrl;}

	/**
	 * Retorna a url de recebimento de dados para
	 * um identificador específico.
	 * 
	 * @param identifier
	 * @return
	 * @throws NoSuchFieldException 
	 */
	protected String getGetDataUrlForModel(String identifier) throws NoSuchFieldException {
		String url = mModelGetDataUrls.get(identifier);
		
		if (url == null) {
			throw new NoSuchFieldException("não foi encontrada uma url para o identificador " + identifier + ". Verifique o arquivo XML de configuração da sincronização.");
		}
		return url; 
	}
	
	/**
	 * Retorna a url de envio de dados para o servidor.
	 * @return
	 */
	protected String getSendDataUrl() {
		return mSendDataUrl;
	}
	
	/**
	 * Retorna o nome da database.
	 * @return
	 */
	protected SQLiteDatabase getDatabase() {
		DatabaseProvider provider = (DatabaseProvider) this.context;
		return provider.getApplicationDatabase();
	}


	/**
	 * Retorna o identificador da conta criada para o usuário.
	 *
	 * @return
	 */
	public String getAccountType(){ return accountType; }

    /**
     * Returns the encryption key or null if it was not specified.
     * @return
     */
    public String getEncryptionPassword() {
        return this.mEncryptionPassword;
    }

    /**
     * Returns whether encryption was enabled.
     * @return
     */
    public boolean isEncryptionActive() {
        return mEncryptionActive;
    }
	/**
	 * Retorna a conta do usuário.
	 *
	 * @return
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
	 * Solicita uma sincronização com o servidor.
	 *
	 */
	public void requestSync() {
		Log.d(TAG,"********** SOLICITANDO SYNC *********");
		requestSync(false);
	}

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
	 * Retorna a classe utilizada para login.
	 * Utilizada no AccountAuthenticatorService.
	 *
	 * @return
	 * @throws ClassNotFoundException
	 */
	public Class<Activity> getLoginActivityClass() throws ClassNotFoundException {
		return (Class<Activity>) Class.forName(loginActivity);
	}

	/**
	 * Carrega as configurações a partir do arquivo json syncing-config.json
	 */
	private void loadSettings() {
		try {
			InputStream inputStream = context.getAssets().open(configFile);
			int size = inputStream.available();
			byte[] buffer = new byte[size];
			inputStream.read(buffer);
			inputStream.close();
			
			String jsonString = new String(buffer, "UTF-8");
			JSONObject jsonConfig = new JSONObject(jsonString).getJSONObject("syncing");
			
			mGetDataUrl = jsonConfig.getString("getDataUrl");
			mSendDataUrl = jsonConfig.getString("sendDataUrl");
			mAuthenticateUrl = jsonConfig.optString("authenticateUrl");
			mCentralAuthenticateUrl = jsonConfig.optString("centralAuthenticateUrl");
			loginActivity = jsonConfig.optString("loginActivity");
			accountType = jsonConfig.optString("accountType");
            mEncryptionPassword = jsonConfig.optString("encryptionPassword");
            mEncryptionActive = jsonConfig.optBoolean("encryptionActive",false);
			mContentAuthority = jsonConfig.getString("contentAuthority");

			JSONArray syncManagersJson = jsonConfig.getJSONArray("syncManagers");
			JSONObject syncManagerJson;
			SyncManager syncManager;
			String className;
			String getDataUrl;
			Class klass;
			String identifier;
			String responseIdentifier;
			for (int i = 0; i < syncManagersJson.length(); i++) {
				syncManagerJson = syncManagersJson.getJSONObject(i);
				className = syncManagerJson.getString("class");
				getDataUrl = syncManagerJson.getString("getDataUrl");
				klass = Class.forName(className);
				syncManager = (SyncManager) klass.newInstance();
				identifier = syncManager.getIdentifier();
				responseIdentifier = syncManager.getResponseIdentifier();
				syncManager.setDataSyncHelper(this.dataSyncHelper);
				syncManagersByIdentifier.put(identifier,syncManager);
				syncManagersByResponseIdentifier.put(responseIdentifier, syncManager);
				mModelGetDataUrls.put(identifier, getDataUrl);
				
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public void logout() {
		logout(true);
	}
	/**
	 * Remove a conta do usuário, apaga as preferências de sincronização e, ao final,
	 * lança um evento da classe UserLoggedOutEvent.
	 *
	 */
	public void logout(final boolean postEvent) {
		String authToken = getAuthToken();
		Account account = getUserAccount();

		eraseSyncPreferences();
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

						if (postEvent) {
							bus.post(new UserLoggedOutEvent());
						}
						Log.d(TAG,"Postou evento UserLoggedOutEvent hashcode " + bus.hashCode());
					} else {
						throw new RuntimeException("Erro ao fazer logout. A remoção da conta não foi permitida pelo Authenticator.");
					}
				} catch (OperationCanceledException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} catch (AuthenticatorException e) {
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
