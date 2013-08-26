/**
 * Copyright 2012 Muzima Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.muzima.search.api.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Proxy;

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
