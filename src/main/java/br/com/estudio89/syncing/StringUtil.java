package br.com.estudio89.syncing;

import java.util.List;

/**
 * Created by luccascorrea on 7/2/15.
 *
 */
public class StringUtil {

    public static String join(long[] array, String delimiter) {
        if (array.length == 1) {
            return String.valueOf(array[0]);
        }
        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (long id : array) {
            sb.append(delim).append(id);
            delim = delimiter;
        }

        String result = sb.toString();
        if (result.startsWith(",")) {
            result = result.substring(1);
        }

        return result;
    }

    public static String join(List<Long> array, String delimiter) {
        if (array.size() == 1) {
            return String.valueOf(array.get(0));
        }

        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (long id : array) {
            sb.append(delim).append(id);
            delim = delimiter;
        }

        String result = sb.toString();
        if (result.startsWith(",")) {
            result = result.substring(1);
        }

        return result;
    }

    public static String join(Integer[] array, String delimiter) {
        if (array.length == 1) {
            return String.valueOf(array[0]);
        }

        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (long id : array) {
            sb.append(delim).append(id);
            delim = delimiter;
        }
        String result = sb.toString();
        if (result.startsWith(",")) {
            result = result.substring(1);
        }

        return result;
    }
}

