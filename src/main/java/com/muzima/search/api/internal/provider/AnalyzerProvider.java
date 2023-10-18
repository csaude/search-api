/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.internal.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

public class AnalyzerProvider implements Provider<Analyzer> {

    private final Version version;

    // TODO: create a factory that takes a hint of what type of analyzer should be returned here
    // see the example for checked provider
    @Inject
    protected AnalyzerProvider(final Version version) {
        this.version = version;
    }

    @Override
    public Analyzer get() {
        return new StandardAnalyzer(version);
    }
}
