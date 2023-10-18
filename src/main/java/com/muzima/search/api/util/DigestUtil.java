/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtil {

    static final byte[] HEX_CHAR_TABLE = {
            (byte) '0', (byte) '1', (byte) '2', (byte) '3',
            (byte) '4', (byte) '5', (byte) '6', (byte) '7',
            (byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
            (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
    };

    private static MessageDigest getDigest(final String defaultAlgorithm) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(defaultAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Unable to find that algorithm for the message digest!", e);
        }
        return digest;
    }

    private static byte[] createChecksum(final InputStream inputStream) throws IOException {
        MessageDigest digest = getDigest("SHA1");
        try {
            int count;
            byte[] buffer = new byte[1024];
            while ((count = inputStream.read(buffer)) != -1)
                digest.update(buffer, 0, count);
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
        return digest.digest();
    }

    private static String getHexString(final byte[] raw) throws UnsupportedEncodingException {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, "ASCII");
    }

    public static String getSHA1Checksum(final String data) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(data.getBytes());
        return getHexString(createChecksum(inputStream));
    }

    public static String getSHA1Checksum(final File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        return getHexString(createChecksum(inputStream));
    }
}
