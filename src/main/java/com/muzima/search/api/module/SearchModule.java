/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.throwingproviders.ThrowingProviderBinder;
import com.muzima.search.api.internal.lucene.DefaultIndexer;
import com.muzima.search.api.internal.lucene.Indexer;
import com.muzima.search.api.internal.provider.AnalyzerProvider;
import com.muzima.search.api.internal.provider.DirectoryProvider;
import com.muzima.search.api.internal.provider.ReaderProvider;
import com.muzima.search.api.internal.provider.SearchProvider;
import com.muzima.search.api.internal.provider.SearcherProvider;
import com.muzima.search.api.internal.provider.WriterProvider;
import com.muzima.search.api.resource.Resource;
import com.muzima.search.api.service.RestAssuredService;
import com.muzima.search.api.service.impl.RestAssuredServiceImpl;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import java.util.HashMap;
import java.util.Map;

public class SearchModule extends AbstractModule {

    /**
     * Configures a {@link com.google.inject.Binder} via the exposed methods.
     */
    @Override
    protected void configure() {
        bind(Integer.class)
                .annotatedWith(Names.named("connection.timeout"))
                .toInstance(10000);

        bind(new TypeLiteral<Map<String, Resource>>() {
        })
                .toInstance(new HashMap<String, Resource>());

        bind(Indexer.class)
                .to(DefaultIndexer.class)
                .in(Singleton.class);

        bind(RestAssuredService.class)
                .to(RestAssuredServiceImpl.class)
                .in(Singleton.class);

        bind(Version.class).toInstance(Version.LUCENE_36);
        bind(Analyzer.class).toProvider(AnalyzerProvider.class);

        ThrowingProviderBinder.create(binder())
                .bind(SearchProvider.class, Directory.class)
                .to(DirectoryProvider.class)
                .in(Singleton.class);

        ThrowingProviderBinder.create(binder())
                .bind(SearchProvider.class, IndexReader.class)
                .to(ReaderProvider.class);

        ThrowingProviderBinder.create(binder())
                .bind(SearchProvider.class, IndexSearcher.class)
                .to(SearcherProvider.class);

        ThrowingProviderBinder.create(binder())
                .bind(SearchProvider.class, IndexWriter.class)
                .to(WriterProvider.class);
    }
}
