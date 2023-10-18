/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import java.io.File;

public class JUnitModule extends AbstractModule {

    public static final String LUCENE_DIRECTORY = File.separator + "lucene";

    /**
     * Configures a {@link com.google.inject.Binder} via the exposed methods.
     */
    @Override
    protected void configure() {
        String tmpDirectory = System.getProperty("java.io.tmpdir");
        bind(String.class)
                .annotatedWith(Names.named("configuration.lucene.directory"))
                .toInstance(tmpDirectory + LUCENE_DIRECTORY);
        bind(String.class)
                .annotatedWith(Names.named("configuration.lucene.field.key"))
                .toInstance("uuid");

        bind(Boolean.class)
                .annotatedWith(Names.named("connection.use.proxy"))
                .toInstance(Boolean.FALSE);
        bind(String.class)
                .annotatedWith(Names.named("connection.username"))
                .toInstance("admin");
        bind(String.class)
                .annotatedWith(Names.named("connection.password"))
                .toInstance("test");
        bind(String.class)
                .annotatedWith(Names.named("connection.server"))
                .toInstance("http://140.182.15.70:8081/");
        // ampath test server specific bindings

        bind(String.class)
                .annotatedWith(Names.named("configuration.lucene.encryption"))
                .toInstance("AES/ECB/PKCS5Padding");
        bind(String.class)
                .annotatedWith(Names.named("configuration.lucene.encryption.key"))
                .toInstance("this-is-an-example-of-a-secure-key");
        bind(Boolean.class)
                .annotatedWith(Names.named("configuration.lucene.usingEncryption"))
                .toInstance(true);
        bind(Boolean.class)
                .annotatedWith(Names.named("configuration.lucene.usingCompression"))
                .toInstance(false);

    }
}
