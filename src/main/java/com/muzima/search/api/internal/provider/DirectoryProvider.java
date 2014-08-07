/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.internal.provider;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.transform.CompressedIndexDirectory;
import org.apache.lucene.store.transform.TransformedDirectory;
import org.apache.lucene.store.transform.algorithm.ReadPipeTransformer;
import org.apache.lucene.store.transform.algorithm.StorePipeTransformer;
import org.apache.lucene.store.transform.algorithm.compress.DeflateDataTransformer;
import org.apache.lucene.store.transform.algorithm.compress.InflateDataTransformer;
import org.apache.lucene.store.transform.algorithm.security.DataDecryptor;
import org.apache.lucene.store.transform.algorithm.security.DataEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.zip.Deflater;

public class DirectoryProvider implements SearchProvider<Directory> {

    private final String directory;
    private final Logger logger = LoggerFactory.getLogger(DirectoryProvider.class.getSimpleName());

    @Inject(optional = true)
    @Named("configuration.lucene.usingCompression")
    Boolean usingCompression;

    @Inject(optional = true)
    @Named("configuration.lucene.usingEncryption")
    Boolean usingEncryption;

    @Inject(optional = true)
    @Named("configuration.lucene.encryption.key")
    String password;

    @Inject(optional = true)
    @Named("configuration.lucene.encryption")
    String encryption;

    // TODO: create a factory to customize the type of directory returned by this provider
    @Inject
    protected DirectoryProvider(final @Named("configuration.lucene.directory") String directory) {
        this.directory = directory;
    }

    @Override
    public Directory get() throws IOException {
        Directory directory = FSDirectory.open(new File(this.directory));

        if (usingEncryption) {
            byte[] salt = new byte[16];

            if (logger.isDebugEnabled()) {
                logger.debug("Using password with inject - {}", password);
                logger.debug("Using encryption with inject - {}", encryption);
            }

            DataEncryptor enc;
            try {
                enc = new DataEncryptor(encryption, password, salt, 128, false);
            } catch (GeneralSecurityException e) {
                throw new IOException("Unable to create data encryptor.", e);
            }
            DataDecryptor dec = new DataDecryptor(password, salt, false);

            if (usingCompression) {
                StorePipeTransformer st = new StorePipeTransformer(new DeflateDataTransformer(Deflater.BEST_COMPRESSION, 1), enc);
                ReadPipeTransformer rt = new ReadPipeTransformer(dec, new InflateDataTransformer());

                if (logger.isDebugEnabled()) {
                    logger.debug("Using encryption and compression!");
                }

                // encrypted and compressed
                return new TransformedDirectory(directory, st, rt);
            }

            // encrypted but not compressed
            if (logger.isDebugEnabled()) {
                logger.debug("Using encryption!");
            }
            return new TransformedDirectory(directory, enc, dec);
        } else {
            if (usingCompression) {
                // not encrypted but compressed
                if (logger.isDebugEnabled()) {
                    logger.debug("Using compression!");
                }
                return new CompressedIndexDirectory(directory);
            }
        }

        // not encrypted not compressed
        if (logger.isDebugEnabled()) {
            logger.debug("Using standard directory!");
        }
        return directory;
    }
}
