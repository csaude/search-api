/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.internal.provider;

import com.google.inject.Inject;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;

public class SearcherProvider implements SearchProvider<IndexSearcher> {

    private SearchProvider<IndexReader> readerProvider;

    @Inject
    protected SearcherProvider(final SearchProvider<IndexReader> readerProvider) {
        this.readerProvider = readerProvider;
    }

    @Override
    public IndexSearcher get() throws IOException {
        IndexReader indexReader = readerProvider.get();
        return new IndexSearcher(indexReader);
    }
}
