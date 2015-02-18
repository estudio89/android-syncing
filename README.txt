ALTERACOES:

1) Criar arquivo syncadapter.xml dentro da pasta res/xml. Inserir:

<?xml version="1.0" encoding="utf-8"?>
<sync-adapter
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:contentAuthority="@string/content_authority"
    android:accountType="@string/account_type"
    android:userVisible="false"
    android:supportsUploading="true"
    android:allowParallelSyncs="false"
    android:isAlwaysSyncable="true"/>

2) Criar arquivo authenticator.xml também na pasta xml. Inserir:

<?xml version="1.0" encoding="utf-8"?>
<account-authenticator
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accountType="@string/account_type"
    android:icon="@drawable/icone_logo"
    android:smallIcon="@drawable/icone_logo"
    android:label="@string/app_name"
    android:accountPreferences="@xml/account_preferences"/>

3) No manifest, incluir:

<!-- Servico de autenticacao -->
<service
    android:name="br.com.estudio89.syncing.extras.AccountAuthenticatorService"
    android:exported="true" >
    <intent-filter>
        <action android:name="android.accounts.AccountAuthenticator" />
    </intent-filter>

    <meta-data
        android:name="android.accounts.AccountAuthenticator"
        android:resource="@xml/authenticator" />
</service>

<!-- Servico de sincronizacao -->
<service
    android:name="br.com.estudio89.syncing.extras.SyncService"
    android:exported="true">
    <intent-filter>
        <action android:name="android.content.SyncAdapter"/>
    </intent-filter>
    <meta-data android:name="android.content.SyncAdapter"
        android:resource="@xml/syncadapter"/>

</service>

<!-- Content Provider -->
<provider
    android:name="br.com.estudio89.syncing.extras.StubProvider"
    android:authorities="@string/content_authority"
    android:exported="false"
    android:syncable="true">

</provider>

4) Adicionar as seguintes permissões ao manifest:
<uses-permission android:name="android.permission.AUTHENTICATE_ACCOUNTS" />
<uses-permission android:name="android.permission.MANAGE_ACCOUNTS" />
<uses-permission android:name="android.permission.USE_CREDENTIALS" />
<uses-permission android:name="android.permission.GET_ACCOUNTS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_SYNC_SETTINGS" />
<uses-permission android:name="android.permission.WRITE_SYNC_SETTINGS" />

5) No arquivo strings.xml, incluir:

<string name="content_authority">br.com.estudio89.syncing.provider</string>
<string name="account_type">...</string>

6) Note que a string "content_authority" deve ser única para a sua aplicação.
   Para a string "account_type", deve ser utilizado o mesmo identificador da account utilizado no arquivo config.json

7) Criar arquivo config.json e colocá-lo na pasta assets:

{
    "syncing": {
        "getDataUrl":"http://192.168.0.100:8000/formularios/api/get-data-from-server/",
        "sendDataUrl":"http://192.168.0.100:8000/formularios/api/send-data-to-server/",
        "syncManagers":[
            {"class":"br.com.estudio89.crmvpr.syncing.SyncManagerFormulario", "getDataUrl":""},
            {"class":"br.com.estudio89.crmvpr.syncing.SyncManagerCampo", "getDataUrl":""},
            {"class":"br.com.estudio89.crmvpr.syncing.SyncManagerOpcao", "getDataUrl":""},
            {"class":"br.com.estudio89.crmvpr.syncing.SyncManagerFormularioDeleted", "getDataUrl":""},
            {"class":"br.com.estudio89.crmvpr.syncing.SyncManagerEmpresa", "getDataUrl":""},
            {"class":"br.com.estudio89.crmvpr.syncing.SyncManagerEmpresaDeleted", "getDataUrl":""},
            {"class":"br.com.estudio89.crmvpr.syncing.SyncManagerRegistro", "getDataUrl":""},
            {"class":"br.com.estudio89.crmvpr.syncing.SyncManagerResposta", "getDataUrl":""},
            {"class":"br.com.estudio89.crmvpr.syncing.SyncManagerOpcaoResp", "getDataUrl":""},
            {"class":"br.com.estudio89.crmvpr.syncing.SyncManagerImagem", "getDataUrl":""}
        ],

        "loginActivity":"br.com.estudio89.crmvpr.activities.ActivityLogin",
        "authenticateUrl":"http://192.168.0.100:8000/formularios/api/authenticate/",
        "accountType":"br.com.estudio89.crmvpr.account"
    }

}

8) Fazer com que a classe da aplicação implemente a interface DatabaseProvider:

@Override
public SQLiteDatabase getApplicationDatabase() {
    Database sugarDatabase = super.getDatabase();
    return sugarDatabase.getDB();
}

9) No método onCreate da aplicação, inicializar a biblioteca de sincronização, passando o nome do arquivo de configuração:

    public void onCreate(){
        // ...

        // Sincronização
        SyncingInjection.init(this,"config.json");

        // ...
    }

10) No método onCreate da ActivityPrincipal, solicitar que a tela de login seja exibida caso necessário:

@Override
public void onCreate(Bundle savedInstanceState) {
    // Authentication
    SyncConfig.getInstance().showLoginIfNeeded(this);
}

11) Para implementar a Activity de autenticação, criar classe herdando de AbstractLoginActivity

12) Nessa activity, ao enviar os dados de login para o servidor, utilizar o método submitLogin

13) É necessário que no método onResume dessa Activity, ela se registre para receber eventos de sincronização e desregistre no método onStop:

    @Override
    protected void onResume() {
        super.onResume();
        EventBusManager.getBus().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBusManager.getBus().unregister(this);
    }

Além disso, é preciso que os métodos onWrongCredentials, onBlockedLogin, onConnectionError e onSuccessFulLogin recebam a anotação @Subscribe (Otto)