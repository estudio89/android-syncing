package br.com.estudio89.syncing.manager;

import android.content.Context;
import android.content.SharedPreferences;
import br.com.estudio89.syncing.SyncManager;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.serialization.JSONSerializer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by luccascorrea on 6/21/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SyncManagerTests {

    TestSyncManager testSyncManager;

    @Mock
    ChildSyncManager childSyncManager;

    @Mock
    Context context;

    @Mock
    SharedPreferences sharedPreferences;

    @Mock
    SharedPreferences.Editor editor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testSyncManager = new TestSyncManager();

        // Mocking shared preferences
        Mockito.when(sharedPreferences.edit()).thenReturn(editor);
        Mockito.when(context.getSharedPreferences(Mockito.any(String.class), Mockito.any(Integer.class))).thenReturn(sharedPreferences);
    }

    @Test
    public void testVerifyFields() throws Exception {
        Assert.assertEquals(testSyncManager.getDateField().getName(),"pubDate");
        Assert.assertEquals(testSyncManager.getParentField().getName(),"parent");
        Assert.assertEquals(testSyncManager.getParentFieldName(),"parent_id");
        HashMap<Field, SyncManager> childrenFields = testSyncManager.getChildrenFields();
        Assert.assertEquals(childrenFields.size(), 1);
    }

    @Test
    public void testGetDate() throws Exception {
        TestSyncModel item = new TestSyncModel();
        item.pubDate = new Date();
        Assert.assertEquals(testSyncManager.getDate(item), item.pubDate);
    }

    @Test
    public void testSerializeObject() throws Exception {
        JSONSerializer<TestSyncModel> serializer = new JSONSerializer<TestSyncModel>(TestSyncModel.class);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 20);
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.HOUR, 13);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        ParentSyncModel parent = new ParentSyncModel();
        parent.setIdServer(10);
        parent.setId((long) 1);

        TestSyncModel item = new TestSyncModel();
        item.setId((long) 1);
        item.pubDate = cal.getTime();
        item.name = "Luccas";
        item.parent = parent;
        item.setIdServer((long) 5);
        item.setId((long) 2);

        JSONObject jsonObject = testSyncManager.serializeObject(item);

        Assert.assertEquals("{\"id\":5,\"pubDate\":\"2015-06-20T13:00:00.000-03:00\",\"name\":\"Luccas\",\"idClient\":2,\"parent_id\":10}", jsonObject.toString());

    }

    @Test
    public void testSaveNewObject() throws Exception {
        JSONObject jsonObject = new JSONObject("{\"id\":5,\"pubDate\":\"2015-06-20T13:00:00.000-03:00\",\"name\":\"Luccas\",\"idClient\":2,\"parent_id\":10,\"children_objs\":[]}");

        ParentSyncModel parent = new ParentSyncModel();
        TestSyncManager spyTestSyncManager = Mockito.spy(new TestSyncManager());
        Mockito.doReturn(childSyncManager).when(spyTestSyncManager).getNestedSyncManager(ChildSyncManager.class);
        Mockito.doReturn(null).when(spyTestSyncManager).findItem(Mockito.any(Long.class), Mockito.any(String.class));
        Mockito.doReturn(parent).when(spyTestSyncManager).findParent(Mockito.eq(ParentSyncModel.class), Mockito.any(String.class));
        Mockito.doNothing().when(spyTestSyncManager).performSave(Mockito.any(TestSyncModel.class));
        Mockito.doReturn(null).when(childSyncManager).saveNewData(Mockito.any(JSONArray.class), Mockito.any(String.class), Mockito.any(JSONObject.class), Mockito.any(Context.class));

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 19);
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.HOUR, 13);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        TestSyncModel oldestItem = new TestSyncModel();
        oldestItem.setPubDate(cal.getTime());
        spyTestSyncManager.verifyFields();
        spyTestSyncManager.setOldestInCache(oldestItem);
        TestSyncModel item = spyTestSyncManager.saveObject(jsonObject, "deviceId", context);


        // Checking if attributes are correct
        cal.set(Calendar.DAY_OF_MONTH, 20);

        Assert.assertEquals(item.getIdServer(), 5);
        Assert.assertTrue(item.getId() == null);
        Assert.assertEquals(item.getPubDate().getTime(), cal.getTime().getTime());
        Assert.assertEquals(item.getName(), "Luccas");
        Assert.assertEquals(item.getParent(), parent);


        // Checking if item was set as new
        Assert.assertTrue(item.isNew());

        // Checking if children's syncManager was called
        Mockito.verify(childSyncManager, Mockito.times(1)).saveNewData(Mockito.any(JSONArray.class),Mockito.any(String.class), Mockito.any(JSONObject.class), Mockito.any(Context.class));
        Mockito.verify(childSyncManager, Mockito.times(1)).postEvent(Mockito.any(List.class), Mockito.any(AsyncBus.class), Mockito.any(Context.class));
    }

    @Test
    public void testSaveOldObject() throws Exception {
        JSONObject jsonObject = new JSONObject("{\"id\":5,\"pubDate\":\"2015-06-20T13:00:00.000-03:00\",\"name\":\"Luccas\",\"idClient\":2,\"parent_id\":10,\"children_objs\":[]}");

        ParentSyncModel parent = new ParentSyncModel();
        TestSyncModel oldItem = new TestSyncModel();
        TestSyncManager spyTestSyncManager = Mockito.spy(new TestSyncManager());
        Mockito.doReturn(childSyncManager).when(spyTestSyncManager).getNestedSyncManager(ChildSyncManager.class);
        Mockito.doReturn(oldItem).when(spyTestSyncManager).findItem(Mockito.any(Long.class), Mockito.any(String.class));
        Mockito.doReturn(parent).when(spyTestSyncManager).findParent(Mockito.eq(ParentSyncModel.class), Mockito.any(String.class));
        Mockito.doNothing().when(spyTestSyncManager).performSave(Mockito.any(TestSyncModel.class));
        Mockito.doReturn(null).when(childSyncManager).saveNewData(Mockito.any(JSONArray.class),Mockito.any(String.class), Mockito.any(JSONObject.class), Mockito.any(Context.class));

        spyTestSyncManager.verifyFields();
        TestSyncModel item = spyTestSyncManager.saveObject(jsonObject, "deviceId", context);


        // Checking if attributes are correct
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 20);
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.HOUR, 13);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Assert.assertEquals(item.getIdServer(), 5);
        Assert.assertTrue(item.getId() == null);
        Assert.assertEquals(item.getPubDate().getTime(), cal.getTime().getTime());
        Assert.assertEquals(item.getName(), "Luccas");
        Assert.assertEquals(item.getParent(), parent);


        // Checking if item was set as old
        Assert.assertFalse(item.isNew());

        // Checking if children's syncManager was called
        Mockito.verify(childSyncManager, Mockito.times(1)).saveNewData(Mockito.any(JSONArray.class),Mockito.any(String.class), Mockito.any(JSONObject.class), Mockito.any(Context.class));
        Mockito.verify(childSyncManager, Mockito.times(1)).postEvent(Mockito.any(List.class), Mockito.any(AsyncBus.class), Mockito.any(Context.class));
    }

    @Test
    public void testSaveNewOldObject() throws Exception {
        /**
         * Tests the situation where an object that is older than the oldest object in cache is sent by the server.
         * This situation would happen if the user were paginating.
         */
        JSONObject jsonObject = new JSONObject("{\"id\":5,\"pubDate\":\"2015-06-20T13:00:00.000-03:00\",\"name\":\"Luccas\",\"idClient\":2,\"parent_id\":10,\"children_objs\":[]}");

        ParentSyncModel parent = new ParentSyncModel();
        TestSyncManager spyTestSyncManager = Mockito.spy(new TestSyncManager());
        Mockito.doReturn(childSyncManager).when(spyTestSyncManager).getNestedSyncManager(ChildSyncManager.class);
        Mockito.doReturn(null).when(spyTestSyncManager).findItem(Mockito.any(Long.class), Mockito.any(String.class));
        Mockito.doReturn(parent).when(spyTestSyncManager).findParent(Mockito.eq(ParentSyncModel.class), Mockito.any(String.class));
        Mockito.doNothing().when(spyTestSyncManager).performSave(Mockito.any(TestSyncModel.class));
        Mockito.doReturn(null).when(childSyncManager).saveNewData(Mockito.any(JSONArray.class),Mockito.any(String.class), Mockito.any(JSONObject.class), Mockito.any(Context.class));

        TestSyncModel oldItem = new TestSyncModel();
        oldItem.setPubDate(new Date());
        spyTestSyncManager.setOldestInCache(oldItem);
        spyTestSyncManager.verifyFields();
        TestSyncModel item = spyTestSyncManager.saveObject(jsonObject, "deviceId", context);


        // Checking if attributes are correct
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 20);
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.HOUR, 13);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Assert.assertEquals(item.getIdServer(), 5);
        Assert.assertTrue(item.getId() == null);
        Assert.assertEquals(item.getPubDate().getTime(), cal.getTime().getTime());
        Assert.assertEquals(item.getName(), "Luccas");
        Assert.assertEquals(item.getParent(), parent);


        // Checking if item was set as old
        Assert.assertFalse(item.isNew());

        // Checking if children's syncManager was called
        Mockito.verify(childSyncManager, Mockito.times(1)).saveNewData(Mockito.any(JSONArray.class),Mockito.any(String.class), Mockito.any(JSONObject.class), Mockito.any(Context.class));
        Mockito.verify(childSyncManager, Mockito.times(1)).postEvent(Mockito.any(List.class), Mockito.any(AsyncBus.class), Mockito.any(Context.class));
    }

    public void testSaveNewDataPaginationMore() throws Exception {

    }
}
