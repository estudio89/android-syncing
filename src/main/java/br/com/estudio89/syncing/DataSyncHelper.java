package br.com.estudio89.syncing;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import br.com.estudio89.sentry.Sentry;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.injection.SyncingInjection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Essa classe é responsável por realizar a sincronização de dados. 
 * Ela se comunica com todos os {@link SyncManager}s e com o Servidor, fazendo a troca de informação entre as entidades.
 * @author luccascorrea
 *
 */
public class DataSyncHelper {
	
	public Context appContext;
	public AsyncBus bus;
	public SyncConfig syncConfig;
	public ServerComm serverComm;
	public CustomTransactionManager transactionManager;
	public ThreadChecker threadChecker;
	private String TAG = "Syncing";
	
	public static DataSyncHelper getInstance() {
		return SyncingInjection.get(DataSyncHelper.class);
	}
	
	/**
	 * Método responsável por buscar novos dados no servidor.
	 * Ele opera na seguinte lógica:
	 * 
	 * <ol>
	 * 	<li> Adiciona um identificador para o thread executado.
	 *  <li> Busca o token e, caso for nulo, interrompe a execução.
	 *  <li> Monta objeto JSON contendo metadados (token e timestamp).
	 *  <li> Solicita dados ao servidor usando a URL de GetData.
	 *  <li> Recebe a resposta do servidor como um objeto JSON.
	 *  <li> Extrai da resposta o novo timestamp recebido
	 *  <li> Faz um loop em todos os keys recebidos na resposta, buscando o SyncManager responsável por processar aquele key.
	 *  <li> Para o SyncManager encontrado, solicita que salve os dados constantes no JSON.
	 *  <li> Lança um evento de sincronização finalizada para aquele determinado SyncManager, passando os novos dados recebidos junto ao evento.
	 *  <li> Ao processar todos os SyncManagers, verifica se o identificador do thread ainda é válido e, caso sim, dá commit na transação e seta o novo timestamp.
	 *  <li> Lança um evento indicando que a busca de dados foi finalizada.
	 * </ol>
	 * @return boolean indicando se a busca de dados foi realizada. Só será false se o usuário fizer logout antes que termine.
	 */
	public boolean getDataFromServer() throws IOException {
		final String threadId = threadChecker.setNewThreadId();
		String token = syncConfig.getAuthToken();
		
		if (token == null) {
			threadChecker.removeThreadId(threadId);
			return false;
		}
		
		JSONObject data = new JSONObject();
		try {
			data.put("token", token);
			data.put("timestamp", syncConfig.getTimestamp());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		final JSONObject jsonResponse = serverComm.post(syncConfig.getGetDataUrl(),data);

		final String timestamp;
		try {
			timestamp = jsonResponse.getString("timestamp");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		if (this.processGetDataResponse(threadId,jsonResponse,timestamp)) {
			threadChecker.removeThreadId(threadId);
			return true;
		} else {
			threadChecker.removeThreadId(threadId);
			return false;
		}

	}
	/**
	 * Esse é um método público que serve para buscar dados do servidor quando se necessita 
	 * de um model específico e de um número limitado de itens anteriores aos que já estão
	 * armazenados no banco. Esse método possibilita realizar cache parcial de informaçães,
	 * ou seja, na sincronização inicial são trazidos apenas os últimos X dados do servidor e, 
	 * caso o usuário solicitar (por exemplo rolando uma timeline) dados anteriores são solicitados
	 * do servidor.
	 * A lógica utilizada é a seguinte:
	 * <ol>
	 * <li> Adiciona um identificador para o thread executado.
	 * <li> Busca o token e, caso for nulo, interrompe a execução.
	 * <li> É montado um objeto JSON contendo o token e os parâmetros passados.
	 * <li> É feita uma requisição para a url que corresponde ao identifier. 
	 * <li> Recebe a resposta do servidor como um objeto JSON.
	 * <li> É buscado o SyncManager correspondente.
	 * <li> Para o SyncManager encontrado, faz com que salve os dados recebidos.
	 * <li> Lança um evento de sincronização finalizada para aquele determinado SyncManager, passando os novos dados recebidos junto ao evento.
	 * <li> Verifica se o identificador do thread ainda é válido e, caso sim, dá commit na transação.
	 * </ol>
	 * 
	 * @param identifier identificador do {@link SyncManager} solicitado.
	 * @param parameters objeto JSON contendo parâmetros necessários para realizar a busca no servidor.
	 * @return boolean indicando se a busca de dados foi realizada. Só será false se o usuário fizer logout antes que termine.
	 */
	public boolean getDataFromServer(String identifier, JSONObject parameters) throws IOException {
		
		final String threadId = threadChecker.setNewThreadId();
		String token = syncConfig.getAuthToken();
		
		if (token == null) {
			threadChecker.removeThreadId(threadId);
			return false;
		}
		
		try {
			parameters.put("token", token);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		final JSONObject jsonResponse;
		try {
			jsonResponse = serverComm.post(syncConfig.getGetDataUrlForModel(identifier), parameters);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}

		if (this.processGetDataResponse(threadId,jsonResponse,null)) {
			threadChecker.removeThreadId(threadId);
			return true;
		} else {
			threadChecker.removeThreadId(threadId);
			return false;
		}

	}
	/**
	 * Método utilizado para enviar dados ao servidor.
	 * Esse método opera com a seguinte lógica
	 * <ol>
	 * <li>Adiciona um identificador para o thread executado.
	 * <li>Busca o token e, caso for nulo, interrompe a execução.
	 * <li>Monta o objeto JSON contendo metadados (token, timestamp e identificador do device).
	 * <li>Faz um loop em cada {@link SyncManager} e, para cada um, verifica se é necessário que o envio
	 * seja feito de um e um ou não.
	 * Caso o envio deva ser feito de um em um:
	 * 	<ol>
	 * 		<li>Faz um loop dentro de todos os objetos JSON a serem enviados</li>
	 * 		<li>Para cada objeto, faz um post de envio contendo o próprio objeto e os arquivos que o acompanham.</li>
	 * 		<li>Processa a resposta do envio dos dados.</li>
	 * 	</ol>
	 * Caso o envio possa ser feito agrupado:
	 * 	<ol>
	 * 		<li>Adiciona todos os dados de todos os SyncManagers a um único objeto JSON, contendo também os arquivos.</li>
	 * 	</ol>
	 * </li>
	 * <li>Verifica se existem dados a serem enviados em conjunto.
	 * <li>Faz o envio dos dados e processa a resposta do envio.
	 * <li>Posta evento indicando que todos os dados foram enviados.
	 * </ol>
	 * 
	 * @return
	 */
	public boolean sendDataToServer() throws IOException {
		final String threadId = threadChecker.setNewThreadId();
		String token = syncConfig.getAuthToken();
		
		// Verificando token
		if (token == null) {
			threadChecker.removeThreadId(threadId);
			return false;
		}
		
		// Metadados
		JSONObject data = new JSONObject();
		try {
			data.put("token", token);
			data.put("timestamp", syncConfig.getTimestamp());
			data.put("device_id", syncConfig.getDeviceId());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		int nroMetadados = data.length();
		
		// Juntando objetos e arquivos
		List<String> files = new ArrayList<String>();
		JSONArray modifiedData;
		for (SyncManager syncManager : syncConfig.getSyncManagers()) {
			if (!syncManager.hasModifiedData()) {
				continue;
			}
			modifiedData = syncManager.getModifiedData();
			
			
			if (syncManager.shouldSendSingleObject()) { // Envio de objetos um a um
				
				try {
					for (int idx = 0; idx < modifiedData.length(); idx++) {
						JSONObject object;
							object = modifiedData.getJSONObject(idx);
						JSONObject partialData = new JSONObject(data.toString());
						JSONArray singleItemArray = new JSONArray();
						singleItemArray.put(object);
						partialData.put(syncManager.getIdentifier(), singleItemArray);
						List<String> partialFiles = syncManager.getModifiedFilesForObject(object);
						Log.d(TAG,"Enviando item " + object);
						JSONObject jsonResponse = serverComm.post(syncConfig.getSendDataUrl(), partialData, partialFiles);
						if (!this.processSendResponse(threadId, jsonResponse)) {
							return false;
						}
						data.put("timestamp", syncConfig.getTimestamp());
					}
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
			} else { // Envio de todos os objetos de uma vez só

				try {
					data.put(syncManager.getIdentifier(), modifiedData);
					files.addAll(syncManager.getModifiedFiles());
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		if (data.length() > nroMetadados) {
			JSONObject jsonResponse = serverComm.post(syncConfig.getSendDataUrl(), data, files);
			if (this.processSendResponse(threadId, jsonResponse)) {
				this.threadChecker.removeThreadId(threadId);
				postSendFinishedEvent();
				return true;
			} else {
				this.threadChecker.removeThreadId(threadId);
				return false;
			}
		} else {
			this.threadChecker.removeThreadId(threadId);
			postSendFinishedEvent();
			return true;
		}
		
	}
	/**
	 * Método privado que faz o processamento da resposta da solicitação de dados.
	 * É utilizada a seguinte lógica:
	 * <ol>
	 * 	<li>Obtém o timestamp da resposta do servidor.
	 * 	<li>Inicia uma transação no banco de dados.
	 * 	<li>Faz um loop por todos os {@link SyncManager}s.
	 * 	<li>Para cada {@link SyncManager} obtém a resposta do servidor.
	 * 	<li>Solicita ao {@link SyncManager} que salve os novos dados.
	 * 	<li>Ao final do loop, verifica se o identificador do thread ainda é válido e, caso sim, dá commit na transação e seta o timestamp.
	 * </ol>
	 * @param threadId o identificador do thread.
	 * @param jsonResponse resposta do post de envio de dados.
	 * @return boolean indicando se os dados foram processados. Só será false se o usuário fizer logout durante a execução.
	 */
	private boolean processGetDataResponse(final String threadId, final JSONObject jsonResponse, final String timestamp) {

		transactionManager.doInTransaction(new CustomTransactionManager.Callback() {

			@Override
			public void manipulateInTransaction() throws InterruptedException {
				for (SyncManager syncManager:syncConfig.getSyncManagers()) {
					String identifier = syncManager.getIdentifier();
					JSONObject jsonObject = jsonResponse.optJSONObject(identifier);

					if (jsonObject != null) {
                        JSONArray jsonArray = jsonObject.optJSONArray("data");
                        if (jsonArray == null) {
                            jsonArray = new JSONArray();
                        }
                        jsonObject.remove("data");
						List<Object> objects = syncManager.saveNewData(jsonArray, syncConfig.getDeviceId(), jsonObject);
						syncManager.postEvent(objects, bus, appContext);
					}
				}

				if (threadChecker.isValidThreadId(threadId)) {
					if (timestamp != null) {
						syncConfig.setTimestamp(timestamp);
					}
					postGetFinishedEvent();
				} else {
					throw new InterruptedException("Sincronização interrompida");
				}

			}
		}, this.appContext, this.syncConfig);

		return transactionManager.wasSuccesful();
	}

	/**
	 * Método privado que faz o processamento da resposta do envio de dados.
	 * É utilizada a seguinte lógica:
	 * <ol>
	 * 	<li>Obtém o timestamp da resposta do servidor.
	 * 	<li>Inicia uma transação no banco de dados.
	 * 	<li>Faz um loop por todos os keys do json recebido.
	 * 	<li>Para cada key, identifica o SyncManager correspondente e solicita que processe a resposta recebida.
	 * 	<li>Caso não seja encontrado o {@link SyncManager}, verifica se o valor no json corresponde a um response id.
	 * 	<li>Caso sim, solicita que o SyncManager correspondente salve os novos dados.</li>
	 * 	<li>Verifica se o identificador do thread ainda é válido e, caso sim, dá commit na transação e seta o timestamp.
	 * </ol>
	 * @param threadId o identificador do thread.
	 * @param jsonResponse resposta do post de envio de dados.
	 * @return boolean indicando se os dados foram processados. Só será false se o usuário fizer logout durante a execução.
	 */
	private boolean processSendResponse(final String threadId, final JSONObject jsonResponse) {
		final String timestamp;
		try {
			timestamp = jsonResponse.getString("timestamp");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		transactionManager.doInTransaction(new CustomTransactionManager.Callback() {

			@Override
			public void manipulateInTransaction() throws InterruptedException {
				JSONArray syncResponse;
				JSONObject newDataResponse;
                JSONArray newData;

				Iterator<String> iterator = jsonResponse.keys();
				while(iterator.hasNext()) {
					String responseId = iterator.next();
					SyncManager syncManager = syncConfig.getSyncManagerByResponseId(responseId);
					if (syncManager != null) {
						syncResponse = jsonResponse.optJSONArray(responseId);
						syncManager.processSendResponse(syncResponse);
					} else { // Não é um response id, mas pode ser um identifier com novos dados
						syncManager = syncConfig.getSyncManager(responseId);
						if (syncManager != null) {
							newDataResponse = jsonResponse.optJSONObject(responseId);
                            newData = newDataResponse.optJSONArray("data");
                            if (newData == null) {
                                newData = new JSONArray();
                            }
                            newDataResponse.remove("data");
							List<Object> objects = syncManager.saveNewData(newData, syncConfig.getDeviceId(), newDataResponse);
							syncManager.postEvent(objects, bus, appContext);
						}
					}
				}

				if (threadChecker.isValidThreadId(threadId)) {
					syncConfig.setTimestamp(timestamp);
				} else {
					throw new InterruptedException();
				}
			}
			
		}, this.appContext, this.syncConfig);
		
		return transactionManager.wasSuccesful();
	}
	
	/**
	 * Esse método realiza uma sincronização completa de forma síncrona, ou seja, 
	 * primeiro busca dados novos no servidor e depois envia dados que tenham sido 
	 * criados no dispositivo (caso existam).
	 */
	private static boolean isRunningSync = false;
	private boolean internalfullSynchronousSync() throws IOException {
		if (isRunningSync) {
			Log.d(TAG, "Sync already running");
			return false;
		}

		Log.d(TAG, "STARTING NEW SYNC");
		boolean completed = false;
		isRunningSync = true;
		try {
            Log.d(TAG, "GETTING DATA FROM SERVER");
			completed = getDataFromServer();
            Log.d(TAG, "GOT DATA FROM SERVER");
			if (completed && hasModifiedData()) {
				completed = sendDataToServer();
			}
		} finally {
			isRunningSync = false;
		}

		if (completed) {
			postSyncFinishedEvent();
			return true;
		} else {
			return false;
		}
	}

	public boolean fullSynchronousSync() throws IOException {
		try {
			return this.internalfullSynchronousSync();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			sendCaughtException(e);
		}

		return false;
	}
	
	/**
	 * Esse método realiza uma sincronização completa de forma assíncrona, ou seja, 
	 * primeiro busca dados novos no servidor e depois envia dados que tenham sido 
	 * criados no dispositivo (caso existam). Caso a sincronização esteja em curso,
	 * não inicia uma nova sincronização.
	 *
	 */
	public void fullAsynchronousSync() {
		if (!isRunningSync) {
			Log.d(TAG,"Running new FullSyncAsyncTask");
			new FullSyncAsyncTask().execute();
		}
	}

    /**
     * Esse método realiza uma sincronização de um model específico de forma assíncrona,
     * executando o método getDataFromServer(String identifier, JSONObject parameters)
     * em um thread à parte.
     *
     */
    private static HashMap<String, Boolean> partialSyncFlag = new HashMap<String, Boolean>();
    public void partialAsynchronousSync(String identifier, JSONObject parameters) {
        Boolean flag = partialSyncFlag.get(identifier);
        if (flag == null || !flag) {
            PartialSyncTask task = new PartialSyncTask();
            task.parameters = parameters;
            task.identifier = identifier;
            task.execute();
        }
    }

	/**
	 * Esse método verifica se algum dos {@link SyncManager}s possui necessidade de enviar dados ao servidor.
	 * @return
	 */
	public boolean hasModifiedData() {
		for (SyncManager syncManager: syncConfig.getSyncManagers()) {
			if (syncManager.hasModifiedData()) {
				return true;
			}
		}
		return false;
	}
	
	public void sendCaughtException(Throwable t) {
		try {
			Sentry.captureException(t);
		} catch (Exception e) {
			throw new RuntimeException(t);
		}
	}
	/**
	 * Método público, o qual limpa o {@link br.com.estudio89.syncing.ThreadChecker}.
	 * Esse método deve ser chamado ao fazer logout, o que 
	 * garante que se uma sincronização estiver sendo realizada, 
	 * nenhum dado será salvo no banco de dados quando ela finalizar
	 * assim como o timestamp não será alterado.
	 */
	public void stopSyncThreads() {
		this.threadChecker.clear();
	}
	
	/**
	 * Lança um evento indicando que o envio de dados foi finalizado.
	 */
	protected void postSendFinishedEvent() {
		bus.post(new SendFinishedEvent());
	}
	
	/**
	 * Lança um evento indicando que o recebimento de dados foi finalizado.
	 */
	protected void postGetFinishedEvent() {
		bus.post(new GetFinishedEvent());
	}
	
	/**
	 * Lança um evento indicando que a sincronização (recebimento + envio) foi finalizada.
	 */
	protected void postSyncFinishedEvent() {
		bus.post(new SyncFinishedEvent());
		Log.d(TAG, "=== Posted sync finished event bus hash:" + bus.hashCode());
	}

	/**
	 * Lança um evento quando há erro na sincronização.
	 * Chamado pelo SyncService.
	 *
	 * @param t
	 */
	public void postBackgroundSyncError(Throwable t) {
		bus.post(new BackgroundSyncError(t));
	}
	
	/**
	 * Evento lançado ao finalizar o envio de dados de todos os models.
	 * @author luccascorrea
	 *
	 */
	public static class SendFinishedEvent {
		public SendFinishedEvent() {
		}
	}
	
	/**
	 * Evento lançado ao receber todos os dados do servidor.
	 * @author luccascorrea
	 *
	 */
	public static class GetFinishedEvent {
		public GetFinishedEvent() {
		}
	}
	
	/**
	 * Evento lançado ao finalizar a sincronização.
	 * @author luccascorrea
	 *
	 */
	public static class SyncFinishedEvent {
		public SyncFinishedEvent(){
		}
	}

	/**
	 * Evento lançado quando ocorre um erro durante a sincronização em background ou na sincronização assíncrona.
	 *
	 */
	public static class BackgroundSyncError {
		private Throwable t;
		public BackgroundSyncError(Throwable t) {
			this.t = t;
		}

		public Throwable getError() {
			return this.t;
		}
	}

	class FullSyncAsyncTask extends AsyncTask<Void,Void,Void> {

		@Override
		protected Void doInBackground(Void... voids) {
			try {
				fullSynchronousSync();
			} catch (IOException e) {
				postBackgroundSyncError(e);
                Log.d(TAG,"Background sync error: " + e.getMessage());
			}

			return null;
		}
	}

    class PartialSyncTask extends AsyncTask<Void,Void,Void> {
        public JSONObject parameters;
        public String identifier;

        @Override
        protected Void doInBackground(Void... voids) {
            partialSyncFlag.put(identifier, true);

            try {
                getDataFromServer(identifier, parameters);
            } catch (IOException e) {
                postBackgroundSyncError(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            partialSyncFlag.put(identifier, false);
        }
    }
}
