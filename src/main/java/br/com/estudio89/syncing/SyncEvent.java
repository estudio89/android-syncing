package br.com.estudio89.syncing;
import java.util.List;


public interface SyncEvent<Model> {
	
	/**
	 * Retorna uma lista com todos os novos
	 * objetos criados.
	 * 
	 * @return lista de novos objetos.
	 */
	public List<Model> getObjects();
}
