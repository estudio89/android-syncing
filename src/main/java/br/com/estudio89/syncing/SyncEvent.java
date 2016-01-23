package br.com.estudio89.syncing;
import java.util.List;


public interface SyncEvent<Model> {
	
	/**
	 * Returns a list with all the new objects inserted in the database or updated.
	 * 
	 * @return list of updated or inserted objects.
	 */
	List<Model> getObjects();
}
