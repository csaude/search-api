/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.internal.provider;

import com.google.inject.throwingproviders.CheckedProvider;

import java.io.IOException;

public interface SearchProvider<T> extends CheckedProvider<T> {

    T get() throws IOException;
}
