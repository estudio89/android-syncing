package br.com.estudio89.syncing.injection;

import javax.inject.Singleton;

import android.app.Application;
import android.content.Context;
import dagger.Module;
import dagger.Provides;

@Module(
		injects = {
				Context.class,
		},
		includes = {
			DataSyncHelperModule.class
		}
)
public class AppContextModule {
	private Application app;
	
	public AppContextModule(Application app) {
		this.app = app;
	}
	
	@Provides @Singleton public Context provideAppContext() {
		return app;
	}
}
