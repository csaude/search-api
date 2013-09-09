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
