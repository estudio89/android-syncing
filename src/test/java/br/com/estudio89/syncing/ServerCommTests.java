package br.com.estudio89.syncing;

import br.com.estudio89.syncing.exceptions.Http403Exception;
import br.com.estudio89.syncing.exceptions.Http408Exception;
import br.com.estudio89.syncing.exceptions.Http500Exception;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.URL;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by luccascorrea on 11/28/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ServerCommTests {
    static MockWebServer server = new MockWebServer();

    URL url;

    ServerComm serverComm;

    @BeforeClass
    public static void startServer() throws Exception {
        server.play();
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        url = server.getUrl("/");

        serverComm = Mockito.spy(new ServerComm());
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.shutdown();
    }

    /**
     * Mais informações MockWebServer: https://github.com/square/okhttp/tree/master/mockwebserver
     *
     * @throws Exception
     */
    @Test(expected = Http403Exception.class)
    public void test403Response() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(403));

        JSONObject object = new JSONObject();
        object.put("token","123");

        serverComm.post(url.toString(),object);

    }

    @Test(expected = Http408Exception.class)
    public void test408Response() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(408));
        JSONObject object = new JSONObject();
        object.put("token","123");

        serverComm.post(url.toString(),object);
    }

    @Test(expected = Http500Exception.class)
    public void test500Response() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));
        JSONObject object = new JSONObject();
        object.put("token","123");

        serverComm.post(url.toString(),object);
    }

    @Test(expected = Http403Exception.class)
    public void testContentType() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type","text/html; charset=utf-8"));
        JSONObject object = new JSONObject();
        object.put("token","123");

        serverComm.post(url.toString(),object);
    }
}
