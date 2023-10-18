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
public class Filter {

    private String fieldName;

    private String fieldValue;

    /**
     * Get the filtered field's name.
     *
     * @return the filtered field's name.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Set  the filtered field's name.
     *
     * @param fieldName the filtered field's name.
     */
    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Get the filtered field's value.
     *
     * @return the filtered field's value.
     */
    public String getFieldValue() {
        return fieldValue;
    }

    /**
     * Set the filtered field's value.
     *
     * @param fieldValue the filtered field's value.
     */
    public void setFieldValue(final String fieldValue) {
        this.fieldValue = fieldValue;
    }
}
