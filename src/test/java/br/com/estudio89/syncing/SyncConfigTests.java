package br.com.estudio89.syncing;

import android.app.Application;
import android.content.res.AssetManager;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.manager.TestSyncManager;
import org.json.JSONObject;
import org.junit.Assert;
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

    @Mock
    SyncManager syncManagerRegistros;

    @Mock
    SyncManager syncManagerEmpresas;

    @Mock
    SyncManager syncManagerFormularios;

    List<SyncManager> syncManagers;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        syncConfig = Mockito.spy(new SyncConfig(application, bus));
        syncManagers = new ArrayList<SyncManager>();

        Mockito.when(syncManagerRegistros.getIdentifier()).thenReturn("registros");
        Mockito.when(syncManagerRegistros.hasTimestamp()).thenReturn(true);
        Mockito.when(syncManagerEmpresas.getIdentifier()).thenReturn("empresas");
        Mockito.when(syncManagerEmpresas.hasTimestamp()).thenReturn(true);
        Mockito.when(syncManagerFormularios.getIdentifier()).thenReturn("formularios");
        Mockito.when(syncManagerFormularios.hasTimestamp()).thenReturn(true);

        syncManagers.add(syncManagerRegistros);
        syncManagers.add(syncManagerEmpresas);
        syncManagers.add(syncManagerFormularios);

        Mockito.doReturn(new JSONObject("{\"registros\":\"777\"}")).when(syncConfig).getTimestamp("registros");
        Mockito.doReturn(new JSONObject("{\"empresas\":\"778\"}")).when(syncConfig).getTimestamp("empresas");
        Mockito.doReturn(new JSONObject("{\"formularios\":\"779\"}")).when(syncConfig).getTimestamp("formularios");
    }

    @Test
    public void testLoadSettings() throws Exception {
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        when(assetManager.open(anyString())).thenReturn(Thread.currentThread().getContextClassLoader().getResourceAsStream("syncing-config.json"));
        when(application.getAssets()).thenReturn(assetManager);

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

    @Test
    public void testGetTimestamps() throws Exception {
        Mockito.doReturn(syncManagers).when(syncConfig).getSyncManagers();
        JSONObject timestamps = syncConfig.getTimestamps();

        Assert.assertEquals(new JSONObject("{\n" +
                "\t  \"registros\":\"777\",\n" +
                "\t  \"empresas\":\"778\",\n" +
                "\t  \"formularios\":\"779\"\n" +
                "\t}").toString(), timestamps.toString());
    }

    @Test
    public void testUserNeverSynced() throws Exception {
        Mockito.doReturn(syncManagers).when(syncConfig).getSyncManagers();
        Mockito.doReturn(syncManagerEmpresas).when(syncConfig).getSyncManager("empresas");
        Mockito.doReturn(syncManagerRegistros).when(syncConfig).getSyncManager("registros");
        Mockito.doReturn(syncManagerFormularios).when(syncConfig).getSyncManager("formularios");
        Boolean neverSynced = syncConfig.userNeverSynced();
        Assert.assertEquals(false, neverSynced);

        Mockito.doReturn(new JSONObject("{\"registros\":\"\"}")).when(syncConfig).getTimestamp("registros");
        Mockito.doReturn(new JSONObject("{\"empresas\":\"\"}")).when(syncConfig).getTimestamp("empresas");
        Mockito.doReturn(new JSONObject("{\"formularios\":\"\"}")).when(syncConfig).getTimestamp("formularios");

        neverSynced = syncConfig.userNeverSynced();
        Assert.assertEquals(true, neverSynced);

        Mockito.doReturn(new JSONObject("{\"registros\":\"777\"}")).when(syncConfig).getTimestamp("registros");
        Mockito.doReturn(new JSONObject("{\"empresas\":\"\"}")).when(syncConfig).getTimestamp("empresas");
        Mockito.doReturn(new JSONObject("{\"formularios\":\"\"}")).when(syncConfig).getTimestamp("formularios");

        neverSynced = syncConfig.userNeverSynced();
        Assert.assertEquals(true, neverSynced);
    }

}
