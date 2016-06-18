package br.com.estudio89.syncing.injection;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import br.com.estudio89.syncing.*;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.extras.ServerAuthenticate;
import br.com.estudio89.syncing.models.DatabaseReflectionUtil;
import br.com.estudio89.syncing.security.SecurityUtil;

import java.util.ArrayList;
import java.util.List;

public class SyncingInjection {
	private static List<Object> graph = new ArrayList<Object>();
    public static String LIBRARY_VERSION = "1.0.13";

	public static void init(Application application, String configFile, String baseURL) {
		init(application, configFile, baseURL, true);
	}
	/**
	 * Injects all dependencies in the classes in the sync module.
	 * 
	 * @param application an instance of the application
	 * @param initialSync boolean indicating if a sync operation should be run right after initialization.
	 */
	public static void init(Application application, String configFile, String baseURL, boolean initialSync) {

		// Checking interface
		if (!(application instanceof DatabaseProvider)) {
			throw new IllegalArgumentException("The application must implement the interface br.com.estudio89.syncing.DatabaseProvider!");
		}

		// Kickstarting injection
        executeInjection(application);

        SyncConfig syncConfig = get(SyncConfig.class);
        assert syncConfig != null;
        syncConfig.setConfigFile(configFile, baseURL);

		if (initialSync) { // Prevents sync operation from running in a different process
			DataSyncHelper.getInstance().fullAsynchronousSync();
		}
	}

    private static void executeInjection(Application application) {
        Context context = application;

        int appVersion;

        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        AsyncBus asyncBus = new AsyncBus();

        DatabaseReflectionUtil dataUtil = new DatabaseReflectionUtil(application);
        dataUtil.createDB();

        SyncConfig syncConfig = new SyncConfig(context, asyncBus, dataUtil);

        CustomTransactionManager customTransactionManager = new CustomTransactionManager();

        ThreadChecker threadChecker = new ThreadChecker();

        SecurityUtil securityUtil = new SecurityUtil(syncConfig);

        GzipUtil gzipUtil = new GzipUtil();

        ServerComm serverComm = new ServerComm(securityUtil, gzipUtil, appVersion);

        DataSyncHelper dataSyncHelper = new DataSyncHelper();
        dataSyncHelper.appContext = context;
        dataSyncHelper.bus = asyncBus;
        dataSyncHelper.syncConfig = syncConfig;
        dataSyncHelper.serverComm = serverComm;
        dataSyncHelper.transactionManager = customTransactionManager;
        dataSyncHelper.threadChecker = threadChecker;

        ServerAuthenticate serverAuthenticate = new ServerAuthenticate();
        serverAuthenticate.serverComm = serverComm;
        serverAuthenticate.syncConfig = syncConfig;
        serverAuthenticate.bus = asyncBus;

        syncConfig.setDataSyncHelper(dataSyncHelper);

        graph.add(context);
        graph.add(asyncBus);
        graph.add(syncConfig);
        graph.add(customTransactionManager);
        graph.add(threadChecker);
        graph.add(securityUtil);
        graph.add(gzipUtil);
        graph.add(serverComm);
        graph.add(dataSyncHelper);
        graph.add(serverAuthenticate);
        graph.add(dataUtil);
    }
	
	/**
	 * Returns an instance of the requested class with all its dependencies injected.
	 * 
	 * @param k class requested.
	 * @return instance of the class or null if not in the object graph.
	 */
	public static <E> E get(Class<E> k) {
        for (Object obj:graph) {
            if (k.isAssignableFrom(obj.getClass())) {
                return (E) obj;
            }
        }
		return null;
	}
}
