ALTERACOES:

Criar syncadapter.xml. Inserir:

<?xml version="1.0" encoding="utf-8"?>
<sync-adapter
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:contentAuthority="@string/content_authority"
    android:accountType="@string/account_type"
    android:userVisible="false"
    android:supportsUploading="true"
    android:allowParallelSyncs="false"
    android:isAlwaysSyncable="true"/>

Criar authenticator.xml na pasta xml. Inserir:

<?xml version="1.0" encoding="utf-8"?>
<account-authenticator
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accountType="@string/account_type"
    android:icon="@drawable/icone_logo"
    android:smallIcon="@drawable/icone_logo"
    android:label="@string/app_name"
    android:accountPreferences="@xml/account_preferences"/>

No manifest, incluir:

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

Adicionar permissões:
<uses-permission android:name="android.permission.AUTHENTICATE_ACCOUNTS" />
<uses-permission android:name="android.permission.MANAGE_ACCOUNTS" />
<uses-permission android:name="android.permission.USE_CREDENTIALS" />
<uses-permission android:name="android.permission.GET_ACCOUNTS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_SYNC_SETTINGS" />
<uses-permission android:name="android.permission.WRITE_SYNC_SETTINGS" />

No arquivo strings.xml, incluir:

<string name="content_authority">br.com.estudio89.syncing.provider</string>
<string name="account_type">...</string>

Para o account_type, utilizar o mesmo identificador da account utilizado no syncing-config.json

Criar arquivo syncing-config.json na pasta assets:

{
	"getDataUrl":"http://api.estudio89.com.br/send-data",
	"sendDataUrl":"http://api.estudio89.com.br/get-data",
	"syncManagers":[
		{"class":"br.com.estudio89.syncing.TestSyncManager", "getDataUrl":"http://api.estudio89.com.br/test/"}
	],


	"loginActivity":"br.com.estudio89.activities.ActivityLogin",
	"authenticateUrl":"http://api.estudio89.com.br/auth",
	"accountType":"br.com.estudio89"
}
