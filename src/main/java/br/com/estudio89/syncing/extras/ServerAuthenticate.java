package br.com.estudio89.syncing.extras;

import android.os.AsyncTask;
import android.util.Log;
import br.com.estudio89.syncing.ServerComm;
import br.com.estudio89.syncing.SyncConfig;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.exceptions.Http403Exception;
import br.com.estudio89.syncing.injection.SyncingInjection;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ServerAuthenticate {
	private static String TAG = "Syncing";

	public ServerComm serverComm;

	public SyncConfig syncConfig;

	public AsyncBus bus;

	public static ServerAuthenticate getInstance() {

		return SyncingInjection.get(ServerAuthenticate.class);

	}


	/**
	 * Realiza a autenticação no servidor na url definida no arquivo syncing-config.json.
	 * O servidor deverá retornar um json que deverá ser:
	 *
	 * <pre>
	 *     {
	 *         "verified":false
	 *     }
	 * </pre>
	 *
	 * para o caso em que a autenticação falhou ou:
	 *
	 * <pre>
	 *     {
	 *         "verified":true,
	 *         "token":"asdasd"
	 *     }
	 * </pre>
	 *
	 * para o caso em que a autenticação foi bem sucedida.
	 *
	 * @param username
	 * @param password
	 * @return
	 * @throws IOException
	 */
	protected String syncAuthentication(String username, String password)  {
		JSONObject auth = new JSONObject();
		try {
			auth.put("username", username);
			auth.put("password", password);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		JSONObject response = null;
		try {
			Log.d(TAG,"Enviando post de autenticação ao servidor");
			response = serverComm.post(syncConfig.getAuthenticateUrl(), auth);
		} catch (Http403Exception e) {
			Log.d(TAG,"Exceção 403. Bus = " + bus.hashCode());
			bus.post(new BlockedLoginEvent());
			return null;
		} catch (IOException e) {
			Log.d(TAG,"Erro de conexão. Bus = " + bus.hashCode());
			bus.post(new ConnectionErrorEvent());
			return null;
		}

		boolean verified = false;
		try {
			verified = response.getBoolean("verified");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		String authToken = null;
		String userId = null;
		if (verified) {
			try {
				authToken = response.getString("token");
				userId = response.getString("id");
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			bus.post(new SuccessfulLoginEvent(username, password, syncConfig.getAccountType(), authToken, userId));
			Log.d(TAG,"Login bem sucedido. Bus = " + bus.hashCode());
		} else {
			bus.post( new WrongCredentialsEvent());
			Log.d(TAG,"Credenciais incorretas. Bus = " + bus.hashCode());
		}
		return authToken;
	}

	public void asyncAuthentication(String username, String password) {
		if (!isAuthenticating) {
			new AuthenticationAsyncTask().execute(username,password);
		}
	}

	private static boolean isAuthenticating = false;
	class AuthenticationAsyncTask extends AsyncTask<String,Void,Void> {

		@Override
		protected Void doInBackground(String... credentials) {
			String username = credentials[0];
			String password = credentials[1];

			try {
				isAuthenticating = true;
				Log.d(TAG, "Iniciando autenticação assíncrona");
				syncAuthentication(username, password);
			} finally {
				isAuthenticating = false;
			}

			return null;
		}
	}

	public static class WrongCredentialsEvent{}
	public static class BlockedLoginEvent{}
	public static class ConnectionErrorEvent{}
	public static class SuccessfulLoginEvent{
		private String username;
		private String password;
		private String accountType;
		private String authToken;
		private String userId;

		public SuccessfulLoginEvent(String username, String password, String accountType, String authToken, String userId) {
			this.username = username;
			this.password = password;
			this.accountType = accountType;
			this.authToken = authToken;
			this.userId = userId;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

		public String getAccountType() {
			return accountType;
		}

		public String getAuthToken() {
			return authToken;
		}

		public String getUserId() {
			return userId;
		}
	}

}
