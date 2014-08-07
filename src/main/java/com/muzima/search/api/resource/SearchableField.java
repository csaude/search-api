/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.resource;

public class SearchableField {

    private final String name;

    private final String expression;

    private final Boolean unique;

    public SearchableField(final String name, final String expression, final Boolean unique) {
        this.name = name;
        this.expression = expression;
        this.unique = unique;
    }

    /**
     * Get the name of the searchable field.
     *
     * @return the searchable field's name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the expression to get the actual value of the searchable field.
     *
     * @return expression to get the value of the searchable value
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Flag to determine whether the value of this field would be unique for a resource.
     *
     * @return true if the value of the this field should be unique for this resource
     */
    public Boolean isUnique() {
        return unique;
    }
}
