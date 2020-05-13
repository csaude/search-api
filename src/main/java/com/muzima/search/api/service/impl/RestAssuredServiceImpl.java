/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.muzima.search.api.filter.Filter;
import com.muzima.search.api.internal.lucene.Indexer;
import com.muzima.search.api.model.object.Searchable;
import com.muzima.search.api.model.resolver.Resolver;
import com.muzima.search.api.resource.Resource;
import com.muzima.search.api.service.RestAssuredService;
import com.muzima.search.api.util.CollectionUtil;
import com.muzima.search.api.util.FilenameUtil;
import com.muzima.search.api.util.StringUtil;
import org.apache.lucene.queryParser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class RestAssuredServiceImpl implements RestAssuredService {

    private static final String GET = "GET";

    private final Logger logger = LoggerFactory.getLogger(RestAssuredServiceImpl.class.getSimpleName());

    private Indexer indexer;

    @Inject(optional = true)
    @Named("connection.proxy")
    private Proxy proxy;

    @Inject
    @Named("connection.timeout")
    private int timeout;

    @Inject
    protected RestAssuredServiceImpl(final Indexer indexer) {
        this.indexer = indexer;
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#loadObjects(java.util.Map, com.muzima.search.api.resource.Resource)
     */
    @Override
    public List<Searchable> loadObjects(final Map<String, String> resourceParams, final Resource resource)
            throws IOException {
        Resolver resolver = resource.getResolver();
        String resourcePath = resolver.resolve(resourceParams);
        return downloadResource(resourcePath, resource);
    }

    /* Internal implementation of download process. The download will open connection to the REST resource and then
     * download the content into the specified path. On the subsequent call, the method will follow the NEXT field in
     * the REST resource if it is available :)
     * See:
     * - http://tools.ietf.org/html/rfc4287
     * - http://tools.ietf.org/html/rfc5005
     */
    private List<Searchable> downloadResource(final String resourcePath, final Resource resource)
            throws IOException {
        List<Searchable> searchableList = new ArrayList<Searchable>();
        String resourcePayload = readResource(resourcePath, resource);
        List<Object> pagingInfo = Collections.emptyList();
        do {
            if (!StringUtil.isEmpty(resourcePayload)) {
                StringReader stringReader = new StringReader(resourcePayload);
                searchableList.addAll(indexer.loadObjects(resource, stringReader));
                try {
                    pagingInfo = JsonPath.read(resourcePayload, "$['links'][?(@.rel == 'next')]");
                    if (!CollectionUtil.isEmpty(pagingInfo)) {
                        String nextPath = JsonPath.read(pagingInfo.get(0), "$['uri']");
                        resourcePayload = readResource(nextPath, resource);
                    }
                } catch (InvalidPathException e) {
                    logger.info("REST resource doesn't contains paging information. Exiting!");
                }
            }
        } while (!CollectionUtil.isEmpty(pagingInfo));
        return searchableList;
    }

    /*
     * Internal implementation of reading a REST resource and convert them into String object.
     */
    private String readResource(final String resourcePath, final Resource resource) throws IOException {
        URL url = new URL(resourcePath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (proxy != null) {
            connection = (HttpURLConnection) url.openConnection(proxy);
        }
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setRequestMethod(GET);
        connection.setConnectTimeout(timeout);
        Resolver resolver = resource.getResolver();
        connection = resolver.setCustomRequestProperties(connection);
        resolver.authenticate(connection);
        // we need to cache the input stream and then follow the next link.
        StringBuilder builder = new StringBuilder();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK
                || connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
            BufferedReader reader;
            if (connection.getHeaderField("Content-Encoding") != null
                    && connection.getHeaderField("Content-Encoding").equals("gzip")) {
                reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream())));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                builder.append(inputLine);
            }
            reader.close();
        }

        return builder.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#loadObjects(String, com.muzima.search.api.resource.Resource, java.io.File)
     */
    @Override
    public List<Searchable> loadObjects(final String searchString, final Resource resource, final File file)
            throws IOException {
        List<Searchable> searchableList = new ArrayList<Searchable>();
        if (!file.isDirectory() && FilenameUtil.contains(file.getName(), searchString)) {
            FileReader reader = null;
            try {
                reader = new FileReader(file);
                return indexer.loadObjects(resource, reader);
            } finally {
                if (reader != null)
                    reader.close();
            }
        } else {
            File[] files = file.listFiles();
            if (files != null) {
                for (File jsonFile : files)
                    searchableList.addAll(loadObjects(searchString, resource, jsonFile));
            }
        }
        return searchableList;
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#getObject(String, Class)
     */
    @Override
    public <T> T getObject(final String key, final Class<T> clazz) throws IOException {
        return indexer.getObject(key, clazz);
    }

    @Override
    public <T> Boolean objectExists(final String key, final Class<T> clazz) throws IOException {
        return indexer.objectExists(key, clazz);
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#getObject(String, com.muzima.search.api.resource.Resource)
     */
    @Override
    public Searchable getObject(final String key, final Resource resource) throws IOException {
        return indexer.getObject(key, resource);
    }

    @Override
    public Boolean objectExists(final String key, final Resource resource) throws IOException {
        return indexer.objectExists(key, resource);
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#getObjects(java.util.List, Class)
     */
    @Override
    public <T> List<T> getObjects(final List<Filter> filters, final Class<T> clazz) throws IOException {
        return indexer.getObjects(filters, clazz);
    }

    @Override
    public <T> List<T> getSortedObjects(final List<Filter> filters, final Class<T> clazz, final Resource resource) throws IOException {
        return indexer.getSortedObjects(filters, clazz, resource);
    }

    @Override
    public <T> List<T> getObjects(final List<Filter> filters, final Class<T> clazz,
                                  final Integer page, final Integer pageSize) throws IOException {
        return indexer.getObjects(filters, clazz, page, pageSize);
    }
    @Override
    public <T> List<T> getSortedObjects(final List<Filter> filters, final Class<T> clazz, final Resource resource,
                                  final Integer page, final Integer pageSize) throws IOException {
        return indexer.getSortedObjects(filters, clazz, resource, page, pageSize);
    }

    @Override
    public <T> Integer countObjects(final List<Filter> filters, final Class<T> clazz) throws IOException {
        return indexer.countObjects(filters, clazz);
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#getObjects(java.util.List, com.muzima.search.api.resource.Resource)
     */
    @Override
    public List<Searchable> getObjects(final List<Filter> filters, final Resource resource) throws IOException {
        return indexer.getObjects(filters, resource);
    }

    @Override
    public List<Searchable> getSortedObjects(final List<Filter> filters, final Resource resource) throws IOException {
        return indexer.getSortedObjects(filters, resource);
    }

    @Override
    public List<Searchable> getObjects(final List<Filter> filters, final Resource resource,
                                       final Integer page, final Integer pageSize) throws IOException {
        return indexer.getObjects(filters, resource, page, pageSize);
    }

    @Override
    public List<Searchable> getSortedObjects(final List<Filter> filters, final Resource resource,
                                       final Integer page, final Integer pageSize) throws IOException {
        return indexer.getSortedObjects(filters, resource, page, pageSize);
    }

    @Override
    public Integer countObjects(final List<Filter> filters, final Resource resource) throws IOException {
        return indexer.countObjects(filters, resource);
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#getObjects(String, Class)
     */
    @Override
    public <T> List<T> getObjects(final String searchString, final Class<T> clazz) throws ParseException, IOException {
        return indexer.getObjects(searchString, clazz);
    }

    @Override
    public <T> List<T> getObjects(final String searchString, final Class<T> clazz,
                                  final Integer page, final Integer pageSize) throws ParseException, IOException {
        return indexer.getObjects(searchString, clazz, page, pageSize);
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#getObjects(String, com.muzima.search.api.resource.Resource)
     */
    @Override
    public List<Searchable> getObjects(final String searchString, final Resource resource)
            throws ParseException, IOException {
        return indexer.getObjects(searchString, resource);
    }

    @Override
    public List<Searchable> getObjects(final String searchString, final Resource resource,
                                       final Integer page, final Integer pageSize) throws ParseException, IOException {
        return indexer.getObjects(searchString, resource, page, pageSize);
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#deleteObjects(java.util.List, com.muzima.search.api.resource.Resource)
     */
    @Override
    public void deleteObjects(final List<Searchable> objects, final Resource resource) throws IOException {
        indexer.deleteObjects(objects, resource);
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#createObjects(java.util.List, com.muzima.search.api.resource.Resource)
     */
    @Override
    public void createObjects(final List<Searchable> objects, final Resource resource) throws IOException {
        indexer.createObjects(objects, resource);
    }

    /**
     * {@inheritDoc}
     *
     * @see RestAssuredService#updateObjects(java.util.List, com.muzima.search.api.resource.Resource)
     */
    @Override
    public void updateObjects(final List<Searchable> objects, final Resource resource) throws IOException {
        indexer.updateObjects(objects, resource);
    }
}
