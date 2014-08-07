/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.internal.file;

import com.muzima.search.api.util.FilenameUtil;
import com.muzima.search.api.util.StringUtil;

import java.io.File;
import java.io.FileFilter;

public class ResourceFileFilter implements FileFilter {

    public static final String RESOURCE_FILE_EXTENSION = "j2l";

    public static final String JSON_FILE_EXTENSION = "json";

    @Override
    public boolean accept(final File file) {
        if (file.isDirectory())
            return true;

        String extension = FilenameUtil.getExtension(file);
        return (StringUtil.equals(extension, RESOURCE_FILE_EXTENSION)
                || StringUtil.equals(extension, JSON_FILE_EXTENSION));
    }
}
