package br.com.estudio89.syncing;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by luccascorrea on 12/2/14.
 */
public interface DatabaseProvider {
    public SQLiteDatabase getApplicationDatabase();
}
