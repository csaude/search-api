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
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

import java.io.IOException;

public class ReaderProvider implements SearchProvider<IndexReader> {

    private final SearchProvider<Directory> directoryProvider;

    private SearchProvider<IndexWriter> writerProvider;

    @Inject
    protected ReaderProvider(final SearchProvider<IndexWriter> writerProvider,
                             final SearchProvider<Directory> directoryProvider) {
        this.writerProvider = writerProvider;
        this.directoryProvider = directoryProvider;
    }

    @Override
    public IndexReader get() throws IOException {
        Directory directory = directoryProvider.get();
        if (!IndexReader.indexExists(directory)) {
            IndexWriter writer = writerProvider.get();
            writer.close();
        }
        return IndexReader.open(directory);
    }
}
