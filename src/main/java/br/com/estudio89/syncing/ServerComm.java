package br.com.estudio89.syncing;

import android.util.Log;
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
 * This class is responsible for making requests to the server
 * and parsing the response as json.
 *
 */
public class ServerComm {
	public static final MediaType OCTET = MediaType.parse("application/octet-stream");
	public static final MediaType IMAGE_JPEG = MediaType.parse("image/jpeg;");
	public static final String HEADER_APP_VERSION = "X-APP-VERSION";
	public static final String HEADER_VERSION = "X-E89-SYNCING-VERSION";
	public static final String HEADER_GZIP = "X-SECURITY-GZIP";
	public static final String HEADER_PLATFORM = "X-E89-SYNCING-PLATFORM";
	OkHttpClient client = new OkHttpClient();
    SecurityUtil securityUtil;
	GzipUtil gzipUtil;
	int appVersion;

    public ServerComm(SecurityUtil securityUtil, GzipUtil gzipUtil, int appVersion) {
        this.securityUtil = securityUtil;
		this.gzipUtil = gzipUtil;
		this.appVersion = appVersion;
		client.setConnectTimeout(1, TimeUnit.MINUTES);
		client.setReadTimeout(1, TimeUnit.MINUTES);
    }

	public static ServerComm getInstance() {
		return SyncingInjection.get(ServerComm.class);
	}

	/**
	 * Makes a post request to the server including json in its body.
	 *
	 * @param url the url
	 * @param data the data that should be sent in the request body
	 * @return the json response
	 */
	public JSONObject post(String url, JSONObject data) throws IOException {

		return post(url, data, null);

	}

	/**
	 * Makes a post request to the server including json in its body as well as the files passed.
	 *
	 * @param url the url
	 * @param data the data that should be sent in the request body
	 * @param files files that should be sent along with the request
	 * @return the json response
	 */
	public JSONObject post(String url, JSONObject data, List<String> files) throws IOException {
		MultipartBuilder builder = new MultipartBuilder().type(MultipartBuilder.FORM);
        try {
			byte[] compressed = gzipUtil.compressMessage(data.toString());
			byte[] encrypted = securityUtil.encryptMessage(compressed);
            builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"json\""), RequestBody.create(OCTET, encrypted));
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
				.addHeader(HEADER_VERSION, SyncingInjection.LIBRARY_VERSION)
				.addHeader(HEADER_GZIP, "true")
				.addHeader(HEADER_PLATFORM, "android")
				.addHeader(HEADER_APP_VERSION, String.valueOf(appVersion))
				.build();

		Response response;

		Log.d("ServerComm", "Starting request: " + url);
		response = client.newCall(request).execute();
		Log.d("ServerComm", "Finished request: " + url);
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
				case 504:
					throw new Http504Exception();
				default:
					throw new IOException("Unsuccessful request to url " + url + " - received response with status code " + response.code());
			}
		} else if (!contentType.contains("application/octet-stream")) { // Requisição barrada antes de chegar ao servidor.
			throw new Http403Exception();
		}

		JSONObject responseJson;
		try {
			byte[] bodyBytes =  response.body().bytes();
			byte[] decrypted = securityUtil.decryptMessage(bodyBytes);
			String decompressed = gzipUtil.decompressMessage(decrypted);
			responseJson = new JSONObject(decompressed);
		} catch (JSONException | CryptorException e) {
			throw new RuntimeException(e);
		}

		return responseJson;
	}
	
}
