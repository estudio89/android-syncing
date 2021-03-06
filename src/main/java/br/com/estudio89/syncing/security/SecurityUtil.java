package br.com.estudio89.syncing.security;

import br.com.estudio89.syncing.SyncConfig;
import br.com.estudio89.syncing.injection.SyncingInjection;
import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.CryptorException;
import org.cryptonode.jncryptor.JNCryptor;

import java.io.UnsupportedEncodingException;

/**
 * Created by luccascorrea on 4/16/15.
 *
 * This class takes care of encrypting and decrypting request bodies.
 */
public class SecurityUtil {

    public SyncConfig syncConfig;

    public SecurityUtil(SyncConfig syncConfig) {
        this.syncConfig = syncConfig;
    }

    public static SecurityUtil getInstance() {
        return SyncingInjection.get(SecurityUtil.class);
    }

    public byte[] encryptMessage(String message) throws CryptorException, UnsupportedEncodingException {
        byte[] plainText = message.getBytes();
        return encryptMessage(plainText);
    }

    public byte[] encryptMessage(byte[] plainText) throws CryptorException, UnsupportedEncodingException {
        byte[] cipherText;
        if (syncConfig.isEncryptionActive()) {

            AES256JNCryptor cryptor = new AES256JNCryptor();
            cryptor.setPBKDFIterations(100);
            String password = syncConfig.getEncryptionPassword();
            cipherText = cryptor.encryptData(plainText,password.toCharArray());
        } else {
            cipherText = plainText;
        }

        return cipherText;
    }

    public byte[] decryptMessage(byte[] cipherText) throws UnsupportedEncodingException, CryptorException {
        byte[] plainText;

        if (syncConfig.isEncryptionActive()) {
            JNCryptor cryptor = new AES256JNCryptor();
            cryptor.setPBKDFIterations(100);
            String password = syncConfig.getEncryptionPassword();
            plainText = cryptor.decryptData(cipherText, password.toCharArray());
        } else {
            plainText = cipherText;
        }

        return plainText;
    }

}
