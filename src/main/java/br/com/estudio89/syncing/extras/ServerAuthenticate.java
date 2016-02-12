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
	 * Authenticates in the server through the user defined in the configuration file.
	 * The server will return a json containing:
	 *
	 * <pre>
	 *     {
	 *         "verified":false
	 *     }
	 * </pre>
	 *
	 * in case the authentication failed or:
	 *
	 * <pre>
	 *     {
	 *         "verified":true,
	 *         "token":"asdasd",
	 *         "userId":"..."
	 *     }
	 * </pre>
	 *
	 * in case the authentication was successful.
	 *
	 * If the authentication fails because the server rejected the credentials,
	 * a {@link br.com.estudio89.syncing.extras.ServerAuthenticate.WrongCredentialsEvent} is posted.
	 * If the authentication fails due to a connection error,
	 * a {@link br.com.estudio89.syncing.extras.ServerAuthenticate.ConnectionErrorEvent} is posted.
	 * If the authentication fails because the request was blocked before it could reach the server,
	 * a {@link br.com.estudio89.syncing.extras.ServerAuthenticate.BlockedLoginEvent} is posted.
	 * If the authentication is successful
	 * a {@link br.com.estudio89.syncing.extras.ServerAuthenticate.SuccessfulLoginEvent} is posted.
	 *
	 *
	 * @param username username provided by the user
	 * @param password password provided by the user
	 * @return the authtoken.
	 */
	protected String syncAuthentication(String username, String password)  {
		JSONObject auth = new JSONObject();
		try {
			auth.put("username", username);
			auth.put("password", password);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		JSONObject response;
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

		boolean verified;
		try {
			verified = response.getBoolean("verified");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		String authToken = null;
		String userId;
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

	/**
	 * Handles the authentication in a separate thread.
	 */
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
