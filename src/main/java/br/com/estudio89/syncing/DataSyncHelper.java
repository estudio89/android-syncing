package br.com.estudio89.syncing;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import br.com.estudio89.sentry.Sentry;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.exceptions.*;
import br.com.estudio89.syncing.injection.SyncingInjection;
import br.com.estudio89.syncing.models.SyncModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * This class is responsible for synchronizing all the data with the server.
 * It communicates with all the {@link SyncManager}s e with the server (through {@link ServerComm}),
 * exchanging data with all entities.
 *
 * @author luccascorrea
 *
 */
@SuppressWarnings({"unchecked", "unused"})
public class DataSyncHelper {
	
	public Context appContext;
	public AsyncBus bus;
	public SyncConfig syncConfig;
	public ServerComm serverComm;
	public CustomTransactionManager transactionManager;
	public ThreadChecker threadChecker;
	private HashMap<String, List<? extends SyncModel>> eventQueue = new HashMap<>();

	private String TAG = "Syncing";
	private static boolean isRunningSync = false; // Indicates if a full synchronization is running
	private static HashMap<String, Boolean> partialSyncFlag = new HashMap<>(); // Indicates if a sync manager is syncing
	private static int numberAttempts = 0; // Stores the number of attemps when trying to sync
	
	public static DataSyncHelper getInstance() {
		return SyncingInjection.get(DataSyncHelper.class);
	}

	public boolean getDataFromServer() throws IOException {
		return getDataFromServer(null, new JSONObject(), true);
	}

	public boolean getDataFromServer(String identifier, JSONObject parameters) throws IOException {
		return getDataFromServer(identifier, parameters, false);
	}

	protected boolean getDataFromServer(String identifier) throws IOException {
		return getDataFromServer(identifier, new JSONObject(), true);
	}
	/**
	 * Fetches new data from the server.
	 * Follows the logic below:
	 * 
	 * <ol>
	 * 	<li> Adds an identifier to the thread being run.</li>
	 *  <li> Finds the token and, if null, stops the execution.</li>
	 *  <li> Puts together a JSON object containing metadata (token and timestamps).
	 *  <li> Fetches data from the server.</li>
	 *  <li> Receives the response from the server as a {@link JSONObject}.</li>
	 *  <li> Extracts the timestamps from the response.</li>
	 *  <li> Loops through every key in the response, looking for the {@link SyncManager} responsible for processing it.</li>
	 *  <li> For the {@link SyncManager} found, requests it to save the data contained in the {@link JSONObject}.</li>
	 *  <li> Posts a {@link br.com.estudio89.syncing.DataSyncHelper.GetFinishedEvent} (see {@link #processGetDataResponse(String, JSONObject, JSONObject)}).</li>
	 * </ol>
	 * @return boolean indicating if the operation was successful. It will only be false
	 * if an exception occurred while processing or the user logged out.
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

		String url;
		try {
			url = identifier != null ? syncConfig.getGetDataUrlForModel(identifier) : syncConfig.getGetDataUrl();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}

		long time = System.currentTimeMillis();
		final JSONObject jsonResponse = serverComm.post(url,parameters);
		Log.d("DataSyncHelper", "GETTING DATA REQUEST FINISHED time = " + (System.currentTimeMillis() - time)/1000.0 + " s");

		final JSONObject timestamps;
		try {

			timestamps = sendTimestamp ? jsonResponse.getJSONObject("timestamps") : null;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		time = System.currentTimeMillis();
		if (this.processGetDataResponse(threadId,jsonResponse,timestamps)) {
			threadChecker.removeThreadId(threadId);
			Log.d("DataSyncHelper", "PROCESS GETTING DATA REQUEST TIME" + (System.currentTimeMillis() - time)/1000.0 + " s");
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
	 * Method used for sending data do the server.
	 * This method has the following logic:
	 * <ol>
	 * <li>Adds an identifier to the thread being executed.</li>
	 * <li>Looks for the token and, if it is null, stops the execution.
	 * <li>Puts together a JSON object containing metadata (token, timestamp and device id).
	 * <li>Loops through every {@link SyncManager} and, for each, checks if the items should be sent
	 * one by one or all at once.
	 * In case they should be sent one by one:
	 * 	<ol>
	 * 		<li>Loops through every JSON object being sent</li>
	 * 		<li>For every object, executes a post request containing the object itself and the files associated with it.</li>
	 * 		<li>Process the send response.</li>
	 * 	</ol>
	 * In case the items can be sent all at once:
	 * 	<ol>
	 * 		<li>Adds all the data from all SyncManagers to a single JSON object as well as adds the files associated
	 * 		with these items.</li>
	 * 	</ol>
	 * </li>
	 * <li>Checks if there are any data to be sent.
	 * <li>Sends the data and processes the response.
	 * <li>Posts an event indicating that data were sent.
	 * </ol>
	 *
	 * @param identifier the identifier of a particular {@link SyncManager}. This could be null.
	 * @return boolean indicating if the operation was successful.
	 */
	protected boolean sendDataToServer(String identifier) throws IOException {
		final String threadId = threadChecker.setNewThreadId();
		String token = syncConfig.getAuthToken();
		
		// Verifying token
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
		
		// Putting together objects and files
		List<String> files = new ArrayList<>();
		List<SyncManager> syncManagers = new ArrayList<>();
		if (identifier == null) {
			syncManagers = syncConfig.getSyncManagers();
		} else {
			syncManagers.add(syncConfig.getSyncManager(identifier));
		}
		long time = System.currentTimeMillis();
		JSONArray modifiedData;
		for (SyncManager syncManager : syncManagers) {
			if (!syncManager.hasModifiedData()) {
				continue;
			}
			modifiedData = syncManager.getModifiedData();
			
			
			if (syncManager.shouldSendSingleObject()) { // Sending objects one by one
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
			} else { // Sending all objects at once

				try {
					data.put(syncManager.getIdentifier(), modifiedData);
					files.addAll(syncManager.getModifiedFiles());
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
			}
		}
		Log.d("DataSyncHelper", "SEND DATA GET TIME = " + (System.currentTimeMillis() - time)/1000.0);
		if (data.length() > nroMetadados) {
			time = System.currentTimeMillis();
			JSONObject jsonResponse = serverComm.post(syncConfig.getSendDataUrl(), data, files);
			Log.d("DataSyncHelper", "SEND DATA REQUEST TIME = " + (System.currentTimeMillis() - time)/1000.0);
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
	 * Processes the response received by the server after fetching data.
	 * It follows the logic below:
	 * <ol>
	 * 	<li>Obtains the timestamps sent by the server.</li>
	 * 	<li>Starts a transaction in the database.</li>
	 * 	<li>Loops through all {@link SyncManager}s.</li>
	 * 	<li>For every {@link SyncManager}, obtains the response sent by the server.</li>
	 * 	<li>Requests the {@link SyncManager} to save all the new data.</li>
	 * 	<li>At the end of the loop, verifies if the thread identifier is still valid and, if yes, commits the transaction e sets all timestamps.</li>
	 * </ol>
	 * @param threadId o identificador do thread.
	 * @param jsonResponse resposta do post de envio de dados.
	 * @param timestamps the timestamps sent by the server.
	 * @return boolean indicando se os dados foram processados. Só será false se o usuário fizer logout durante a execução.
	 */
	private boolean processGetDataResponse(final String threadId, final JSONObject jsonResponse, final JSONObject timestamps) {

		transactionManager.doInTransaction(new CustomTransactionManager.Callback() {

			@Override
			public void manipulateInTransaction() throws InterruptedException {
				long time;
				for (SyncManager syncManager:syncConfig.getSyncManagers()) {
					time = System.currentTimeMillis();
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
					Log.d("DataSyncHelper", "TIME PROCESSING RESPONSE WITH IDENTIFIER " + identifier + " " + (time - System.currentTimeMillis())/1000. + "s");
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
	 * Processes the response obtained after sending data to the server.
	 * It follows the logic below:
	 * <ol>
	 * 	<li>Obtains the timestamps sent by the server.</li>
	 * 	<li>Starts a transaction in the database.</li>
	 * 	<li>Loops through all the keys in the {@link JSONObject} that was received.</li>
	 * 	<li>For each key, finds the associated SyncManager and requests it to process the response.</li>
	 * 	<li>In case a {@link SyncManager} is not found, checks if the {@link JSONObject}'s key corresponds to an identifier and not a response id.
	 * 	<li>If yes, requests the {@link SyncManager} to save the new data.</li>
	 * 	<li>Verifies if the thread identifier is still valid and, if yes, commits the transaction e sets all timestamps.
	 * </ol>
	 * @param threadId the thread identifier.
	 * @param jsonResponse the response received from the server after sending data.
	 * @return boolean indicating if the operation was successful. It will only be false
	 * if an exception occurred while processing or the user logged out.
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
	 * This method runs a complete sinchronous sync, that is,
	 * it first fetches new data from the server and then sends data that
	 * was created in the device (if there is anything to be sent).
	 * @param identifier the identifier of a specific {@link SyncManager}. If this is null,
	 *                   then data from all the Sync Managers is synced.
	 * @return boolean indicating if the operation was successful
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
	 * @return boolean indicating if the operation was successful
	 */
	protected boolean runSynchronousSync(String identifier) throws IOException {
		try {
			numberAttempts += 1;
			boolean response = this.internalRunSynchronousSync(identifier);
			numberAttempts = 0;
			return response;
		} catch (Http502Exception | Http503Exception | Http408Exception | Http504Exception e) {
			// Server is overloaded - exponential backoff
			if (numberAttempts < 4) {
				double waitTimeSeconds = 0.5 * (Math.pow(2, numberAttempts) - 1);
				waitTimeSeconds += new Random().nextDouble();
				try {
					Thread.sleep(Math.round(waitTimeSeconds * 1000));
					return this.runSynchronousSync(identifier);
				} catch (InterruptedException ignored) {}
			} else {
				numberAttempts = 0;
				throw new Http408Exception();
			}
		} catch (UnknownHostException | InterruptedIOException | Http403Exception e) {
			postBackgroundSyncError(e);
			syncConfig.requestSync();
		} catch(SocketException e) {
			syncConfig.requestSync();
			postConnectionFailedError(e);
		} catch (Exception e) {
			sendCaughtException(e);
		}

		return false;
	}
	/**
	 * This method runs a complete synchronous sync of all {@link SyncManager}s, that is,
	 * it first fetches new data from the server and then sends data that
	 * was created in the device (if there is anything to be sent).
	 * If there is already a sync operation running, it does not do anything.
	 *
	 * @return boolean indicating if the operation was successful
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
	/**
	 * This method runs a synchronous sync of a particular {@link SyncManager}s, that is,
	 * it first fetches new data from the server and then sends data that
	 * was created in the device (if there is anything to be sent).
	 * If there is already a sync operation running, it does not do anything.
	 *
	 * @param identifier the identifier of a particular {@link SyncManager}
	 * @return boolean indicating if the operation was successful
	 */
	@SuppressWarnings("UnusedReturnValue")
	public boolean partialSynchronousSync(String identifier) throws IOException {
		return partialSynchronousSync(identifier, false);
	}
	/**
	 * This method runs a synchronous sync of a particular {@link SyncManager}s, that is,
	 * it first fetches new data from the server and then sends data that
	 * was created in the device (if there is anything to be sent).
	 * If there is already a sync operation running, it does not do anything.
	 *
	 * @param identifier the identifier of a particular {@link SyncManager}
	 * @param allowDelay a boolean indicating if this request can be delayed according
	 *                   to the delay specified by the {@link SyncManager}
	 * @return boolean indicating if the operation was successful
	 */
	public boolean partialSynchronousSync(final String identifier, @SuppressWarnings("SameParameterValue") boolean allowDelay) throws IOException {
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
	 * This method runs a complete asynchronous sync of all {@link SyncManager}s, that is,
	 * it first fetches new data from the server and then sends data that
	 * was created in the device (if there is anything to be sent).
	 * If there is already a sync operation running, it does not do anything.
	 *
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
	 * This method runs an asynchronous sync of a particular {@link SyncManager}s, that is,
	 * it first fetches new data from the server and then sends data that
	 * was created in the device (if there is anything to be sent).
	 * If there is already a sync operation running, it does not do anything.
	 *
	 * @param identifier the identifier of a particular {@link SyncManager}
	 */
	public void partialAsynchronousSync(String identifier) {
		partialAsynchronousSync(identifier, null, null, null);
	}

	/**
	 * This method runs an asynchronous sync of a particular {@link SyncManager}s, that is,
	 * it first fetches new data from the server and then sends (*) data that
	 * was created in the device (if there is anything to be sent).
	 * If there is already a sync operation running, it does not do anything.
	 *
	 * (*) If a JSONObject containing parameters is passed, then no data
	 * is sent after fetching data from the server. This is meant to be used
	 * when paginating requests.
	 *
	 * @param identifier the identifier of a particular {@link SyncManager}
	 * @param parameters {@link @JSONObject} containing parameters to be sent along with the request
	 */
    public void partialAsynchronousSync(String identifier, JSONObject parameters) {
		partialAsynchronousSync(identifier, parameters, null, null);
	}
	/**
	 * This method runs an asynchronous sync of a particular {@link SyncManager}s, that is,
	 * it first fetches new data from the server and then sends (*) data that
	 * was created in the device (if there is anything to be sent).
	 * If there is already a sync operation running, it does not do anything.
	 *
	 * (*) If a JSONObject containing parameters is passed, then no data
	 * is sent after fetching data from the server. This is meant to be used
	 * when paginating requests.
	 *
	 * @param identifier the identifier of a particular {@link SyncManager}
	 * @param parameters {@link @JSONObject} containing parameters to be sent along with the request
	 * @param successCallback a callback to be run if the request is successful
	 * @param failCallback a callback to be run if the request is unsuccessful
	 */
	@SuppressWarnings("SameParameterValue")
	public void partialAsynchronousSync(String identifier, JSONObject parameters, Runnable successCallback, Runnable failCallback) {
        if (canRunSync(identifier, parameters)) {
            PartialSyncTask task = new PartialSyncTask();
            task.parameters = parameters;
			task.sendModified = parameters == null;
            task.identifier = identifier;
			task.successCallback = successCallback;
			task.failCallback = failCallback;
            task.execute();
        } else {
			Log.d(TAG,"Sync already running");
		}
    }

	/**
	 * This method checks if a sync can be run. It will only be allowed if
	 * there isn't a sync already running.
	 *
	 * @param identifier the identifier of a particular {@link SyncManager}. This could be null.
	 * @param parameters {@link @JSONObject} containing parameters to be sent along with the request. This is used internally.
	 * @return boolean indicating if a sync can be run.
	 */
	public boolean canRunSync(@Nullable String identifier, @Nullable JSONObject parameters) {
		if (identifier == null && parameters == null) {
			return !isRunningSync;
		}

		Boolean flag = partialSyncFlag.get(identifier);
		return ((flag == null || !flag) && !isRunningSync)  || parameters != null;
	}

	/**
	 * This method checks if a sync can be run. It will only be allowed if
	 * there isn't a sync already running.
	 *
	 * @return boolean indicating if a sync can be run.
	 */
	public boolean canRunSync() {
		return canRunSync(null, null);
	}

	/**
	 * This method checks if any of the {@link SyncManager}s
	 * has data that should be sent to the server.
	 * @return boolean indicating if there is data to be sent.
	 */
	public boolean hasModifiedData() {
		for (SyncManager syncManager: syncConfig.getSyncManagers()) {
			if (syncManager.hasModifiedData()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sends an exception that was caught to Sentry.
	 * @param t the exception that was thrown.
	 */
	public void sendCaughtException(Throwable t) {
		try {
			Sentry.captureException(t);
			postBackgroundSyncError(t);
		} catch (Exception e) {
			throw new RuntimeException(t);
		}
	}
	/**
	 * This method cleans the {@link br.com.estudio89.syncing.ThreadChecker}.
	 * It is called when logging out, which assures that if a sync
	 * is running at that moment, no data will be saved to the
	 * database when it finishes as well as no timestamp will be
	 * changed.
	 */
	public void stopSyncThreads() {
		this.threadChecker.clear();
	}

	/**
	 * Adds an event to the event queue.
	 *
	 * @param syncManager the {@link SyncManager} that needs to post the event.
	 * @param objects the objects that should be added to the event.
	 */
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

	/**
	 * Posts all the events in the event queue.
	 */
	public void postEventQueue() {
		List<String> keys = new ArrayList<>();
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
	 * Posts an event indicating that all the data was sent.
	 */
	protected void postSendFinishedEvent() {
		bus.post(new SendFinishedEvent());
	}
	
	/**
	 * Posts an event indicating that all the data was received from the server.
	 */
	protected void postGetFinishedEvent() {
		bus.post(new GetFinishedEvent());
	}
	
	/**
	 * Posts an event indicating that a sync operation finished (fetching + sending).
	 */
	protected void postSyncFinishedEvent() {
		bus.post(new SyncFinishedEvent());
		Log.d(TAG, "=== Posted sync finished event bus hash:" + bus.hashCode());
	}

	/**
	 * Posts an event indicating that a partial sync operation finished (fetching + sending).
	 */
	protected void postPartialSyncFinishedEvent() {
		bus.post(new PartialSyncFinishedEvent());
		Log.d(TAG, "=== Posted partial sync finished event bus hash:" + bus.hashCode());
	}

	/**
	 * Posts an event indicating that there was an error during a sync operation.
	 *
	 * @param t the exception that occurred
	 */
	public void postBackgroundSyncError(Throwable t) {
		bus.post(new BackgroundSyncError(t));
	}

	/**
	 * Posts an event indicating that there was a connection error during a sync operation.
	 *
	 * @param t the exception that occurred
	 */
	public void postConnectionFailedError(Throwable t) {
		bus.post(new ConnectionFailedError(t));
	}
	/**
	 * Event posted when finished sending all the data to the server.
	 *
	 */
	public static class SendFinishedEvent {
		public SendFinishedEvent() {
		}
	}

	/**
	 * Event posted when finished fetching all the data from the server.
	 *
	 */
	public static class GetFinishedEvent {
		public GetFinishedEvent() {
		}
	}

	/**
	 * Event posted when finished a sync operation.
	 *
	 */
	public static class SyncFinishedEvent {
		public SyncFinishedEvent(){
		}
	}

	/**
	 * Event posted when finished a partial sync operation (fetching + sending).
	 *
	 */
	public static class PartialSyncFinishedEvent {
		public PartialSyncFinishedEvent(){
		}
	}

	/**
	 * Event posted when an exception was thrown during a sync operation.
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
	 * Event posted when there was a connection error during a sync operation.
	 *
	 */
	public static class ConnectionFailedError extends BackgroundSyncError {

		public ConnectionFailedError(Throwable t) {
			super(t);
		}
	}

	/**
	 * Class that runs a full sync operation in a background thread.
	 */
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

	/**
	 * Class that runs a partial sync operation in a background thread.
	 */
    class PartialSyncTask extends AsyncTask<Void,Void,Boolean> {
        public JSONObject parameters;
        public String identifier;
		public boolean sendModified;
		public Runnable successCallback;
		public Runnable failCallback;

        @Override
        protected Boolean doInBackground(Void... voids) {

            try {
				if (sendModified) {
					partialSynchronousSync(identifier);
				} else {
					partialSyncFlag.put(identifier, true);
                	getDataFromServer(identifier, parameters);
				}
            } catch (IOException e) {
                postBackgroundSyncError(e);
				return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            partialSyncFlag.put(identifier, false);
			if (successCallback != null && success) {
				successCallback.run();
			}

			if (failCallback != null && !success) {
				failCallback.run();
			}
        }
    }
}
