package com.muzima.search.api.internal.http;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * Implementation of class which can be used for connect to server with self signed SSL certificate.
 * <p/>
 * To create this class, you need to pass input stream and the password to open the certificate.
 * <p/>
 * See http://blog.crazybob.org/2010/02/android-trusting-ssl-certificates.html
 * See also http://hc.apache.org/httpclient-3.x/sslguide.html
 * See also http://hc.apache.org/httpcomponents-client-ga/examples.html
 * Another example of oAuth http://www.ibm.com/developerworks/library/wa-oauthsupport/
 */
public class CustomKeyStore {

    private final Logger logger = LoggerFactory.getLogger(CustomKeyStore.class.getSimpleName());

    private static final String SSL_CONTEXT_TLS = "TLS";

    @Inject(optional = true)
    @Named("key.store.type")
    private String keyStoreType;

    @Inject(optional = true)
    @Named("key.store.password")
    private String password;

    @Inject(optional = true)
    @Named("key.store.path")
    private String path;

    @Inject(optional = true)
    @Named("key.store.credential")
    private String credential;

    protected CustomKeyStore() {
    }

    public SSLContext createContext() {

        SSLContext context = null;
        try {
//            if (credential != null) {
//                BufferedReader bufferedReader = new BufferedReader(new FileReader(credential));
//                String credentials = bufferedReader.readLine();
//                bufferedReader.close();
//
//                password = credentials.substring(credentials.indexOf(":") + 1);
//                keyStoreType = credentials.substring(0, credentials.indexOf(":"));
//            }

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream inputStream = new BufferedInputStream(new FileInputStream(new File(path)));
            Certificate certificate = certificateFactory.generateCertificate(inputStream);

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("CA", certificate);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
//
//
//            // Create a KeyStore containing our trusted CAs
//            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
//            keyStore.load(inputStream, password.toCharArray());
//            // Create a TrustManager that trusts the CAs in our KeyStore
//            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
//            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
//            tmf.init(keyStore);
//            // Create an SSLContext that uses our TrustManager
//            context = SSLContext.getInstance(SSL_CONTEXT_TLS);
//            context.init(null, tmf.getTrustManagers(), null);
        } catch (Exception e) {
            logger.error("Unable generate ssl context object.", e);
        }
        return context;
    }
}
