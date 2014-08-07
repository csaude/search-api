/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.sample.resolver;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.muzima.search.api.internal.http.CustomKeyStore;
import com.muzima.search.api.model.resolver.Resolver;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;

public abstract class AbstractResolver implements Resolver {

    protected final String WEB_SERVER = "http://localhost:8081/";

    protected final String WEB_CONTEXT = "openmrs-standalone/";

    @Inject
    @Named("connection.username")
    private String username;

    @Inject
    @Named("connection.password")
    private String password;

    @Inject
    @Named("connection.server")
    private String server;

    @Inject
    private CustomKeyStore customKeyStore;

    @Override
    public HttpURLConnection authenticate(final HttpURLConnection connection) {
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) connection;
            SSLContext sslContext = customKeyStore.createContext();
            if (sslContext != null) {
                httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return (hostname.equals(server));
                    }
                };
                httpsURLConnection.setHostnameVerifier(hostnameVerifier);
            }
        }
        return connection;
    }
}
