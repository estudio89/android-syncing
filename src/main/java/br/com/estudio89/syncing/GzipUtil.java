package br.com.estudio89.syncing;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by luccascorrea on 10/13/15.
 *
 * Gzips a request before sending to the server and ungzips response.
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

    public byte[] compressMessage(String uncompressedMessage) {
        byte[] bytes = new byte[0];
        try {
            bytes = uncompressedMessage.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        GZIPOutputStream zos;
        try {
            zos = new GZIPOutputStream(new BufferedOutputStream(buffer));
            zos.write(bytes);
            zos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return buffer.toByteArray();
    }
}
