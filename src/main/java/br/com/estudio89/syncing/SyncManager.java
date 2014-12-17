package br.com.estudio89.syncing;
import br.com.estudio89.syncing.bus.AsyncBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;


/**
 * Essa é uma interface que deve ser implementada pela classe responsável por 
 * buscar os dados de um determinado model que necessitam ser sincronizados com 
 * o servidor.
 * Dessa forma, para cada model do projeto, deverá existir uma classe que 
 * implementa essa interface. 
 * 
 * A classe será responsável por:
 * <ul>
 * <li>Transformação de um objeto a partir de JSON ou para JSON (encoding e decoding)<li>
 * <li>Busca de quais objetos precisam ser sincronizados.</li>
 * <li>Persistir dados sincronizados no banco.</li>
 * <li>Lançar eventos de sincronização</li>
 * </ul>

 * @author luccascorrea
 *
 * @param <Model>
 */
public interface SyncManager <Model>{
	
	/**
	 * Deve retornar um identificador para o model.
	 * Esse identificador será utilizado na montagem do
	 * JSON a ser enviado ao servidor.
	 * Por exemplo, se o identificador for "teste",
	 * ao enviar dados ao servidor, o JSON criado será:
	 * <pre>
	 * {
	 *    ...
	 *    "teste":[obj1,obj2,...],
	 *    ...
	 * }
	 * </pre>
	 * 
	 * @return identificador do SyncManager/model.
	 */
	public String getIdentifier();
	
	/**
	 * Deve retornar um identificador para a resposta de
	 * envio de dados ao servidor. Por exemplo, se o servidor
	 * enviar como resposta uma lista contendo os novos ids dos 
	 * objetos no seguinte formato:
	 * <pre>
	 * {
	 *     ...
	 *     "teste_ids":[...],
	 *     ...
	 *  }
	 *  </pre>
	 *  
	 *  então o método deverá retornar "teste_ids".
	 *  
	 * @return identificador da resposta do servidor.
	 */
	public String getResponseIdentifier();
	
	/**
	 * Boolean que indica se os objetos devem ser enviados todos
	 * de uma vez ou um a um. Deve ser true para casos em que o
	 * objeto pode ter tamanho grande em disco, para facilitar
	 * o envio.
	 *  
	 * @return boolean de envio fragmentado.
	 */
	public boolean shouldSendSingleObject();
	
	/**
	 * Esse método é responsável por retornar um array JSON 
	 * contendo todos os objetos a serem enviados ao servidor. 
	 * Nesse método deve ser feita uma query no banco de dados 
	 * verificando se existem objetos que necessitam ser 
	 * enviados, por exemplo, verificando se seu id no servidor 
	 * é -1.
	 * @return
	 */
	public JSONArray getModifiedData();
	
	/**
	 * Esse método indica se existem dados a serem sincronizados.
	 * Deve ser feita uma query para verificar se o número de 
	 * objetos que precisam ser enviados ao servidor é maior que zero.
	 * Caso não existam objetos JSON a serem enviados, mas apenas arquivos,
	 * esse método ainda deve retornar true.
	 *
	 * @return
	 */
	public boolean hasModifiedData();
	
	/**
	 * Retorna uma lista contendo strings que representam o caminho
	 * dos arquivos que precisam ser enviados ao servidor.
	 * Caso o model não envie arquivos consigo, deve retornar uma
	 * lista vazia.
	 * 
	 * @return lista de strings com path de arquivos.
	 */
	public List<String> getModifiedFiles();
	
	/**
	 * Retorna uma lista contendo strings que representam o caminho
	 * dos arquivos que precisam ser enviados ao servidor
	 * por um determinado objeto.
	 * Caso o objeto não envie arquivos consigo, deve retornar uma
	 * lista vazia. Esse método é o que possibilita o envio de objetos 
	 * um a um.
	 * 
	 * @param object objeto JSON para o qual se deseja listar os arquivos.
	 * @return lista de strings com path de arquivos.
	 */
	public List<String> getModifiedFilesForObject(JSONObject object);
	
	/**
	 * Esse método é responsável por salvar um grupo de objetos definidos 
	 * em um array JSON. Cada objeto contido no array é decodificado e 
	 * salvo no banco.
	 * 
	 * @param jsonObjects objetos JSON a serem decodificados e salvos.
	 * @return lista de novos objetos criados.
	 */
	public List<Model> saveNewData(JSONArray jsonObjects, String deviceId);
	
	/**
	 * Esse método é responsável por processar a resposta do 
	 * envio de dados ao servidor para, por exemplo, atualizar 
	 * o id server de cada objeto.
	 *
	 * Esse método será chamado após o envio, mesmo que o servidor não tenha
	 * enviado uma resposta específica para esse model. Sendo assim, se para
	 * o SyncManager em questão, uma resposta nula do servidor não faria sentido,
	 * você deverá ignorá-la em sua implementação.
	 * 
	 * @param jsonResponse
	 */
	public void processSendResponse(JSONArray jsonResponse);
	
	/**
	 * Esse método é responsável por criar uma representação em JSON de um objeto.
	 * @param object
	 * @return
	 */
	public JSONObject serializeObject(Model object);
	
	/**
	 * Esse método é responsável por decodificar um objeto e 
	 * salvá-lo no banco de dados. Deve-se atentar para 
	 * verificar se o objeto sendo decodificado já existe (atualização) 
	 * ou ainda não (criação) no banco de dados.
	 * 
	 * @param object
	 * @return
	 */
	public Model saveObject(JSONObject object, String deviceId);
	
	/**
	 * Esse método envia um evento indicando que novos dados foram salvos no banco de dados. 
	 * É necessária a criação de uma classe pública e estática que defina o evento.
	 * A classe do evento deverá implementar a interface {@link SyncEvent}.
	 * 
	 * Para que uma outra classe se inscreva para receber o evento, basta
	 * criar um método com a anotação {@literal}Subscribe e recebendo um único
	 * paâmetro da classe de evento criada. Exemplo:
	 * 
	 * <pre>
	 * {@literal @}Subscribe
	 * public void onTestEvent(TestEvent event) {
	 *     ...
	 * }
	 * </pre>
	 *  @param objects objetos a serem enviados juntos com o evento.
	 * @param bus {@link com.squareup.otto.Bus} no qual deve ser postado o evento.
	 */
	public void postEvent(List<Model>objects, AsyncBus bus);
}
