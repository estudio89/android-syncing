package br.com.estudio89.syncing.security;

import br.com.estudio89.syncing.SyncConfig;
import br.com.estudio89.syncing.injection.SyncingInjection;
import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.CryptorException;
import org.cryptonode.jncryptor.JNCryptor;

import java.io.UnsupportedEncodingException;

/**
 * Created by luccascorrea on 4/16/15.
 */
public class SecurityUtil {

    public SyncConfig syncConfig;

    public SecurityUtil(SyncConfig syncConfig) {
        this.syncConfig = syncConfig;
    }

    public static SecurityUtil getInstance() {
        return SyncingInjection.get(SecurityUtil.class);
    }

    public String encryptMessage(String message) throws CryptorException, UnsupportedEncodingException {

        if (syncConfig.isEncryptionActive()) {

            byte[] plainText = message.getBytes("ISO-8859-1");
            AES256JNCryptor cryptor = new AES256JNCryptor();
            String password = syncConfig.getEncryptionPassword();
            byte[] cipherText = cryptor.encryptData(plainText,password.toCharArray());
            message = new String(cipherText, "ISO-8859-1");
        }

        return message;
    }

    public String decryptMessage(String message) throws UnsupportedEncodingException, CryptorException {

        if (syncConfig.isEncryptionActive()) {
            JNCryptor cryptor = new AES256JNCryptor();
            byte[] cipherText = message.getBytes("ISO-8859-1");
            String password = syncConfig.getEncryptionPassword();
            byte[] plainText = cryptor.decryptData(cipherText, password.toCharArray());
            message = new String(plainText, "ISO-8859-1");
        }

        return message;
    }

}
