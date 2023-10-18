/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.filter;

/**
 * TODO: Write brief description about the class here.
 */
public class FilterFactory {

    public static Filter createFilter(final String fieldName, final String fieldValue) {
        Filter filter = new Filter();
        filter.setFieldName(fieldName);
        filter.setFieldValue(fieldValue);
        return filter;
    }
}
