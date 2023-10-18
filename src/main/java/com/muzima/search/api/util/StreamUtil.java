/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

/**
 * Created after reading the IOUtils from Apache's commons-io
 */
public class StreamUtil {

    /**
     * The default buffer size to use.
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static String readAsString(final Reader reader) throws IOException {

        char[] buffer = new char[DEFAULT_BUFFER_SIZE];

        StringWriter writer = new StringWriter();
        BufferedReader bufferedReader = new BufferedReader(reader);

        int count;
        while ((count = bufferedReader.read(buffer)) != -1)
            writer.write(buffer, 0, count);
        return writer.toString();
    }
}
