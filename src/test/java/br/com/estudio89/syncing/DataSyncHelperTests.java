package br.com.estudio89.syncing;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import br.com.estudio89.syncing.bus.AsyncBus;
import org.json.JSONArray;
import org.json.JSONException;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by luccascorrea on 11/27/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DataSyncHelperTests {

    @Captor
    ArgumentCaptor<String> stringCaptor;

    @Captor
    ArgumentCaptor<JSONObject> jsonCaptor;

    @Captor
    ArgumentCaptor<List<String>> listCaptor;

    @Captor
    ArgumentCaptor<JSONArray> arrayCaptor;

    @Mock
    ServerComm serverComm;

    @Mock
    Application application;

    @Mock
    SyncConfig syncConfig;

    CustomTransactionManager customTransactionManager;

    DataSyncHelper dataSyncHelper = new DataSyncHelper();

    ThreadChecker threadChecker = new ThreadChecker();

    @Mock
    SyncManager syncManagerRegistros;

    @Mock
    SyncManager syncManagerEmpresas;

    @Mock
    SyncManager syncManagerFormularios;

    @Mock
    AsyncBus bus;

    @Mock
    SQLiteDatabase database;

    List<String> modifiedFiles = new ArrayList<String>();

    @Before public void setUp() throws Exception{
        initMocks(this);

        // AppContext
        Mockito.when(application.openOrCreateDatabase(Mockito.anyString(),Mockito.eq(Context.MODE_PRIVATE),Mockito.isNull(SQLiteDatabase.CursorFactory.class))).thenReturn(database);

        // SyncManager
            // Registros
        Mockito.when(syncManagerRegistros.getIdentifier()).thenReturn("registros");
        Mockito.when(syncManagerRegistros.saveNewData(Mockito.<JSONArray>any(),Mockito.anyString())).thenReturn(new ArrayList());
        JSONArray registrosModified = null;
        try {
            registrosModified = loadJsonArrayResource("modified-data-registros.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Mockito.when(syncManagerRegistros.getModifiedData()).thenReturn(registrosModified);
        Mockito.when(syncManagerRegistros.shouldSendSingleObject()).thenReturn(false);

        modifiedFiles.add("imagem1.jpg");
        modifiedFiles.add("imagem2.jpg");
        modifiedFiles.add("imagem3.jpg");
        Mockito.when(syncManagerRegistros.getModifiedFiles()).thenReturn(modifiedFiles);
        Mockito.when(syncManagerRegistros.getResponseIdentifier()).thenReturn("registros_id");
        Mockito.when(syncManagerRegistros.getModifiedFilesForObject(Mockito.any(JSONObject.class))).thenReturn(modifiedFiles.subList(0,1),modifiedFiles.subList(1,3));
        Mockito.when(syncManagerRegistros.hasModifiedData()).thenReturn(true);
            // Empresas
        Mockito.when(syncManagerEmpresas.getIdentifier()).thenReturn("empresas");
        Mockito.when(syncManagerEmpresas.saveNewData(Mockito.<JSONArray>any(),Mockito.anyString())).thenReturn(new ArrayList());
        JSONArray empresasModified = null;
        try {
            empresasModified = loadJsonArrayResource("modified-data-empresas.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Mockito.when(syncManagerEmpresas.getModifiedData()).thenReturn(empresasModified);
        Mockito.when(syncManagerEmpresas.shouldSendSingleObject()).thenReturn(false);
        Mockito.when(syncManagerEmpresas.getModifiedFiles()).thenReturn(new ArrayList<String>());
        Mockito.when(syncManagerEmpresas.getResponseIdentifier()).thenReturn("empresas_id");
        Mockito.when(syncManagerEmpresas.hasModifiedData()).thenReturn(true);
            // Formularios
        Mockito.when(syncManagerFormularios.getIdentifier()).thenReturn("formularios");
        Mockito.when(syncManagerFormularios.saveNewData(Mockito.<JSONArray>any(),Mockito.anyString())).thenReturn(new ArrayList());
        Mockito.when(syncManagerFormularios.getModifiedData()).thenReturn(new JSONArray());
        Mockito.when(syncManagerFormularios.shouldSendSingleObject()).thenReturn(false);
        Mockito.when(syncManagerFormularios.getModifiedFiles()).thenReturn(new ArrayList<String>());
        Mockito.when(syncManagerFormularios.getResponseIdentifier()).thenReturn("formularios_id");
        Mockito.when(syncManagerFormularios.hasModifiedData()).thenReturn(false);


        // SyncConfig
        Mockito.when(syncConfig.getAuthToken()).thenReturn("123");
        Mockito.when(syncConfig.getTimestamp()).thenReturn("666");
        Mockito.when(syncConfig.getDatabase()).thenReturn(database);
        Mockito.when(syncConfig.getGetDataUrl()).thenReturn("http://127.0.0.1:8000/api/get-data/");
        Mockito.when(syncConfig.getSendDataUrl()).thenReturn("http://127.0.0.1:8000/api/send-data/");
        Mockito.when(syncConfig.getDeviceId()).thenReturn("asdasda");
        List<SyncManager> syncManagers = new ArrayList<SyncManager>();
        syncManagers.add(syncManagerRegistros);
        syncManagers.add(syncManagerEmpresas);
        syncManagers.add(syncManagerFormularios);
        Mockito.when(syncConfig.getSyncManagers()).thenReturn(syncManagers);
        Mockito.when(syncConfig.getSyncManager("registros")).thenReturn(syncManagerRegistros);
        Mockito.when(syncConfig.getSyncManager("empresas")).thenReturn(syncManagerEmpresas);
        Mockito.when(syncConfig.getSyncManager("formularios")).thenReturn(syncManagerFormularios);
        Mockito.when(syncConfig.getSyncManagerByResponseId("registros_id")).thenReturn(syncManagerRegistros);
        Mockito.when(syncConfig.getSyncManagerByResponseId("empresas_id")).thenReturn(syncManagerEmpresas);
        Mockito.when(syncConfig.getSyncManagerByResponseId("formularios_id")).thenReturn(syncManagerFormularios);

        try {
            Mockito.when(syncConfig.getGetDataUrlForModel("registros")).thenReturn("http://127.0.0.1:8000/api/get-data/registros/");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        // CustomTransactionManager
        customTransactionManager = new CustomTransactionManager();

        // ServerComm
        JSONObject jsonGetResponse = null;
        JSONObject jsonSendResponse = null;
        JSONObject jsonGetResponseForModel = null;
        try {
            jsonGetResponse = loadJsonResource("get-data-response.json");
            jsonSendResponse = loadJsonResource("send-data-response.json");
            jsonGetResponseForModel = loadJsonResource("get-data-for-model-response.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Mockito.when(serverComm.post(Mockito.eq(syncConfig.getGetDataUrl()),Mockito.any(JSONObject.class))).thenReturn(jsonGetResponse);
        Mockito.when(serverComm.post(Mockito.eq(syncConfig.getSendDataUrl()),Mockito.any(JSONObject.class),Mockito.anyList())).thenReturn(jsonSendResponse);
        try {
            Mockito.when(serverComm.post(Mockito.eq(syncConfig.getGetDataUrlForModel("registros")),Mockito.any(JSONObject.class))).thenReturn(jsonGetResponseForModel);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // DataSyncHelper - Injeção manual
        dataSyncHelper.appContext = application;
        dataSyncHelper.bus = bus;
        dataSyncHelper.serverComm = serverComm;
        dataSyncHelper.syncConfig = syncConfig;
        dataSyncHelper.transactionManager = customTransactionManager;
        dataSyncHelper.threadChecker = threadChecker;

    }

    /**
     * Carrega um arquivo da pasta resources e transforma-o em uma string.
     *
     * @param filename nome do arquivo.
     * @return string com conteúdo.
     * @throws IOException
     * @throws JSONException
     */
    private String loadResource(String filename) throws IOException, JSONException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        inputStream.read(buffer);
        inputStream.close();

        String jsonString = new String(buffer, "UTF-8");
        return jsonString;
    }

    /**
     * Carrega um objeto json de teste da pasta resources.
     *
     * @param filename nome do arquivo.
     * @return objeto JSON correspondente.
     * @throws IOException
     * @throws JSONException
     */
    private JSONObject loadJsonResource(String filename) throws IOException, JSONException {
        String jsonString = loadResource(filename);
        return new JSONObject(jsonString);
    }

    /**
     * Carrega um array json de teste da pasta resources.
     *
     * @param filename nome do arquivo.
     * @return array json correspondente.
     * @throws IOException
     * @throws JSONException
     */
    private JSONArray loadJsonArrayResource(String filename) throws IOException, JSONException {
        String jsonString = loadResource(filename);
        return new JSONArray(jsonString);
    }


    @Test
    public void testGetDataFromServer() throws Exception {


        boolean completed = dataSyncHelper.getDataFromServer();

        // Verificando se post foi realizado corretamente
        JSONObject getRequestJSON = loadJsonResource("get-data-request.json");
        Mockito.verify(serverComm).post(stringCaptor.capture(), jsonCaptor.capture());
        Assert.assertEquals("http://127.0.0.1:8000/api/get-data/", stringCaptor.getValue());
        Assert.assertEquals(getRequestJSON.toString(),jsonCaptor.getValue().toString());

        // Verificando se os dados foram salvos
        JSONObject getResponseJSON = loadJsonResource("get-data-response.json");
        JSONArray registrosArray = getResponseJSON.getJSONArray("registros");
        JSONArray empresasArray = getResponseJSON.getJSONArray("empresas");
        JSONArray formulariosArray = getResponseJSON.getJSONArray("formularios");

        Assert.assertEquals(true,customTransactionManager.wasSuccesful());
        Mockito.verify(syncManagerRegistros).saveNewData(arrayCaptor.capture(),Mockito.anyString());
        Assert.assertEquals(registrosArray.toString(),arrayCaptor.getValue().toString());
        Mockito.verify(syncManagerEmpresas).saveNewData(arrayCaptor.capture(),Mockito.anyString());
        Assert.assertEquals(empresasArray.toString(),arrayCaptor.getValue().toString());
        Mockito.verify(syncManagerFormularios).saveNewData(arrayCaptor.capture(),Mockito.anyString());
        Assert.assertEquals(formulariosArray.toString(),arrayCaptor.getValue().toString());


        // Verificando se o post do evento foi realizado
        Mockito.verify(syncManagerRegistros).postEvent(Mockito.any(List.class),Mockito.eq(bus));

        // Verificando se o timestamp foi salvo
        Mockito.verify(syncConfig).setTimestamp("777");

        // Get data realizado
        Assert.assertEquals(true, completed);
    }

    @Test
    public void testGetDataFromServerFail() throws Exception {

        // Thread interrompido
        ThreadChecker threadChecker = Mockito.mock(ThreadChecker.class);
        Mockito.when(threadChecker.isValidThreadId(Mockito.anyString())).thenReturn(false);
        dataSyncHelper.threadChecker = threadChecker;
        boolean completed = dataSyncHelper.getDataFromServer();

        // Assegurando que o banco de dados não fez commit
        Mockito.verify(database, Mockito.never()).setTransactionSuccessful();

        // Assegurando que o timestamp não foi salvo
        Mockito.verify(syncConfig, Mockito.never()).setTimestamp(Mockito.anyString());

        // Get data não realizado
        Assert.assertEquals(false, completed);
    }

    @Test
    public void testGetDataFromServerForModel() throws Exception {
        JSONObject parameters = new JSONObject();
        parameters.put("newest_id",5);

        boolean completed = dataSyncHelper.getDataFromServer("registros",parameters);

        // Conferindo se o post foi realizado corretamente
        JSONObject getRequestFormModelJSON = loadJsonResource("get-data-for-model-request.json");
        Mockito.verify(serverComm).post(stringCaptor.capture(), jsonCaptor.capture());
        Assert.assertEquals("http://127.0.0.1:8000/api/get-data/registros/", stringCaptor.getValue());
        Assert.assertEquals(getRequestFormModelJSON.toString(),jsonCaptor.getValue().toString());

        // Conferindo se os dados foram salvos
        JSONObject getResponseJSON = loadJsonResource("get-data-for-model-response.json");
        JSONArray registrosArray = getResponseJSON.getJSONArray("registros");
        JSONArray empresasArray = getResponseJSON.getJSONArray("empresas");

        Assert.assertEquals(true,customTransactionManager.wasSuccesful());
        Mockito.verify(syncManagerRegistros).saveNewData(arrayCaptor.capture(),Mockito.anyString());
        Assert.assertEquals(registrosArray.toString(),arrayCaptor.getValue().toString());
        Mockito.verify(syncManagerEmpresas).saveNewData(arrayCaptor.capture(),Mockito.anyString());
        Assert.assertEquals(empresasArray.toString(),arrayCaptor.getValue().toString());
        Mockito.verify(syncManagerFormularios, Mockito.never()).saveNewData(Mockito.any(JSONArray.class),Mockito.anyString());

        // Assegurando que o timestamp não foi salvo
        Mockito.verify(syncConfig, Mockito.never()).setTimestamp(Mockito.anyString());

        // GetData realizado
        Assert.assertEquals(true, completed);
    }

    @Test
    public void testGetDataFromServerForModelFail() throws Exception {
        // Thread interrompido
        ThreadChecker threadChecker = Mockito.mock(ThreadChecker.class);
        Mockito.when(threadChecker.isValidThreadId(Mockito.anyString())).thenReturn(false);
        JSONObject parameters = new JSONObject();
        parameters.put("newest_id",5);
        dataSyncHelper.threadChecker = threadChecker;

        boolean completed = dataSyncHelper.getDataFromServer("registros",parameters);

        // Assegurando que o banco de dados não fez commit
        Mockito.verify(database, Mockito.never()).setTransactionSuccessful();

        // Assegurando que o timestamp não foi salvo
        Mockito.verify(syncConfig, Mockito.never()).setTimestamp(Mockito.anyString());

        // Get data não realizado
        Assert.assertEquals(false, completed);
    }

    /**
     * Testa o envio de todos os itens de uma vez só.
     *
     * @throws Exception
     */
    @Test
    public void testSendDataToServerMultiple() throws Exception {
        boolean completed = dataSyncHelper.sendDataToServer();

        // Verificando se post foi realizado corretamente
        JSONObject sendRequestJSON = loadJsonResource("send-data-request.json");
        Mockito.verify(serverComm).post(stringCaptor.capture(), jsonCaptor.capture(),listCaptor.capture());
        Assert.assertEquals("http://127.0.0.1:8000/api/send-data/", stringCaptor.getValue());
        Assert.assertEquals(sendRequestJSON.toString(),jsonCaptor.getValue().toString());
        Assert.assertEquals(true,listCaptor.getValue().equals(modifiedFiles));

        // Verificando se os dados foram atualizados
        JSONObject sendResponseJson = loadJsonResource("send-data-response.json");
        JSONArray registrosArray = sendResponseJson.getJSONArray("registros_id");
        JSONArray empresasResponseArray = sendResponseJson.getJSONArray("empresas_id");

        Assert.assertEquals(true,customTransactionManager.wasSuccesful());

        Mockito.verify(syncManagerRegistros).processSendResponse(arrayCaptor.capture());
        Assert.assertEquals(registrosArray.toString(), arrayCaptor.getValue().toString());
        Mockito.verify(syncManagerEmpresas).processSendResponse(arrayCaptor.capture());
        Assert.assertEquals(empresasResponseArray.toString(), arrayCaptor.getValue().toString());

        // Verificando se dados novos foram salvos
        JSONArray newEmpresasArray = sendResponseJson.getJSONArray("empresas");
        Mockito.verify(syncManagerEmpresas).saveNewData(arrayCaptor.capture(),Mockito.anyString());
        Assert.assertEquals(newEmpresasArray.toString(),arrayCaptor.getValue().toString());

        // Verificando se o post do evento foi realizado apenas para os novos dados
        Mockito.verify(syncManagerRegistros, Mockito.never()).postEvent(Mockito.any(List.class),Mockito.eq(bus));
        Mockito.verify(syncManagerEmpresas).postEvent(Mockito.any(List.class),Mockito.eq(bus));

        // Verificando se o timestamp foi salvo
        Mockito.verify(syncConfig).setTimestamp("777");

        // Send data realizado
        Assert.assertEquals(true, completed);

    }

    /**
     * Testa o envio de um item por vez para um determinado model.
     *
     * @throws Exception
     */
    @Test
    public void testSendDataToServerSingle() throws Exception {
        Mockito.when(syncManagerRegistros.shouldSendSingleObject()).thenReturn(true);

        JSONObject firstResponse = loadJsonResource("send-data-response-first.json");
        JSONObject secondResponse = loadJsonResource("send-data-response-second.json");
        JSONObject thirdResponse = loadJsonResource("send-data-response-third.json");
        // Nos posts subsequentes, diferentes respostas são devolvidas pelo servidor
        Mockito.when(serverComm.post(Mockito.eq(syncConfig.getSendDataUrl()),Mockito.any(JSONObject.class),Mockito.anyList())).thenReturn(firstResponse, secondResponse, thirdResponse);
        // Após enviar todos os dados, o syncManagerRegistros avisa que não possui mais dados
        Mockito.when(syncManagerRegistros.hasModifiedData()).thenReturn(true,  true,true,false);
        boolean completed = dataSyncHelper.sendDataToServer();

        // Verificando se foram realizados múltiplos posts
        JSONObject sendRequestJSON = loadJsonResource("send-data-request.json");

        Mockito.verify(serverComm, Mockito.times(3)).post(stringCaptor.capture(), jsonCaptor.capture(),listCaptor.capture());
        List<String> capturedUrls = stringCaptor.getAllValues();
        List<JSONObject> capturedData = jsonCaptor.getAllValues();
        List<List<String>> capturedFiles = listCaptor.getAllValues();

            // Primeiro post - registro 1
        JSONObject firstRequest = loadJsonResource("send-data-request-first.json");
        Assert.assertEquals("http://127.0.0.1:8000/api/send-data/", capturedUrls.get(0));
        Assert.assertEquals(firstRequest.toString(),capturedData.get(0).toString());
        Assert.assertEquals(true, capturedFiles.get(0).equals(modifiedFiles.subList(0,1)));

            // Segundo post - registro 2
        JSONObject secondRequest = loadJsonResource("send-data-request-second.json");
        Assert.assertEquals("http://127.0.0.1:8000/api/send-data/", capturedUrls.get(1));
        Assert.assertEquals(secondRequest.toString(),capturedData.get(1).toString());
        Assert.assertEquals(true, capturedFiles.get(1).equals(modifiedFiles.subList(1,3)));

            // Terceiro post - empresas
        JSONObject thirdRequest = loadJsonResource("send-data-request-third.json");
        Assert.assertEquals("http://127.0.0.1:8000/api/send-data/", capturedUrls.get(2));
        Assert.assertEquals(thirdRequest.toString(),capturedData.get(2).toString());
        Assert.assertEquals(true, capturedFiles.get(2).equals(new ArrayList<String>()));

        // Verificando se os dados foram atualizados

        Mockito.verify(syncManagerRegistros, Mockito.times(2)).processSendResponse(arrayCaptor.capture()); // Só é chamado 2 vezes porque no 3º post ele não possui mais dados modificados.
        List<JSONArray> capturedArrays = arrayCaptor.getAllValues();
            // >> syncManagerRegistros
            // Primeiro post - registro 1
        Assert.assertEquals(firstResponse.get("registros_id"),capturedArrays.get(0));
            // Segundo post - registro 2
        Assert.assertEquals(secondResponse.get("registros_id"),capturedArrays.get(1));

            // >> syncManagerEmpresas
        arrayCaptor = ArgumentCaptor.forClass(JSONArray.class);
        Mockito.verify(syncManagerEmpresas).processSendResponse(arrayCaptor.capture()); // É chamado 3 vezes porque possui dados modificados até que o último post seja processado.
        capturedArrays = arrayCaptor.getAllValues();
            // Primeiro post - registro 1

            // Segundo post - registro 2
        Mockito.verify(syncManagerEmpresas, Mockito.times(1)).saveNewData(Mockito.eq(secondResponse.getJSONArray("empresas")),Mockito.anyString()); // Salvando novos dados enviados com a resposta

            // Terceiro post - empresas
        Assert.assertEquals(thirdResponse.get("empresas_id"), capturedArrays.get(0));

        // Envio realizado
        Assert.assertEquals(true,completed);

    }

    @Test
    public void testSendDataToServerFail() throws Exception {
        // Thread interrompido
        ThreadChecker threadChecker = Mockito.mock(ThreadChecker.class);
        Mockito.when(threadChecker.isValidThreadId(Mockito.anyString())).thenReturn(false);

        dataSyncHelper.threadChecker = threadChecker;

        boolean completed = dataSyncHelper.sendDataToServer();

        // Assegurando que o banco de dados não fez commit
        Mockito.verify(database, Mockito.never()).setTransactionSuccessful();

        // Assegurando que o timestamp não foi salvo
        Mockito.verify(syncConfig, Mockito.never()).setTimestamp(Mockito.anyString());

        // Send data não realizado
        Assert.assertEquals(false, completed);
    }

    @Test
    public void testFullSynchronousSync() throws Exception {
        DataSyncHelper dataSyncHelper = Mockito.spy(this.dataSyncHelper);


        // Get data OK, send data fail
        Mockito.doReturn(true).when(dataSyncHelper).getDataFromServer();
        Mockito.doReturn(false).when(dataSyncHelper).sendDataToServer();
        Assert.assertEquals(false, dataSyncHelper.fullSynchronousSync());
        Mockito.verify(dataSyncHelper,Mockito.never()).postSyncFinishedEvent();

        // Get data fail, send data OK
        Mockito.doReturn(false).when(dataSyncHelper).getDataFromServer();
        Mockito.doReturn(true).when(dataSyncHelper).sendDataToServer();
        Assert.assertEquals(false, dataSyncHelper.fullSynchronousSync());
        Mockito.verify(dataSyncHelper,Mockito.never()).postSyncFinishedEvent();

        // Get data ok, send data ok
        Mockito.doReturn(true).when(dataSyncHelper).getDataFromServer();
        Mockito.doReturn(true).when(dataSyncHelper).sendDataToServer();
        Assert.assertEquals(true, dataSyncHelper.fullSynchronousSync());
        Mockito.verify(dataSyncHelper,Mockito.times(1)).postSyncFinishedEvent();

    }

    @Test
    public void testEvents() throws Exception {

        // Get finished
        dataSyncHelper.postGetFinishedEvent();
        ArgumentCaptor<DataSyncHelper.GetFinishedEvent> getFinishedCaptor = ArgumentCaptor.forClass(DataSyncHelper.GetFinishedEvent.class);
        Mockito.verify(bus, Mockito.times(1)).post(getFinishedCaptor.capture());
        Assert.assertEquals(DataSyncHelper.GetFinishedEvent.class, getFinishedCaptor.getValue().getClass());

        // Send finished
        dataSyncHelper.postSendFinishedEvent();
        ArgumentCaptor<DataSyncHelper.SendFinishedEvent> sendFinishedCaptor = ArgumentCaptor.forClass(DataSyncHelper.SendFinishedEvent.class);
        Mockito.verify(bus, Mockito.times(2)).post(sendFinishedCaptor.capture());
        Assert.assertEquals(DataSyncHelper.SendFinishedEvent.class, sendFinishedCaptor.getValue().getClass());

        // Sync finished
        dataSyncHelper.postSyncFinishedEvent();
        ArgumentCaptor<DataSyncHelper.SyncFinishedEvent> syncFinishedCaptor = ArgumentCaptor.forClass(DataSyncHelper.SyncFinishedEvent.class);
        Mockito.verify(bus, Mockito.times(3)).post(syncFinishedCaptor.capture());
        Assert.assertEquals(DataSyncHelper.SyncFinishedEvent.class, syncFinishedCaptor.getValue().getClass());
    }

}