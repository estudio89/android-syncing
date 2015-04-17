package br.com.estudio89.syncing;

import android.app.Application;
import android.content.res.AssetManager;
import br.com.estudio89.syncing.bus.AsyncBus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by luccascorrea on 11/28/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SyncConfigTests {

    @Mock
    Application application;

    SyncConfig syncConfig;

    @Mock
    AsyncBus bus;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

    }

    @Test
    public void testLoadSettings() throws Exception {
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        when(assetManager.open(anyString())).thenReturn(Thread.currentThread().getContextClassLoader().getResourceAsStream("syncing-config.json"));
        when(application.getAssets()).thenReturn(assetManager);
        syncConfig = new SyncConfig(application, bus);
        syncConfig.setConfigFile("syncing-config.json");

        // GetDataUrl
        Assert.assertEquals("http://api.estudio89.com.br/send-data", syncConfig.getGetDataUrl());

        // SendDataUrl
        Assert.assertEquals("http://api.estudio89.com.br/get-data", syncConfig.getSendDataUrl());

        // AuthenticateUrl
        Assert.assertEquals("http://api.estudio89.com.br/auth", syncConfig.getAuthenticateUrl());

        // AccountType
        Assert.assertEquals("br.com.estudio89", syncConfig.getAccountType());

        // Encryption
        Assert.assertEquals(true, syncConfig.isEncryptionActive());
        Assert.assertEquals("1234", syncConfig.getEncryptionPassword());

        // Syncmanagers
        Assert.assertEquals(1,syncConfig.getSyncManagers().size());
        Assert.assertEquals(TestSyncManager.class,syncConfig.getSyncManagers().get(0).getClass());
        Assert.assertEquals(TestSyncManager.class, syncConfig.getSyncManager("test").getClass());
        Assert.assertEquals(TestSyncManager.class, syncConfig.getSyncManagerByResponseId("test_id").getClass());
        Assert.assertEquals("http://api.estudio89.com.br/test/", syncConfig.getGetDataUrlForModel("test"));

    }

}
