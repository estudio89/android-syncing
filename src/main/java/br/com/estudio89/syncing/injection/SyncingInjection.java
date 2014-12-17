package br.com.estudio89.syncing.injection;

import android.app.Application;
import android.content.Context;
import br.com.estudio89.syncing.DataSyncHelper;
import br.com.estudio89.syncing.DatabaseProvider;
import br.com.estudio89.syncing.SyncConfig;
import dagger.ObjectGraph;

import java.util.Arrays;
import java.util.List;

public class SyncingInjection {
	private static ObjectGraph graph;

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

		graph = ObjectGraph.create(getModules(application).toArray());

		// Checking interface
		if (!(application instanceof DatabaseProvider)) {
			throw new IllegalArgumentException("A aplicação precisa implementar a interface br.com.estudio89.syncing.DatabaseProvider!");
		}

		// Kickstarting injection
		Context context = graph.get(Context.class);
		SyncConfig syncConfig = graph.get(SyncConfig.class);
		syncConfig.setConfigFile(configFile);
		String processName = syncConfig.getProcessName(context);
		if (initialSync && !processName.endsWith(":auth")) { // Impede que seja realizada sincronização em outro processo
			DataSyncHelper.getInstance().fullAsynchronousSync();
		}
	}

	/**
	 * Injeta dependências.
	 * 
	 * @param object
	 */
	public static void inject(Object object) {
		graph.inject(object);
	}
	
	/**
	 * Retorna os módulos responsáveis pelas injeções de
	 * dependência no projeto.
	 * 
	 * @param application
	 * @return
	 */
	private static List<Object> getModules(Application application) {
		return Arrays.<Object>asList(new AppContextModule(application));
	}
	
	/**
	 * Retorna uma classe com suas dependências satisfeitas.
	 * 
	 * @param k classe desejada.
	 * @return
	 */
	public static <E> E get(Class<E> k) {
		return graph.get(k);
	}
}
