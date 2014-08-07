/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.util;

import java.util.Collection;

public class CollectionUtil {

    /**
     * Method to check if the collection is empty or not.
     *
     * @param collection the collection
     * @return true if the collection is null or the collection is empty.
     */
    public static Boolean isEmpty(final Collection collection) {
        return collection == null || collection.isEmpty();
    }
}
