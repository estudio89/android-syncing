package br.com.estudio89.syncing;

import br.com.estudio89.syncing.security.SecurityUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by luccascorrea on 4/16/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SecuryUtilTests {
    @Mock
    SyncConfig syncConfig;

    SecurityUtil securityUtil;

    @Before public void setUp() throws Exception{
        initMocks(this);

        Mockito.when(syncConfig.isEncryptionActive()).thenReturn(true);
        Mockito.when(syncConfig.getEncryptionPassword()).thenReturn("1234");

        // SecurityUtil - manual injection
        securityUtil = new SecurityUtil(syncConfig);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @Test
    public void testEncryptionActive() throws Exception {
        // Encryption/Decryption
        String message = "Ação";
        String encrypted = securityUtil.encryptMessage(message);
        String decrypted = securityUtil.decryptMessage(encrypted);

        Assert.assertEquals(message, decrypted);

        // Python encryption/Java decryption
        byte[] pythonEnc = hexStringToByteArray("030168242fc24a43ad7c0ad2e6b91e87ae4898cba42fbb65416e9dcdefa8d67048a3855d45d87591f4f27ff8e43e7b5a2955a411c12d13ad5e2d2f838dd3c97bc3e2582b15d0d036c6342454629e3c5ab679");
        decrypted = securityUtil.decryptMessage(new String(pythonEnc,"ISO-8859-1"));

        Assert.assertEquals(message, decrypted);

        byte[] javaEnc = hexStringToByteArray("03014fcf00374cbbbc086efee7a6829c1c0b159c301c9bee2f066aee67e90c4962cbee3f840f0d3564942449eda953ac05bd38a7eafc01beb197236b3365363e551fb53a288124f487a16787b44fbb30a3fe4be33bda2387e31fb1429a8ec8988e205fa7525fccb45019a521831f5d52e00f6c743abab3988b500f9f91531d50edaac01c876060deb4469b71cd7bb6fea2d6cb615646a1f3fa49b088b0270e9ba2de");
        encrypted = new String(javaEnc,"ISO-8859-1");
        System.out.println(Arrays.toString(new String(javaEnc,"ISO-8859-1").getBytes("ISO-8859-1")));
        decrypted = securityUtil.decryptMessage(new String(javaEnc,"ISO-8859-1"));
        System.out.println(decrypted);
    }

    @Test
    public void testEncryptionInactive() throws Exception {
        Mockito.when(syncConfig.isEncryptionActive()).thenReturn(false);
        String message = "Luccas";
        String encrypted = securityUtil.encryptMessage(message);
        String decrypted = securityUtil.decryptMessage(encrypted);

        Assert.assertEquals(message,decrypted);
        Assert.assertEquals(message,encrypted);

    }
}
