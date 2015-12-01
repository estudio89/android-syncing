package br.com.estudio89.syncing;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import br.com.estudio89.sentry.Sentry;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.exceptions.Http408Exception;
import br.com.estudio89.syncing.exceptions.Http502Exception;
import br.com.estudio89.syncing.exceptions.Http503Exception;
import br.com.estudio89.syncing.injection.SyncingInjection;
import br.com.estudio89.syncing.models.SyncModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.*;

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
	private HashMap<String, List<? extends SyncModel>> eventQueue = new HashMap<String, List<? extends SyncModel>>();

	private String TAG = "Syncing";
	private static boolean isRunningSync = false; // Indicates if a full synchronization is running
	private static HashMap<String, Boolean> partialSyncFlag = new HashMap<String, Boolean>(); // Indicates if a sync manager is syncing
	private static int numberAttempts = 0; // Stores the number of attemps when trying to sync
	
	public static DataSyncHelper getInstance() {
		return SyncingInjection.get(DataSyncHelper.class);
	}

	public boolean getDataFromServer() throws IOException {
		return getDataFromServer(null, new JSONObject(), true);
	}

	protected boolean getDataFromServer(String identifier, JSONObject parameters) throws IOException {
		return getDataFromServer(identifier, parameters, false);
	}

	protected boolean getDataFromServer(String identifier) throws IOException {
		return getDataFromServer(identifier, new JSONObject(), true);
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
	private boolean getDataFromServer(String identifier, JSONObject parameters, boolean sendTimestamp) throws IOException {
		final String threadId = threadChecker.setNewThreadId();
		String token = syncConfig.getAuthToken();
		
		if (token == null) {
			threadChecker.removeThreadId(threadId);
			return false;
		}

		try {
			parameters.put("token", token);
			if (sendTimestamp) {
				if (identifier != null) {
					parameters.put("timestamps", syncConfig.getTimestamp(identifier));
				} else {
					parameters.put("timestamps", syncConfig.getTimestamps());
				}
			}

		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		String url = null;
		try {
			url = identifier != null ? syncConfig.getGetDataUrlForModel(identifier) : syncConfig.getGetDataUrl();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}

		final JSONObject jsonResponse = serverComm.post(url,parameters);

		final JSONObject timestamps;
		try {

			timestamps = sendTimestamp ? jsonResponse.getJSONObject("timestamps") : null;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		if (this.processGetDataResponse(threadId,jsonResponse,timestamps)) {
			threadChecker.removeThreadId(threadId);
			return true;
		} else {
			threadChecker.removeThreadId(threadId);
			return false;
		}

	}

	public boolean sendDataToServer() throws IOException {
		return sendDataToServer(null);
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
	protected boolean sendDataToServer(String identifier) throws IOException {
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
			data.put("timestamps", identifier == null ? syncConfig.getTimestamps() : syncConfig.getTimestamp(identifier));
			data.put("device_id", syncConfig.getDeviceId());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		int nroMetadados = data.length();
		
		// Juntando objetos e arquivos
		List<String> files = new ArrayList<String>();
		List<SyncManager> syncManagers = new ArrayList<SyncManager>();
		if (identifier == null) {
			syncManagers = syncConfig.getSyncManagers();
		} else {
			syncManagers.add(syncConfig.getSyncManager(identifier));
		}
		JSONArray modifiedData;
		for (SyncManager syncManager : syncManagers) {
			if (!syncManager.hasModifiedData()) {
				continue;
			}
			modifiedData = syncManager.getModifiedData();
			
			
			if (syncManager.shouldSendSingleObject()) { // Envio de objetos um a um
				// Removing timestamp from main data object
				try {
					JSONObject timestamps = data.getJSONObject("timestamps");
					timestamps.remove(syncManager.getIdentifier());
					data.put("timestamps", timestamps);
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}

				try {
					for (int idx = 0; idx < modifiedData.length(); idx++) {
						JSONObject object;
							object = modifiedData.getJSONObject(idx);
						JSONObject partialData = new JSONObject(data.toString());
						JSONArray singleItemArray = new JSONArray();
						singleItemArray.put(object);
						partialData.put(syncManager.getIdentifier(), singleItemArray);
						partialData.put("timestamps", syncConfig.getTimestamp(syncManager.getIdentifier()));
						List<String> partialFiles = syncManager.getModifiedFilesForObject(object);
						Log.d(TAG, "Enviando item " + object);
						JSONObject jsonResponse = serverComm.post(syncConfig.getSendDataUrl(), partialData, partialFiles);
						if (!this.processSendResponse(threadId, jsonResponse)) {
							return false;
						}
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
	private boolean processGetDataResponse(final String threadId, final JSONObject jsonResponse, final JSONObject timestamps) {

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
						List<? extends SyncModel> objects = syncManager.saveNewData(jsonArray, syncConfig.getDeviceId(), jsonObject, appContext);
						addToEventQueue(syncManager, objects);
					}
				}

				if (threadChecker.isValidThreadId(threadId)) {
					if (timestamps != null) {
						syncConfig.setTimestamps(timestamps);
					}
					postGetFinishedEvent();
				} else {
					throw new InterruptedException("Sincronização interrompida");
				}

			}
		}, this.appContext, this.syncConfig);

		if (transactionManager.wasSuccesful()) {
			postEventQueue();
		}
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
		final JSONObject timestamps;
		try {
			timestamps = jsonResponse.getJSONObject("timestamps");
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
				while (iterator.hasNext()) {
					String responseId = iterator.next();
					SyncManager syncManager = syncConfig.getSyncManagerByResponseId(responseId);
					if (syncManager != null) {
						syncResponse = jsonResponse.optJSONArray(responseId);
						List<? extends SyncModel> objects = syncManager.processSendResponse(syncResponse);
						addToEventQueue(syncManager, objects);
					} else { // Não é um response id, mas pode ser um identifier com novos dados
						syncManager = syncConfig.getSyncManager(responseId);
						if (syncManager != null) {
							newDataResponse = jsonResponse.optJSONObject(responseId);
							newData = newDataResponse.optJSONArray("data");
							if (newData == null) {
								newData = new JSONArray();
							}
							newDataResponse.remove("data");
							List<? extends SyncModel> objects = syncManager.saveNewData(newData, syncConfig.getDeviceId(), newDataResponse, appContext);
							addToEventQueue(syncManager, objects);
						}
					}
				}

				if (threadChecker.isValidThreadId(threadId)) {
					syncConfig.setTimestamps(timestamps);
				} else {
					throw new InterruptedException();
				}
			}

		}, this.appContext, this.syncConfig);

		if (transactionManager.wasSuccesful()) {
			postEventQueue();
		}

		return transactionManager.wasSuccesful();
	}

	/**
	 * Esse método realiza uma sincronização completa de forma síncrona, ou seja,
	 * primeiro busca dados novos no servidor e depois envia dados que tenham sido
	 * criados no dispositivo (caso existam).
	 */

	private boolean internalRunSynchronousSync(String identifier) throws IOException {

		Log.d(TAG, "STARTING NEW SYNC");
		boolean completed = false;
		if (identifier != null) {
			partialSyncFlag.put(identifier, true);
		} else {
			isRunningSync = true;
		}

		try {
			Log.d(TAG, "GETTING DATA FROM SERVER");
			completed = getDataFromServer(identifier);
			Log.d(TAG, "GOT DATA FROM SERVER");
			if (completed && hasModifiedData()) {
				completed = sendDataToServer(identifier);
			}
		} finally {
			if (identifier != null) {
				partialSyncFlag.put(identifier, false);
			} else {
				isRunningSync = false;
			}
		}

		if (completed) {
			if (identifier != null) {
				postPartialSyncFinishedEvent();
			} else {
				postSyncFinishedEvent();
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * This method wraps the internalRunSynchronousSync method,
	 * in order to report exceptions without crashing the app and
	 * using exponential backoff when the server is overloaded.
	 */
	protected boolean runSynchronousSync(String identifier) throws IOException {
		try {
			numberAttempts += 1;
			boolean response = this.internalRunSynchronousSync(identifier);
			numberAttempts = 0;
			return response;
		} catch (Http502Exception | Http503Exception | Http408Exception e) {
			// Server is overloaded - exponential backoff
			if (numberAttempts < 4) {
				double waitTimeSeconds = 0.5 * (Math.pow(2, numberAttempts) - 1);
				waitTimeSeconds += new Random().nextDouble();
				try {
					Thread.sleep(Math.round(waitTimeSeconds * 1000));
					return this.runSynchronousSync(identifier);
				} catch (InterruptedException e1) {
				}
			} else {
				numberAttempts = 0;
				throw new Http408Exception();
			}
		} catch (SocketTimeoutException e) {
			postBackgroundSyncError(e);
			syncConfig.requestSync();
		} catch(ConnectException e) {
			syncConfig.requestSync();
			postConnectionFailedError(e);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			sendCaughtException(e);
		}

		return false;
	}
	/**
	 * Esse método realiza uma sincronização completa de forma síncrona, ou seja,
	 * primeiro busca dados novos no servidor e depois envia dados que tenham sido
	 * criados no dispositivo (caso existam).
	 */
	public boolean fullSynchronousSync() throws IOException {
		if (canRunSync()) {
			Log.d(TAG,"Running new fullSynchronousSync");
			return runSynchronousSync(null);
		} else {
			Log.d(TAG,"Sync already running");
			return false;
		}
	}
	public boolean partialSynchronousSync(String identifier) throws IOException {
		return partialSynchronousSync(identifier, false);
	}
	/**
	 * Esse método realiza uma sincronização de um model específico de forma síncrona, ou seja,
	 * primeiro busca dados novos no servidor e depois envia dados que tenham sido
	 * criados no dispositivo (caso existam).
	 *
	 */
	public boolean partialSynchronousSync(final String identifier, boolean allowDelay) throws IOException {
		SyncManager sm = syncConfig.getSyncManager(identifier);

		if (sm.getDelay() > 0 && allowDelay) {
			// Delaying execution
			int delay = (int) (sm.getDelay()*new Random().nextDouble())*1000;
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						if (canRunSync(identifier, null)) {
							runSynchronousSync(identifier);
						}
					} catch (IOException e) {
						postBackgroundSyncError(e);
					}
				}
			}, delay);

			return true;
		} else {
			return runSynchronousSync(identifier);
		}

	}

	/**
	 * Esse método realiza uma sincronização completa de forma assíncrona, ou seja,
	 * primeiro busca dados novos no servidor e depois envia dados que tenham sido
	 * criados no dispositivo (caso existam).
	 */
	public void fullAsynchronousSync() {
		if (canRunSync()) {
			Log.d(TAG,"Running new FullSyncAsyncTask");
			new FullSyncAsyncTask().execute();
		} else {
			Log.d(TAG,"Sync already running");
		}
	}
	/**
	 * Esse método realiza uma sincronização de um model específico de forma assíncrona,
	 * executando o método getDataFromServer(String identifier) e sendDataToServer(String identifier)
	 * em um thread à parte.
	 **/
	public void partialAsynchronousSync(String identifier) {
		partialAsynchronousSync(identifier, null);
	}

    /**
     * Esse método realiza uma sincronização de um model específico de forma assíncrona,
     * executando o método getDataFromServer(String identifier, JSONObject parameters)
     * em um thread à parte. Esse método não envia o timestamp ao servidor e nem envia
	 * dados ao servidor. Deve ser usado apenas para paginação.
     *
     */
    public void partialAsynchronousSync(String identifier, JSONObject parameters) {
        if (canRunSync(identifier, parameters)) {
            PartialSyncTask task = new PartialSyncTask();
            task.parameters = parameters;
			task.sendModified = parameters == null;
            task.identifier = identifier;
            task.execute();
        } else {
			Log.d(TAG,"Sync already running");
		}
    }

	/**
	 * This method checks if a sync can be run. It will only be allowed if
	 * there isn't a sync already running.
	 *
	 * @param identifier
	 * @param parameters
	 * @return
	 */
	public boolean canRunSync(@Nullable String identifier, @Nullable JSONObject parameters) {
		if (identifier == null && parameters == null) {
			return !isRunningSync;
		}

		Boolean flag = partialSyncFlag.get(identifier);
		return (!isRunningSync && (flag == null || !flag || parameters == null));
	}

	public boolean canRunSync() {
		return canRunSync(null, null);
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
			postBackgroundSyncError(t);
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

	public void addToEventQueue(SyncManager syncManager, List<? extends SyncModel> objects) {
		if (objects == null) {
			return;
		}

		List<? extends SyncModel> existing = eventQueue.get(syncManager.getIdentifier());
		if (existing != null) {
			List<SyncModel> newList = new ArrayList<>();
			newList.addAll(existing);
			newList.addAll(objects);

			eventQueue.put(syncManager.getIdentifier(), newList);

		} else {
			eventQueue.put(syncManager.getIdentifier(), objects);
		}

	}

	public void postEventQueue() {
		List<String> keys = new ArrayList<String>();
		keys.addAll(eventQueue.keySet());

		for (String identifier:keys) {
			List<? extends SyncModel> objects = eventQueue.get(identifier);
			SyncManager syncManager = syncConfig.getSyncManager(identifier);
			if (syncManager != null && objects != null) {
				syncManager.postEvent(objects, this.bus, this.appContext);
			}
			eventQueue.remove(identifier);
		}

		if (eventQueue.size() > 0) {
			postEventQueue();
		}
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

	protected void postPartialSyncFinishedEvent() {
		bus.post(new PartialSyncFinishedEvent());
		Log.d(TAG, "=== Posted partial sync finished event bus hash:" + bus.hashCode());
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
	 * Lança um evento quando há erro na sincronização.
	 * Chamado pelo SyncService.
	 *
	 * @param t
	 */
	public void postConnectionFailedError(Throwable t) {
		bus.post(new ConnectionFailedError(t));
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
	 * Evento lançado ao finalizar uma sincronização partial (recebimento + envio)
	 */
	public static class PartialSyncFinishedEvent {
		public PartialSyncFinishedEvent(){
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

	/**
	 * Evento lançado quando ocorre um erro durante a sincronização em background ou na sincronização assíncrona.
	 *
	 */
	public static class ConnectionFailedError extends BackgroundSyncError {

		public ConnectionFailedError(Throwable t) {
			super(t);
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
		public boolean sendModified;

        @Override
        protected Void doInBackground(Void... voids) {

            try {
				if (sendModified) {
					partialSynchronousSync(identifier);
				} else {
					partialSyncFlag.put(identifier, true);
                	getDataFromServer(identifier, parameters);
				}
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
