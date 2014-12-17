package br.com.estudio89.syncing.extras;

import android.accounts.*;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import br.com.estudio89.syncing.SyncConfig;
import br.com.estudio89.syncing.injection.SyncingInjection;

public class AccountAuthenticatorService extends Service {
	private static final String TAG = "AccountAuthenticatorService";
	private static AccountAuthenticatorImpl sAccountAuthenticator = null;
	private static final String ARG_IS_ADDING_NEW_ACCOUNT = "ARG_IS_ADDING_NEW_ACCOUNT";
	private static final String ARG_AUTH_TYPE = "ARG_AUTH_TYPE";

	
	public AccountAuthenticatorService() {
		super();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		IBinder ret = null;
		if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
			ret = getAuthenticator().getIBinder();
		}
		return ret;
	}
	
	private AccountAuthenticatorImpl getAuthenticator() {
		if (sAccountAuthenticator == null) {
			sAccountAuthenticator = new AccountAuthenticatorImpl(this);
		}
		return sAccountAuthenticator;
	}
	
	private static class AccountAuthenticatorImpl extends AbstractAccountAuthenticator {
		private Context mContext;
		private SyncConfig syncConfig;
		
		public AccountAuthenticatorImpl(Context context) {
			super(context);
			mContext = context;
			syncConfig = SyncingInjection.get(SyncConfig.class);

		}
		
		/*
		 *  The user has requested to add a new account to the system.  
		 *  We return an intent that will launch our login screen if the user has not logged in yet,
		 *  otherwise our activity will just pass the user's credentials on to the account manager.
		 */
		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response,
				String accountType, String authTokenType,
				String[] requiredFeatures, Bundle options)
						throws NetworkErrorException {

			Log.d(TAG,"Método addAccount chamado");
			final Intent intent;
			try {
				intent = new Intent(mContext, syncConfig.getLoginActivityClass());
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		    intent.putExtra(AccountAuthenticatorService.ARG_AUTH_TYPE, authTokenType);
		    intent.putExtra(AccountAuthenticatorService.ARG_IS_ADDING_NEW_ACCOUNT, true);
			final Bundle bundle = new Bundle();
			bundle.putParcelable(AccountManager.KEY_INTENT, intent);
			
			return bundle;
		}
		
		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response,
				Account account, Bundle options) throws NetworkErrorException {
			return null;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response,
				String accountType) {
			return null;
		}

		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {

			Log.d(TAG, "Método getAuthToken chamado. authTokenType = " + authTokenType);
			// Extract the username and password from the Account Manager, and ask
		    // the server for an appropriate AuthToken.
		    final AccountManager am = AccountManager.get(mContext);
		 
		    String authToken = am.peekAuthToken(account, authTokenType);
			Log.d(TAG, "Método getAuthToken: peek = " + authToken);
		    // Lets give another try to authenticate the user
		    if (TextUtils.isEmpty(authToken)) {
		        final String password = am.getPassword(account);
		        if (password != null) {
					Log.d(TAG, "Método getAuthToken: realizando post de autenticação ");
					authToken = ServerAuthenticate.getInstance().syncAuthentication(account.name, password);
					Log.d(TAG, "Método getAuthToken: post de autenticação realizado");
		        }
		    }
		 
		    // If we get an authToken - we return it
		    if (!TextUtils.isEmpty(authToken)) {
		        final Bundle result = new Bundle();
		        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
		        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
		        result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
		        return result;
		    }
		 
		    // If we get here, then we couldn't access the user's password - so we
		    // need to re-prompt them for their credentials. We do that by creating
		    // an intent to display our AuthenticatorActivity.
			final Intent intent;
			try {
				intent = new Intent(mContext, syncConfig.getLoginActivityClass());
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		    final Bundle bundle = new Bundle();
		    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		    return bundle;
		}

		@Override
		public String getAuthTokenLabel(String authTokenType) {
			return null;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response,
				Account account, String[] features)
				throws NetworkErrorException {
			return null;
		}
		
		@Override
		public Bundle getAccountRemovalAllowed(
				AccountAuthenticatorResponse response, Account account)
				throws NetworkErrorException {
			boolean allowed = true;
			Bundle result = new Bundle();
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, allowed);
			return result;
		}
	}

}
