package br.com.estudio89.syncing.injection;

import android.content.Context;
import br.com.estudio89.syncing.*;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.extras.ServerAuthenticate;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;


@Module(
		injects = {
				DataSyncHelper.class,
				CustomTransactionManager.class,
				ServerComm.class,
				SyncConfig.class,
				AsyncBus.class,
				ThreadChecker.class,
				ServerAuthenticate.class
		},
		complete = false
		)
public class DataSyncHelperModule {

	@Provides public CustomTransactionManager provideCustomTransactionManager() {
		return new CustomTransactionManager();
	}
	
	@Provides public ServerComm provideServerComm() {
		return new ServerComm();
	}
	
	@Provides @Singleton
	public SyncConfig provideSyncConfig(Context context, AsyncBus bus) {
		return new SyncConfig(context, bus);
	}
	
	@Provides @Singleton public AsyncBus provideBus() {
		return new AsyncBus();
	}

	@Provides @Singleton public ThreadChecker provideThreadChecker() {return new ThreadChecker(); }

}
