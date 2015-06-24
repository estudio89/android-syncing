package br.com.estudio89.syncing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by luccascorrea on 6/23/15.
 */
public class TestUtil {
    public static String loadResource(String filename) throws IOException, JSONException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        inputStream.read(buffer);
        inputStream.close();

        String jsonString = new String(buffer, "UTF-8");
        return jsonString;
    }

    /**
     * Carrega um objeto json de teste da pasta resources.
     *
     * @param filename nome do arquivo.
     * @return objeto JSON correspondente.
     * @throws IOException
     * @throws JSONException
     */
    public static JSONObject loadJsonResource(String filename) throws IOException, JSONException {
        String jsonString = loadResource(filename);
        return new JSONObject(jsonString);
    }

    /**
     * Carrega um array json de teste da pasta resources.
     *
     * @param filename nome do arquivo.
     * @return array json correspondente.
     * @throws IOException
     * @throws JSONException
     */
    public static JSONArray loadJsonArrayResource(String filename) throws IOException, JSONException {
        String jsonString = loadResource(filename);
        return new JSONArray(jsonString);
    }
}
