/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.util;

import java.io.File;

public class FilenameUtil {

    public static String getExtension(final File file) {
        String extension = null;
        String fileName = file.getName();
        int i = fileName.lastIndexOf('.');

        if (i > 0 && i < fileName.length() - 1) {
            extension = fileName.substring(i + 1).toLowerCase();
        }
        return extension;
    }

    public static boolean contains(final String filename, final String subset) {
        return filename.toLowerCase().contains(subset.toLowerCase());
    }
}
