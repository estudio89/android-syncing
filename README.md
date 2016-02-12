# Android-syncing

## How to include it in your project

5. Inside your **strings.xml** file, add the following:

    ```xml
    <string name="content_authority">...</string>
    <string name="account_type">...</string>
    ```

    Note that both strings must be unique for your application.

1. Create a file named **syncadapter.xml** inside the folder **res/xml**. The contents of this file should be:

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <sync-adapter
        xmlns:android="http://schemas.android.com/apk/res/android"
        android:contentAuthority="@string/content_authority"
        android:accountType="@string/account_type"
        android:userVisible="false"
        android:supportsUploading="true"
        android:allowParallelSyncs="false"
        android:isAlwaysSyncable="true"/>
    ```

2. Create a file named **authenticator.xml** also inside **res/xml**. The contents of this file should be:

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <account-authenticator
        xmlns:android="http://schemas.android.com/apk/res/android"
        android:accountType="@string/account_type"
        android:icon="@drawable/icone_logo"
        android:smallIcon="@drawable/icone_logo"
        android:label="@string/app_name"/>
    ```

3. In your **AndroidManifest.xml**, add the following as children of the **application** tag:

    ```xml
    <!-- Authentication Service -->
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

    <!-- Sync Service -->
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
    ```

4. Add the following permissions to you **AndroidManifest.xml**:

    ```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.GET_ACCOUNTS" />
    <uses-permission android:name="android.permission.AUTHENTICATE_ACCOUNTS" />
    <uses-permission android:name="android.permission.MANAGE_ACCOUNTS" />
    <uses-permission android:name="android.permission.USE_CREDENTIALS" />
    <uses-permission android:name="android.permission.READ_SYNC_SETTINGS" />
    <uses-permission android:name="android.permission.WRITE_SYNC_SETTINGS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    ```

7. Create the configuration file **config.json** and add it to your **assets** folder:

    ```json
    {
        "syncing": {
            "getDataUrl":"http://192.168.0.100:8000/sync/get-data-from-server/",
            "sendDataUrl":"http://192.168.0.100:8000/sync/send-data-to-server/",
            "loginActivity":"com.example.myapp.ActivityLogin",
            "authenticateUrl":"http://192.168.0.100:8000/sync/authenticate/",
            "accountType":"com.example.myapp.account",
            "contentAuthority":"com.example.myapp.provider",
            "encryptionActive":false,
            "encryptionPassword":"123"
        }

    }
    ```
    Note that you must specify the same **accountType** and **contentAuthority** strings that you added to your **strings.xml** file.

8. Make your **Application** class implement the **DatabaseProvider** interface:

    ```java
    @Override
    public SQLiteDatabase getApplicationDatabase() {
        Database sugarDatabase = super.getDatabase();
        return sugarDatabase.getDB();
    }
    ```

9. In the **onCreate** method of your **Application** class, initialize the sync library, passing the name of the configuration file:

    ```java
    public void onCreate(){
        // ...

        // Sincronização
        SyncingInjection.init(this, "config.json");

        // ...
    }
    ```
10. In the **onCreate** method of you launch activity, you can verify if the user is logged in and display the correct activity accordingly:


    ```java
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SyncConfig syncConfig = SyncConfig.getInstance();
        if (syncConfig.userIsLoggedIn()) {
            if (syncConfig.userNeverSynced()) {
                Intent intent = new Intent(this, ActivityLoading.class);
                startActivity(intent);
                finish();
                return;
            } else {
                Intent i = new Intent(this, ActivityMain.class);
                startActivity(i);
                finish();
                return;
            }
        } else {
            // Authentication
            Intent i = new Intent(this, ActivityLogin.class);
            startActivity(i);
            finish();
        }
    }
    ```

### The login activity

11. In order to implement the login activity, you must create a class that inherits from **AbstractLoginActivity**

12. In this activity, you should override the following methods:

    ```java
    @Override
    public void onIncompleteCredentials()
    /* Called when the user did not type either their username or password but submitted the request anyways */

    @Subscribe
    @Override
    public void onWrongCredentials(ServerAuthenticate.WrongCredentialsEvent wrongCredentialsEvent)
    /* Called when the username and password were considered invalid by the server.
    Use this to notify the user (show a toast?). */

    @Subscribe
    @Override
    public void onBlockedLogin(ServerAuthenticate.BlockedLoginEvent blockedLoginEvent)
    /* Called when the request was not able to reach the server, possibly because the network is
    blocked by a firewall. Use this to notify the user (show a toast?). */

    @Subscribe
    @Override
    public void onConnectionError(ServerAuthenticate.ConnectionErrorEvent connectionErrorEvent)
    /* Called when there was a network error that prevented the request from being
    sent. Use this to notify the user (show a toast?).*/

    @Subscribe
    @Override
    public void onSuccessfulLogin(ServerAuthenticate.SuccessfulLoginEvent event)
    /* Called when the authentication was successful. The event variable contains accessor
    methods that allow you to get the username, password, account type, authentication token
    and user unique id. In this method, you should perform any initialization that should be
    done after logging in, this usually means running a full
    sync with DataSyncHelper.getInstance().fullAsynchronousSync() */

    @Subscribe
    public void onFinishedSync(DataSyncHelper.SyncFinishedEvent event)
    /* This is called when the sync operation (started in the method above) was finished.
    At this point, the user is logged in and all the initial data was fetched, so in this method
    you can unsubscribe from receiving events (see next step), finish this activity
    and launch you main activity.*/

    @Subscribe
    @Override
    public void onBackgroundSyncError(DataSyncHelper.BackgroundSyncError event)
    /* This is called when there was an error while running a sync operation. Use
    this to show a toast to the user, indicating there was a network error. */
    ```

    Note that all the methods above (except for **onIncompleteCredentials**) are annotated with **@Subscribe**. This makes sure these methods are called when their events are fired.

13. It is necessary that in the **onStart** method of this Activity, it registers for receiving events and unregisters in the **onStop** method:

    ```java
    @Override
    protected void onStart() {
        super.onStart();
        EventBusManager.getBus().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            EventBusManager.getBus().unregister(this);
        } catch (IllegalArgumentException e) {
            // No problem. Already unregistered.
        }
    }
    ```
14. After the user types their credentials, you must call the method **submitLogin** passing to it the username and password. This will send the request to the server and trigger the events explained above.


## The SyncManagers

For every model that should be synced in your application, you should create a corresponding class that is called a sync manager. This class's responsibility is to:

* Verify which data must be sent to the server
* Save any data received from the server

When implementing a sync manager, you can inherit from two different classes:

* **```ReadOnlyAbstractSyncManager<Model>```**: meant for data that is only received from the server and never created or altered in the device.
* **```AbstractSyncManager<Model>```**: meant for data that can be updated or created in the device.

Both classes support serialization to and from json out of the box.

When implementing a sync manager, you should extend from one of these classes and implement the necessary methods. For **AbstractSyncManager**, these are:

```java
@Override
public String getIdentifier()
/* This method should return a string that uniquely identifies this sync manager */

@Override
public String getResponseIdentifier()
/* This method should return a string that uniquely identifies a response sent by
the server to this sync manager */

@Override
public boolean shouldSendSingleObject()
/* If the object being synced carries files with it in a way that its size may be large,
you can return true for this method, so that when syncing, only one item is sent
at a time. If you return false (which you should usually do) it will send all items together.*/

@Override
public List<String> getModifiedFiles()
/* This method returns a list with all the files that must be sent the next time a sync happens.
If the model being synced does not send any files with it, you can safely return an empty ArrayList here. */

@Override
public List<String> getModifiedFilesForObject(JSONObject object)
/* This method should return all the files (as a list of file paths) that are
associated with a particular item. If the model being synced does not send any
files with it, you can safely return an empty ArrayList here. */

@Override
public void postEvent(List list, AsyncBus asyncBus, Context context)
/* This method is always called whenever a sync operation was finished and receives
a list of all the items that were either created or updated. This method should then
post an event indicating that synchronization for this sync manager finished.
The implementation of this method is usually something like the following:
    EventBusManager.getBus().post(new MyManagerEvent(list));

The class MyManagerEvent (it could be named something else) should be a public static class
defined in the same file as your sync manager and should implement the SyncEvent<Model> interface.
Then, in your application code, whenever you need to listen to an update event posted
by this sync manager, all you have to do is implement a method that receives and instance
of this class as its only parameter and annotate it with @Subscribe.
*/
```

The difference between **AbstractSyncManager** and **ReadOnlyAbstractSyncManager** is that for the latter, some of the methods above don't need to be implemented.

One thing to note is that every sync manager class you create should be annotated with ```@Register(type = SyncManager.class)``` so that it can be found by the sync library.

## The SyncModels

Every model class that your app has should extend from the class **SyncModel**. This class extends from **SugarRecord**, therefore their behavior is well explained in the **Sugar ORM's** documentation. The only addition that **android-syncing** brings with this class is the addition of a few fields that help identifying the object with the server's unique id and a few other fields (you can see the source code yourself).

Besides that, the android-syncing library includes a few annotations that allow more flexibility when serializing the objects to json. You can use these to annotate the fields in your SyncModel. The annotations are:

### @JSON

This annotation allows changing how a field is serialized and has the following optional parameters:

* **String ignoreIf**: allows you to specify a value that when this field's value is equal to it, it is not written to the json object sent to the server. If not set, the field will never be ignored.
* **boolean ignore**: if this field is used internally and should not be written to the server request or read from the server response, set this to true. Defaults to false.
* **boolean writable**: if this is false, this field will only be read from the server's response (if readable is true), but never written to the request. Defaults to true.
* **boolean readable**: if this is set to false, the field is only read from the server's response but not written to the requests (unless writable is true). Defaults to true.
* **boolean allowOverwrite**: this should be set to false for fields that are changed in the device itself. What this does is it prevents overwriting the value that was changed by the user whenever data is received from the server. Defaults to true.
* **String name**: the name of the json field that will be written/read. If not set, this will be the same as the field's name.

### @NestedManager

This annotation is used when the **SyncModel** has a foreign key to another **SyncModel**. It allows you to specify which sync manager should be used when this particular field will be synced. This annotation has the following parameters:

* **Class manager**: the sync manager class that should be used with this field
* **boolean writable**: works the same as with the @JSON annotation
* **boolean discardOnSave**: this indicates whether the server will always send all of the child items whenever the parent item is synced. If it is true, whenever the parent item is synced, any children no listed along with it will be deleted. Defaults to false.
* **String accessorMethod**: a method that should be used when getting all the children that are associated with the parent item. This is optional and only used if writable is true.
* **paginationParams**: when the nested manager paginates, this should be the name of the field in the json object that represents the pagination parameter for the child.









