package br.com.estudio89.syncing;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

/**
 * Classe responsável por executar operações no banco de dados
 * em uma única transação.
 * 
 * 
 * @author luccascorrea
 *
 */
public class CustomTransactionManager {
	private boolean isSuccessful = false;
	
	public void doInTransaction(CustomTransactionManager.Callback callback, Context context, SyncConfig syncConfig) {
		isSuccessful = false;
		SQLiteDatabase database = syncConfig.getDatabase();
		database.beginTransaction();
		
		try {
			callback.manipulateInTransaction();
			database.setTransactionSuccessful();
			isSuccessful = true;
		} catch (InterruptedException e) {
			
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			try {
				database.endTransaction();
			} catch (SQLiteException e) {

			}

		}
	}
	
	public boolean wasSuccesful() {
		return isSuccessful;
	}
	
	/**
	 * Interface para realização de operações no banco de dados
	 * em uma única transação.
	 * 
	 * @author luccascorrea
	 *
	 */
	public static interface Callback {
		void manipulateInTransaction() throws InterruptedException;
	}
}
