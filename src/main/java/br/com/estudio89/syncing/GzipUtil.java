package br.com.estudio89.syncing;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by luccascorrea on 10/13/15.
 */
public class GzipUtil {

    public String decompressMessage(byte[] compressedMsg) {
        InputStream is = new ByteArrayInputStream(compressedMsg);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        GZIPInputStream zis;
        int nRead;
        byte[] data = new byte[16384];

        try {
            zis = new GZIPInputStream(new BufferedInputStream(is));
            while ((nRead = zis.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new String(buffer.toByteArray());
    }

    public String compressMessage(String uncompressedMessage) {
        byte[] bytes = uncompressedMessage.getBytes();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        GZIPOutputStream zos = null;
        try {
            zos = new GZIPOutputStream(new BufferedOutputStream(buffer));
            zos.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new String(buffer.toByteArray());
    }
}
