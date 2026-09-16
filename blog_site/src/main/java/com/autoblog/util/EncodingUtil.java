package com.autoblog.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class EncodingUtil {
    private static final Charset LATIN1 = StandardCharsets.ISO_8859_1;

    private EncodingUtil() {}

    public static String repairMojibake(String value) {
        if (value == null || value.length() == 0) {
            return value;
        }
        if (!looksLikeMojibake(value)) {
            return value;
        }
        try {
            String repaired = new String(value.getBytes(LATIN1), StandardCharsets.UTF_8);
            if (scoreMojibake(repaired) < scoreMojibake(value)) {
                return repaired;
            }
        } catch (Exception ignored) {
            return value;
        }
        return value;
    }

    private static boolean looksLikeMojibake(String value) {
        return value.indexOf('Ã') >= 0
                || value.indexOf('Â') >= 0
                || value.indexOf('ë') >= 0
                || value.indexOf('ì') >= 0
                || value.indexOf('í') >= 0
                || value.indexOf('ê') >= 0
                || value.indexOf('ð') >= 0;
    }

    private static int scoreMojibake(String value) {
        int score = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == 'Ã' || ch == 'Â' || ch == 'ë' || ch == 'ì' || ch == 'í' || ch == 'ê' || ch == 'ð') {
                score += 2;
            }
            if (ch == '\uFFFD') {
                score += 5;
            }
        }
        return score;
    }
}
