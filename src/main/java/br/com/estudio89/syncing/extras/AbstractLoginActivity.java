package br.com.estudio89.syncing.extras;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import br.com.estudio89.syncing.SyncConfig;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.bus.EventBusManager;
import com.squareup.otto.Subscribe;

/**
 * Created by luccascorrea on 12/4/14.
 */
public abstract class AbstractLoginActivity extends AccountAuthenticatorActivity {
    private static String TAG = "Syncing";
    private boolean foreground = false;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        AsyncBus bus = EventBusManager.getBus();
        bus.register(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        foreground = false;
    }

    public boolean isForeground() {
        return foreground;
    }

    public void submitLogin(String username, String password) {
        if (!verifyCrendentials(username,password)) {
            this.onIncompleteCredentials();
            return;
        }
        ServerAuthenticate serverAuthenticate = ServerAuthenticate.getInstance();
        Log.d(TAG, "Enviando login");
        serverAuthenticate.asyncAuthentication(username, password);
    }

    public boolean verifyCrendentials(String username, String password) {

        return !(TextUtils.isEmpty(username) || TextUtils.isEmpty(password));
    }

    public abstract void onIncompleteCredentials();

    @Subscribe
    public abstract void onWrongCredentials(ServerAuthenticate.WrongCredentialsEvent event);

    @Subscribe
    public abstract void onBlockedLogin(ServerAuthenticate.BlockedLoginEvent event);

    @Subscribe
    public abstract void onConnectionError(ServerAuthenticate.ConnectionErrorEvent event);

    @Subscribe
    public void onSuccessfulLogin(ServerAuthenticate.SuccessfulLoginEvent event) {
        String username = event.getUsername();
        String password = event.getPassword();
        String accountType = event.getAccountType();
        String authtoken = event.getAuthToken();

        final Account account = new Account(username, accountType);
        AccountManager am = AccountManager.get(this);
        am.addAccountExplicitly(account, password, null);
        am.setAuthToken(account, "", authtoken);
        SyncConfig syncConfig = SyncConfig.getInstance();
        syncConfig.setAuthToken(authtoken);
        syncConfig.setUsername(username);

        Bundle authenticatorResult = new Bundle();
        authenticatorResult.putString(AccountManager.KEY_ACCOUNT_TYPE,accountType);
        authenticatorResult.putString(AccountManager.KEY_ACCOUNT_NAME,username);
        authenticatorResult.putString(AccountManager.KEY_AUTHTOKEN,authtoken);
        setAccountAuthenticatorResult(authenticatorResult);
        setResult(RESULT_OK);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

}
