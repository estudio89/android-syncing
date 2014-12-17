package br.com.estudio89.syncing.bus;


import br.com.estudio89.syncing.injection.SyncingInjection;
import com.squareup.otto.Bus;

/**
 * Classe que disponibiliza o BUS utilizado
 * para comunicação de eventos de sincronização.
 * 
 * @author luccascorrea
 *
 */
public class EventBusManager {
	
	/**
	 * Retorna o {@link Bus} utilizado
	 * para comunicação de eventos de 
	 * sincronização.
	 * 
	 */
	public static AsyncBus getBus() {
		return (AsyncBus) SyncingInjection.get(AsyncBus.class);
	}
}
