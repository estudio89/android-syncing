package br.com.estudio89.syncing.manager;

import android.content.Context;
import android.content.SharedPreferences;
import br.com.estudio89.syncing.SyncManager;
import br.com.estudio89.syncing.TestUtil;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.serialization.JSONSerializer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.*;

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

    @Captor
    ArgumentCaptor<JSONObject> jsonCaptor;


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
        Assert.assertEquals(testSyncManager.getDateField().getName(), "pubDate");
        Assert.assertEquals(testSyncManager.getParentFields().size(), 1);
        Assert.assertTrue(testSyncManager.getParentFields().values().contains("parent_id"));
        HashMap<Field, SyncManager> childrenFields = testSyncManager.getChildrenFields();
        Assert.assertEquals(childrenFields.size(), 2);
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
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_MONTH, 20);

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
        List<OtherChildSyncModel> otherChildren = new ArrayList<OtherChildSyncModel>();
        otherChildren.add(new OtherChildSyncModel());
        item.setOtherChildren(otherChildren);

        JSONObject jsonObject = testSyncManager.serializeObject(item);

        Assert.assertEquals("{\"id\":5,\"pubDate\":\"2015-06-20T13:00:00.000-03:00\",\"name\":\"Luccas\",\"idClient\":2,\"other_children_objs\":[{\"other\":\"other\",\"nullField\":null}],\"parent_id\":10}", jsonObject.toString());

    }

    @Test
    public void testSaveNewObject() throws Exception {
        JSONObject jsonObject = new JSONObject("{\"id\":5,\"pubDate\":\"2015-06-20T13:00:00.000-03:00\",\"name\":\"Luccas\",\"idClient\":2,\"parent_id\":10,\"children_objs\":[],\"children_pagination\":{\"more\":true},\"other_children_objs\":[]}");

        ParentSyncModel parent = new ParentSyncModel();
        parent.setId(1L);
        TestSyncManager spyTestSyncManager = Mockito.spy(new TestSyncManager());
        Mockito.doReturn(childSyncManager).when(spyTestSyncManager).getNestedSyncManager(ChildSyncManager.class);
        Mockito.doReturn(null).when(spyTestSyncManager).findItem(Mockito.any(Long.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(Boolean.class), Mockito.any(JSONObject.class));
        Mockito.doReturn(parent).when(spyTestSyncManager).findParent(Mockito.eq(ParentSyncModel.class), Mockito.any(String.class));
        Mockito.doNothing().when(spyTestSyncManager).performSave(Mockito.any(TestSyncModel.class));
        Mockito.doNothing().when(spyTestSyncManager).deleteAllChildren(Mockito.any(Class.class), Mockito.anyString(), Mockito.anyLong());
        Mockito.doReturn(null).when(childSyncManager).saveNewData(Mockito.any(JSONArray.class), Mockito.any(String.class), Mockito.any(JSONObject.class), Mockito.any(Context.class));

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 19);
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        TestSyncModel oldestItem = new TestSyncModel();
        oldestItem.setPubDate(cal.getTime());
        spyTestSyncManager.setNestedManager(childSyncManager);
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

        // Checking if children objects were not deleted before being saved (they should not be deleted as this is a new item)
        Mockito.verify(spyTestSyncManager, Mockito.never()).deleteAllChildren(Mockito.any(Class.class), Mockito.anyString(), Mockito.anyLong());

        // Checking if children's syncManager was called
        Mockito.verify(childSyncManager, Mockito.times(1)).saveNewData(Mockito.any(JSONArray.class), Mockito.any(String.class), jsonCaptor.capture(), Mockito.any(Context.class));
        Assert.assertEquals(new JSONObject("{\"more\":true}").toString(), jsonCaptor.getValue().toString());
                Mockito.verify(childSyncManager, Mockito.times(1)).postEvent(Mockito.any(List.class), Mockito.any(AsyncBus.class), Mockito.any(Context.class));

    }

    @Test
    public void testSaveOldObject() throws Exception {
        JSONObject jsonObject = new JSONObject("{\"id\":5,\"pubDate\":\"2015-06-20T13:00:00.000-03:00\",\"name\":\"Luccas\",\"idClient\":2,\"parent_id\":10,\"children_objs\":[],\"children_pagination\":{\"more\":true},\"other_children_objs\":[]}");

        ParentSyncModel parent = new ParentSyncModel();
        parent.setId(1L);
        TestSyncModel oldItem = new TestSyncModel();
        oldItem.setId(2L);
        TestSyncManager spyTestSyncManager = Mockito.spy(new TestSyncManager());
        Mockito.doReturn(childSyncManager).when(spyTestSyncManager).getNestedSyncManager(ChildSyncManager.class);
        Mockito.doReturn(oldItem).when(spyTestSyncManager).findItem(Mockito.any(Long.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(Boolean.class), Mockito.any(JSONObject.class));
        Mockito.doReturn(parent).when(spyTestSyncManager).findParent(Mockito.eq(ParentSyncModel.class), Mockito.any(String.class));
        Mockito.doNothing().when(spyTestSyncManager).performSave(Mockito.any(TestSyncModel.class));
        Mockito.doNothing().when(spyTestSyncManager).deleteAllChildren(Mockito.any(Class.class), Mockito.anyString(), Mockito.anyLong());
        Mockito.doReturn(null).when(childSyncManager).saveNewData(Mockito.any(JSONArray.class), Mockito.any(String.class), Mockito.any(JSONObject.class), Mockito.any(Context.class));

        spyTestSyncManager.setNestedManager(childSyncManager);
        TestSyncModel item = spyTestSyncManager.saveObject(jsonObject, "deviceId", context);


        // Checking if attributes are correct
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 20);
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Assert.assertEquals(item.getIdServer(), 5);
        Assert.assertTrue(item.getId() == 2);
        Assert.assertEquals(item.getPubDate().getTime(), cal
                .getTime().getTime());
        Assert.assertEquals(item.getName(), "Luccas");
        Assert.assertEquals(item.getParent(), parent);


        // Checking if item was set as old
        Assert.assertFalse(item.isNew());

        // Checking if children objects were deleted before being saved
        Mockito.verify(spyTestSyncManager, Mockito.times(1)).deleteAllChildren(Mockito.any(Class.class), Mockito.anyString(), Mockito.anyLong());
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
        JSONObject jsonObject = new JSONObject("{\"id\":5,\"pubDate\":\"2015-06-20T13:00:00.000-03:00\",\"name\":\"Luccas\",\"idClient\":2,\"parent_id\":10,\"children_objs\":[],\"children_pagination\":{\"more\":true},\"other_children_objs\":[]}");

        ParentSyncModel parent = new ParentSyncModel();
        parent.setId(1L);
        TestSyncManager spyTestSyncManager = Mockito.spy(new TestSyncManager());
        Mockito.doReturn(childSyncManager).when(spyTestSyncManager).getNestedSyncManager(ChildSyncManager.class);
        Mockito.doReturn(null).when(spyTestSyncManager).findItem(Mockito.any(Long.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(Boolean.class), Mockito.any(JSONObject.class));
        Mockito.doReturn(parent).when(spyTestSyncManager).findParent(Mockito.eq(ParentSyncModel.class), Mockito.any(String.class));
        Mockito.doNothing().when(spyTestSyncManager).performSave(Mockito.any(TestSyncModel.class));
        Mockito.doReturn(null).when(childSyncManager).saveNewData(Mockito.any(JSONArray.class), Mockito.any(String.class), Mockito.any(JSONObject.class), Mockito.any(Context.class));

        TestSyncModel oldItem = new TestSyncModel();
        oldItem.setPubDate(new Date());
        spyTestSyncManager.setOldestInCache(oldItem);
        spyTestSyncManager.setNestedManager(childSyncManager);
        TestSyncModel item = spyTestSyncManager.saveObject(jsonObject, "deviceId", context);


        // Checking if attributes are correct
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 20);
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Assert.assertEquals(item.getIdServer(), 5);
        Assert.assertTrue(item.getId() == null);
        Assert.assertEquals(item.getPubDate().getTime(), cal
                .getTime().getTime());
        Assert.assertEquals(item.getName(), "Luccas");
        Assert.assertEquals(item.getParent(), parent);


        // Checking if item was set as old
        Assert.assertFalse(item.isNew());

        // Checking if children's syncManager was called
        Mockito.verify(childSyncManager, Mockito.times(1)).saveNewData(Mockito.any(JSONArray.class),Mockito.any(String.class), Mockito.any(JSONObject.class), Mockito.any(Context.class));
        Mockito.verify(childSyncManager, Mockito.times(1)).postEvent(Mockito.any(List.class), Mockito.any(AsyncBus.class), Mockito.any(Context.class));
    }

    @Test
    public void testSaveNewDataPaginationMore() throws Exception {
        /**
         * Tests the situation where the user is paginating and the server indicates there are still more items to be fetched.
         *
         * Expected results: boolean preference is saved, received objects are saved, no objects are deleted, no delete events are fired.
         */
        JSONArray newObjects = TestUtil.loadJsonArrayResource("syncmanager/save-new-data-more.json");
        JSONObject parameters = new JSONObject();
        parameters.put("more", true);
        parameters.put("paginationIdentifier", 1);


        SyncManager spyDeletedSyncManager = Mockito.spy(new TestSyncManager());
        TestSyncManager spyTestSyncManager = Mockito.spy(new TestSyncManager());
        Mockito.doNothing().when(spyTestSyncManager).saveBooleanPref(Mockito.any(String.class),Mockito.any(Boolean.class),Mockito.any(Context.class));
        Mockito.doReturn(null).when(spyTestSyncManager).getOldest();
        Mockito.doReturn(new TestSyncModel()).when(spyTestSyncManager).saveObject(Mockito.any(JSONObject.class),Mockito.any(String.class),Mockito.any(Context.class));
        Mockito.doReturn(spyDeletedSyncManager).when(spyTestSyncManager).getSyncManagerDeleted();

        List<TestSyncModel> savedObjects = spyTestSyncManager.saveNewData(newObjects,"",parameters, context);

        // Checking if boolean pref was saved
        Mockito.verify(spyTestSyncManager, Mockito.times(1)).saveBooleanPref("more.1",true, context);

        // Making sure no objects were deleted
        Mockito.verify(spyTestSyncManager, Mockito.times(0)).deleteAll();

        // Making sure all objects were saved
        Mockito.verify(spyTestSyncManager, Mockito.times(2)).saveObject(Mockito.any(JSONObject.class), Mockito.any(String.class), Mockito.any(Context.class));
        Assert.assertEquals(2, savedObjects.size());

        // Making sure the delete event was not posted.
        Mockito.verify(spyDeletedSyncManager, Mockito.times(0)).postEvent(Mockito.any(List.class),Mockito.any(AsyncBus.class), Mockito.any(Context.class));
    }

    @Test
    public void testSaveNewDataSyncDeleteCache() throws Exception {
        /**
         * Tests the situation where the user is syncing and the server asks that the cache is cleared.
         *
         * Expected results: boolean preference is set to true, cache is cleared, received objects are saved, delete event is fired.
         */
        JSONArray newObjects = TestUtil.loadJsonArrayResource("syncmanager/save-new-data-more.json");
        JSONObject parameters = new JSONObject();
        parameters.put("deleteCache", true);
        parameters.put("paginationIdentifier", 1);

        TestSyncModel oldItem = new TestSyncModel();
        Calendar olderDate = Calendar.getInstance();
        olderDate.set(Calendar.DAY_OF_MONTH, 19);
        olderDate.set(Calendar.MONTH, Calendar.JUNE);
        olderDate.set(Calendar.YEAR, 2015);
        olderDate.set(Calendar.HOUR_OF_DAY, 13);
        olderDate.set(Calendar.MINUTE, 0);
        olderDate.set(Calendar.SECOND, 0);
        olderDate.set(Calendar.MILLISECOND, 0);
        oldItem.setPubDate(olderDate.getTime());

        SyncManager spyDeletedSyncManager = Mockito.spy(new TestSyncManager());
        TestSyncManager spyTestSyncManager = Mockito.spy(new TestSyncManager());
        Mockito.doNothing().when(spyTestSyncManager).saveBooleanPref(Mockito.any(String.class),Mockito.any(Boolean.class),Mockito.any(Context.class));
        Mockito.doReturn(oldItem).when(spyTestSyncManager).getOldest();
        Mockito.doReturn(new ArrayList<TestSyncModel>()).when(spyTestSyncManager).listAll();
        Mockito.doNothing().when(spyTestSyncManager).deleteAll();
        Mockito.doReturn(new TestSyncModel()).when(spyTestSyncManager).saveObject(Mockito.any(JSONObject.class),Mockito.any(String.class),Mockito.any(Context.class));
        Mockito.doReturn(spyDeletedSyncManager).when(spyTestSyncManager).getSyncManagerDeleted();

        List<TestSyncModel> savedObjects = spyTestSyncManager.saveNewData(newObjects,"",parameters, context);

        // Checking if boolean pref was saved
        Mockito.verify(spyTestSyncManager, Mockito.times(1)).saveBooleanPref("more.1",true, context);

        // Making sure cache was cleared
        Mockito.verify(spyTestSyncManager, Mockito.times(1)).deleteAll();

        // Making sure all objects were saved
        Mockito.verify(spyTestSyncManager, Mockito.times(2)).saveObject(Mockito.any(JSONObject.class), Mockito.any(String.class), Mockito.any(Context.class));
        Assert.assertEquals(2, savedObjects.size());

        // Making sure the delete event was posted.
        Mockito.verify(spyDeletedSyncManager, Mockito.times(1)).postEvent(Mockito.any(List.class),Mockito.any(AsyncBus.class), Mockito.any(Context.class));
    }

    @Test
    public void testSaveNewDataSyncOldObject() throws Exception {
        /**
         * Tests the situation where the user is syncing (not paginating) and the server sends an object that is older that the oldest object in cache.
         *
         * Expected results: boolean preference is set (if there are older objects, there is more on the server), cache is not cleared, only one object is saved, delete event is not fired.
         */
        JSONArray newObjects = TestUtil.loadJsonArrayResource("syncmanager/save-new-data-more.json");
        JSONObject parameters = new JSONObject();
        parameters.put("deleteCache", false);
        parameters.put("paginationIdentifier", 1);

        TestSyncModel oldItem = new TestSyncModel();
        Calendar newerDate = Calendar.getInstance();
        newerDate.set(Calendar.DAY_OF_MONTH, 21);
        newerDate.set(Calendar.MONTH, Calendar.JUNE);
        newerDate.set(Calendar.YEAR, 2015);
        newerDate.set(Calendar.HOUR_OF_DAY, 13);
        newerDate.set(Calendar.MINUTE, 0);
        newerDate.set(Calendar.SECOND, 0);
        newerDate.set(Calendar.MILLISECOND, 0);
        oldItem.setPubDate(newerDate.getTime());

        SyncManager spyDeletedSyncManager = Mockito.spy(new TestSyncManager());
        TestSyncManager spyTestSyncManager = Mockito.spy(new TestSyncManager());
        Mockito.doNothing().when(spyTestSyncManager).saveBooleanPref(Mockito.any(String.class),Mockito.any(Boolean.class),Mockito.any(Context.class));
        Mockito.doReturn(oldItem).when(spyTestSyncManager).getOldest();
        Mockito.doReturn(new ArrayList<TestSyncModel>()).when(spyTestSyncManager).listAll();
        Mockito.doNothing().when(spyTestSyncManager).deleteAll();
        Mockito.doReturn(new TestSyncModel()).when(spyTestSyncManager).saveObject(Mockito.any(JSONObject.class),Mockito.any(String.class),Mockito.any(Context.class));
        Mockito.doReturn(spyDeletedSyncManager).when(spyTestSyncManager).getSyncManagerDeleted();

        List<TestSyncModel> savedObjects = spyTestSyncManager.saveNewData(newObjects,"", parameters, context);

        // Checking if boolean pref was saved
        Mockito.verify(spyTestSyncManager, Mockito.times(1)).saveBooleanPref(Mockito.any(String.class), Mockito.eq(true), Mockito.any(Context.class));

        // Making sure cache was not cleared
        Mockito.verify(spyTestSyncManager, Mockito.times(0)).deleteAll();

        // Making sure only one object was saved
        Mockito.verify(spyTestSyncManager, Mockito.times(1)).saveObject(Mockito.any(JSONObject.class), Mockito.any(String.class), Mockito.any(Context.class));
        Assert.assertEquals(1, savedObjects.size());

        // Making sure the delete event was not posted.
        Mockito.verify(spyDeletedSyncManager, Mockito.times(0)).postEvent(Mockito.any(List.class),Mockito.any(AsyncBus.class), Mockito.any(Context.class));
    }

    @Test
    public void testProcessSendResponse() throws Exception {
        JSONArray sendResponse = TestUtil.loadJsonArrayResource("syncmanager/send-response.json");

        TestSyncModel existingItem = Mockito.spy(new TestSyncModel());

        TestSyncManager spyTestSyncManager = Mockito.spy(new TestSyncManager());
        Mockito.doReturn(existingItem).when(spyTestSyncManager).findItem(Mockito.any(Long.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.eq(true));
        Mockito.doNothing().when(existingItem).save();

        spyTestSyncManager.processSendResponse(sendResponse);

        // Checking if modified was set to false
        Mockito.verify(existingItem, Mockito.times(1)).setModified(false);

        // Making sure server id was set
        Mockito.verify(existingItem, Mockito.times(1)).setIdServer(2);

        // Making sure object was saved
        Mockito.verify(existingItem, Mockito.times(1)).save();
    }

    @Test
    public void testReadOnlySyncManager() throws Exception {
        SyncManager sm = new TestReadOnlySyncManager();
        sm.saveNewData(new JSONArray(), "", new JSONObject(), context);
    }
}
