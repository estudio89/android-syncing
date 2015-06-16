package br.com.estudio89.syncing;

import br.com.estudio89.syncing.exceptions.*;
import br.com.estudio89.syncing.injection.SyncingInjection;
import br.com.estudio89.syncing.security.SecurityUtil;
import com.squareup.okhttp.*;
import org.cryptonode.jncryptor.CryptorException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Classe responsável por realizar requisições ao servidor.
 * 
 * @author luccascorrea
 *
 */
public class ServerComm {
	public static final MediaType JSON = MediaType.parse("application/json; charset=ISO-8859-1");
	public static final MediaType IMAGE_JPEG = MediaType.parse("image/jpeg;");
	OkHttpClient client = new OkHttpClient();
    SecurityUtil securityUtil;

    public ServerComm(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
		client.setConnectTimeout(1, TimeUnit.MINUTES);
		client.setReadTimeout(1, TimeUnit.MINUTES);
    }

	public static ServerComm getInstance() {
		return SyncingInjection.get(ServerComm.class);
	}

	/**
	 * Envia um post ao servidor contendo no corpo da requisição o json do parâmetro data.
	 *
	 * @param url
	 * @param data
	 * @return
	 */
	public JSONObject post(String url, JSONObject data) throws IOException {

		return post(url, data, null);

	}

	/**
	 * Envia um post ao servidor contendo no corpo da requisição o json do parâmetro data assim como
	 * todos os arquivos listados no parâmetro files.
	 *
	 * @param url
	 * @param data
	 * @param files
	 * @return
	 */
	public JSONObject post(String url, JSONObject data, List<String> files) throws IOException {
		MultipartBuilder builder = new MultipartBuilder().type(MultipartBuilder.FORM);
        try {
            builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"json\""), RequestBody.create(JSON, securityUtil.encryptMessage(data.toString())));
        } catch (CryptorException e) {
            throw new RuntimeException(e);
        }

        if (files != null) {
			for (String path:files ){
				File file = new File(path);
				builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + file.getName() +"\"; filename=\"" + file.getName() + "\""), RequestBody.create(IMAGE_JPEG,file));
			}

		}

		RequestBody body = builder.build();
		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();
		Response response = null;

		response = client.newCall(request).execute();
		String contentType = response.header("Content-Type", "");

		if (!response.isSuccessful()) {
			switch (response.code()) {
				case 403:
					throw new Http403Exception();
				case 408:
					throw new Http408Exception();
				case 500:
					throw new Http500Exception();
				case 502:
					throw new Http502Exception();
				case 503:
					throw new Http503Exception();
				default:
					throw new IOException();
			}
		} else if (!contentType.contains("application/json")) { // Requisição barrada antes de chegar ao servidor.
			throw new Http403Exception();
		}

		JSONObject responseJson = null;
		try {
			responseJson = new JSONObject(securityUtil.decryptMessage(response.body().string()));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (CryptorException e) {
            throw new RuntimeException(e);
        }

        return responseJson;
	}
	
}
