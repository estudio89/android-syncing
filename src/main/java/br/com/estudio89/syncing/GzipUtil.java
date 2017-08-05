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
        GZIPInputStream zis;

        final StringBuilder outStr = new StringBuilder();
        try {
            zis = new GZIPInputStream(new BufferedInputStream(is));
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(zis, "UTF-8"));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outStr.append(line);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outStr.toString();
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
