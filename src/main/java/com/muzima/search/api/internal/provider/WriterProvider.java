/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.internal.provider;

import com.google.inject.Inject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import java.io.IOException;

public class WriterProvider implements SearchProvider<IndexWriter> {

    private final Version version;

    private final Analyzer analyzer;

    private final SearchProvider<Directory> directoryProvider;

    @Inject
    protected WriterProvider(final Version version, final Analyzer analyzer,
                             final SearchProvider<Directory> directoryProvider) {
        this.version = version;
        this.analyzer = analyzer;
        this.directoryProvider = directoryProvider;
    }

    @Override
    public IndexWriter get() throws IOException {
        Directory directory = directoryProvider.get();
        IndexWriterConfig config = new IndexWriterConfig(version, analyzer);
        return new IndexWriter(directory, config);
    }
}
