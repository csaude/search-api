/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * TODO: Write brief description about the class here.
 */
public class ProxyModule extends AbstractModule {

    /**
     * Configures a {@link com.google.inject.Binder} via the exposed methods.
     */
    @Override
    protected void configure() {
        // proxy bindings to allow us to connect to the test server
        bind(Boolean.class)
                .annotatedWith(Names.named("connection.use.proxy"))
                .toInstance(Boolean.TRUE);
        bind(Proxy.Type.class)
                .annotatedWith(Names.named("connection.proxy.type"))
                .toInstance(Proxy.Type.SOCKS);
        bind(String.class)
                .annotatedWith(Names.named("connection.proxy.host"))
                .toInstance("localhost");
        bind(Integer.class)
                .annotatedWith(Names.named("connection.proxy.port"))
                .toInstance(1080);
    }

    @Provides
    @Named("connection.proxy")
    Proxy createProxy(@Named("connection.use.proxy") final Boolean useProxy,
                      @Named("connection.proxy.type") final Proxy.Type proxyType,
                      @Named("connection.proxy.host") final String proxyHost,
                      @Named("connection.proxy.port") final Integer proxyPort) {
        Proxy proxy = Proxy.NO_PROXY;
        if (useProxy) {
            proxy = new Proxy(proxyType, new InetSocketAddress(proxyHost, proxyPort));
        }
        return proxy;
    }
}
