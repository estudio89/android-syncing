package br.com.estudio89.syncing;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Created by luccascorrea on 5/6/15.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class InjectionTests {
    Application application;

    @Mock
    SQLiteDatabase database;

    @Before
    public void setup() throws Exception {
        application = Mockito.mock(Application.class, withSettings().extraInterfaces(DatabaseProvider.class));
        System.out.println(application instanceof DatabaseProvider);

        Mockito.when(application.openOrCreateDatabase(Mockito.anyString(), Mockito.eq(Context.MODE_PRIVATE), Mockito.isNull(SQLiteDatabase.CursorFactory.class))).thenReturn(database);

        AssetManager assetManager = Mockito.mock(AssetManager.class);
        ActivityManager manager = Mockito.mock(ActivityManager.class);
        List<ActivityManager.RunningAppProcessInfo> processes = new ArrayList<ActivityManager.RunningAppProcessInfo>();
        ActivityManager.RunningAppProcessInfo processInfo = Mockito.mock(ActivityManager.RunningAppProcessInfo.class);
        processInfo.pid = android.os.Process.myPid();
        processInfo.processName = "test";
        processes.add(processInfo);
        when(manager.getRunningAppProcesses()).thenReturn(processes);

        when(assetManager.open(anyString())).thenReturn(Thread.currentThread().getContextClassLoader().getResourceAsStream("syncing-config.json"));

        when(application.getAssets()).thenReturn(assetManager);
        when(application.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(manager);

    }

    @Test
    public void testInit() {

//        SyncingInjection.init(application,"syncing-config.json");
    }
}
