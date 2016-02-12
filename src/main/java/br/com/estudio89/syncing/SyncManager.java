package br.com.estudio89.syncing;

import android.content.Context;
import br.com.estudio89.syncing.bus.AsyncBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;


/**
 * This interface must be implemented by classes responsible for synchronizing
 * data of a particular model. Therefore, for each model present in the application,
 * a {@link SyncManager} must be implemented.
 *
 * This class is responsible for:
 * <ul>
 * <li>Serializing an object to json and decoding it from json<li>
 * <li>Finding which objects should be sent to the server</li>
 * <li>Persist synchronized items to the database.</li>
 * <li>Post sync events</li>
 * </ul>

 * @author luccascorrea
 *
 * @param <Model> The model class the sync manager handles
 */
public interface SyncManager <Model>{

	/**
	 * This method will be called right after the SyncManager is instantiated
	 * by SyncConfig.
	 * @param dataSyncHelper
	 */
	void setDataSyncHelper(DataSyncHelper dataSyncHelper);

	/**
	 * It must return a unique identifier for this manager.
	 * This identifier will be used when creating the json
	 * object that will be sent to the server.
	 * 
	 * @return the identifier
	 */
	String getIdentifier();
	
	/**
	 * Must return an identifier for the response
	 * received after sending the data to the server.
	 * This identifier will be used when parsing the server's response
	 * so as to identify which data should be passed to this manager.
	 *
	 * @return the response identifier
	 */
	String getResponseIdentifier();
	
	/**
	 * Boolean indicating if the objects must be sent
	 * all at once or one by one.
	 *
	 * It should be true for situations where the object
	 * could occupy a large size in the disk and thus
	 * sending it one at a time eases the transfer.
	 *  
	 * @return true if objects should be sent one by one or false otherwise.
	 */
	boolean shouldSendSingleObject();

	boolean hasTimestamp();
	/**
	 * This method is responsible for returning a json array
	 * containing all the data that should be sent to the server.
	 *
	 * @return the data the should be sent
	 */
	JSONArray getModifiedData();
	
	/**
	 * This method indicates if there are data waiting to be synced.
	 *
	 * @return true if data needs to be synced and false otherwise
	 */
	boolean hasModifiedData();
	
	/**
	 * Returns a list containing strings that represent
	 * the path to files that should be sent to the server.
	 * If the model never sends files along with it,
	 * this should return an empty list.
	 * 
	 * @return list of strings with file paths.
	 */
	List<String> getModifiedFiles();
	
	/**
	 * Returns a list containing strings that represent the path of
	 * files that should be sent to the server along with a particular object.
	 *
	 * If objects never send files along with them, this method should
	 * return an empty list.
	 *
	 * @param object json object whose files must be listed.
	 * @return list of strings with file paths.
	 */
	List<String> getModifiedFilesForObject(JSONObject object);
	
	/**
	 * This method is responsible for saving a group of objects defined in
	 * a json array.
	 * 
	 * @param jsonObjects json objects that should be decoded and saved.
	 * @return list containing all the objects that were saved.
	 */
	List<Model> saveNewData(JSONArray jsonObjects, String deviceId, JSONObject responseParameters, Context context);
	
	/**
	 * This method is responsible for processing
	 * the response received after sending the data to the server.
	 *
	 * This method is called after sending data even if the
	 * server did not send a response specifically to this manager. Therefore
	 * if for your particular manager, the server never sends a response and
	 * an empty response is meaningless, this method can return an empty list.
	 *
	 * @param jsonResponse the response sent by the server
	 * @return a list of items that were updated.
	 */
	List<Model> processSendResponse(JSONArray jsonResponse);
	
	/**
	 * This method is responsible for turning the model instance into a json object.
	 *
	 * @param object the model instance
	 * @return the json object
	 */
	JSONObject serializeObject(Model object);
	
	/**
	 * This method is responsible for decoding a json object into a model instance
	 * and saving it to the database. The json object passed here may be new
	 * of an updated instance.
	 *
	 * @param object the json object to be inserted or updated
	 * @return the model object
	 */
	Model saveObject(JSONObject object, String deviceId, Context context);
	
	/**
	 * This method posts an event indicating that new data were saved by this manager.
	 * It is necessary to create a public static class that implements {@link SyncEvent}
	 * defining this manager's events.
	 *
	 * When a class needs to subscribe to this event, it is necessary to create
	 * a method with the annotation {@literal}Subscribe that should receive a single
	 * parameter that is an instance of the event class.
	 *
	 *  @param objects objects to be sent along with the event
	 * @param bus {@link com.squareup.otto.Bus} that must be used when posting the event.
	 */
	void postEvent(List<Model>objects, AsyncBus bus, Context context);

	/**
	 * Returns the maximum delay in seconds before responding to a push message.
	 * @return
	 */
	int getDelay();
}
