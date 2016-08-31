package br.com.estudio89.syncing;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.IOException;

/**
 * Executes operations in the database inside a single transaction.
 *
 */
public class CustomTransactionManager {
	private boolean isSuccessful = false;
	
	public void doInTransaction(CustomTransactionManager.Callback callback, @SuppressWarnings("UnusedParameters") Context context, SyncConfig syncConfig) throws IOException {
		isSuccessful = false;
		SQLiteDatabase database = syncConfig.getDatabase();
		database.beginTransaction();
		
		try {
			callback.manipulateInTransaction();
			database.setTransactionSuccessful();
			isSuccessful = true;
		} catch (InterruptedException ignored) {

		} catch (IOException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			try {
				database.endTransaction();
			} catch (SQLiteException ignored) {

			}

		}
	}
	
	public boolean wasSuccesful() {
		return isSuccessful;
	}
	
	/**
	 * Interface for running operations in the database in a single transaction.
	 * 
	 * @author luccascorrea
	 *
	 */
	public interface Callback {
		void manipulateInTransaction() throws InterruptedException, IOException;
	}
}
