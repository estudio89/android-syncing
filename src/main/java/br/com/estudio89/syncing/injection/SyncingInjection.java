package br.com.estudio89.syncing.injection;

import android.app.Application;
import android.content.Context;
import br.com.estudio89.syncing.*;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.extras.ServerAuthenticate;
import br.com.estudio89.syncing.security.SecurityUtil;

import java.util.ArrayList;
import java.util.List;

public class SyncingInjection {
	private static List<Object> graph = new ArrayList<Object>();

	public static void init(Application application, String configFile) {
		init(application, configFile, true);
	}
	/**
	 * Realiza a injeção de dependência nas classes
	 * do módulo de sincronização.
	 * 
	 * @param application
	 * @param initialSync conduz uma sincronização completa ao iniciar.
	 */
	public static void init(Application application, String configFile, boolean initialSync) {

		// Checking interface
		if (!(application instanceof DatabaseProvider)) {
			throw new IllegalArgumentException("A aplicação precisa implementar a interface br.com.estudio89.syncing.DatabaseProvider!");
		}

		// Kickstarting injection
        executeInjection(application);

        SyncConfig syncConfig = get(SyncConfig.class);
		syncConfig.setConfigFile(configFile);
		String processName = syncConfig.getProcessName((Context) application);
		if (initialSync && !processName.endsWith(":auth")) { // Impede que seja realizada sincronização em outro processo
			DataSyncHelper.getInstance().fullAsynchronousSync();
		}
	}

    private static void executeInjection(Application application) {
        Context context = (Context) application;

        AsyncBus asyncBus = new AsyncBus();

        SyncConfig syncConfig = new SyncConfig(context, asyncBus);

        CustomTransactionManager customTransactionManager = new CustomTransactionManager();

        ThreadChecker threadChecker = new ThreadChecker();

        SecurityUtil securityUtil = new SecurityUtil(syncConfig);

        ServerComm serverComm = new ServerComm(securityUtil);

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


        graph.add(context);
        graph.add(asyncBus);
        graph.add(syncConfig);
        graph.add(customTransactionManager);
        graph.add(threadChecker);
        graph.add(securityUtil);
        graph.add(serverComm);
        graph.add(dataSyncHelper);
        graph.add(serverAuthenticate);
    }
	
	/**
	 * Retorna uma classe com suas dependências satisfeitas.
	 * 
	 * @param k classe desejada.
	 * @return
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
