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
import net.minidev.json.JSONObject;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;

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
     * Get remote REST resource.
     * <p/>
     * This method will use the URI resolver to resolve the URI of the REST resources and then apply the
     * <code>searchString</code> to limit the data that needs to get converted.
     * <p/>
     * TODO: need to handle paging
     * - one of the solution probably merging this loadObject into:
     * - loadObject(final Resource resource, final String payload)
     * - this method then will read the response from the server
     * - delegate the paging handling to the subclass (if applicable).
     * - short term solution: increase the page size
     * <p/>
     *
     * @param resourceParams the parameters needed to construct the correct REST resource.
     * @param resource       the resource object which will describe how to index the json resource to lucene.
     */
    @Override
    public List<Searchable> loadObjects(final Map<String, String> resourceParams, final Resource resource)
            throws IOException {
        Resolver resolver = resource.getResolver();
        String resourcePath = resolver.resolve(resourceParams);
        return downloadResource(resourcePath, resource);
    }

    // Internal implementation of download process. The download will open connection to the REST resource and then
    // download the content into the specified path. On the subsequent call, the method will follow the NEXT field in
    // the REST resource if it is available :)
    // See:
    // * http://tools.ietf.org/html/rfc4287
    // * http://tools.ietf.org/html/rfc5005
    private List<Searchable> downloadResource(final String resourcePath, final Resource resource)
            throws IOException {
        List<Searchable> searchableList = new ArrayList<Searchable>();

        URL url = new URL(resourcePath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (proxy != null) {
            connection = (HttpURLConnection) url.openConnection(proxy);
        }
        connection.setRequestMethod(GET);
        connection.setConnectTimeout(timeout);
        Resolver resolver = resource.getResolver();
        connection = resolver.authenticate(connection);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String inputLine;
        // we need to cache the input stream and then follow the next link.
        StringBuilder builder = new StringBuilder();
        while ((inputLine = reader.readLine()) != null) {
            builder.append(inputLine);
        }
        reader.close();

        StringReader stringReader = new StringReader(builder.toString());
        searchableList.addAll(indexer.loadObjects(resource, stringReader));
        try {
            List<Object> pagingInfo = JsonPath.read(builder.toString(), "$['links'][?(@.rel == 'next')]");
            if (!CollectionUtil.isEmpty(pagingInfo)) {
                String nextPath = JsonPath.read(pagingInfo.get(0), "$['uri']");
                searchableList.addAll(downloadResource(nextPath, resource));
            }
        } catch (InvalidPathException e) {
            logger.error("REST resource doesn't contains paging information. Exiting!");
        }
        return searchableList;
    }

    /**
     * Convert JSON from local file to the correct object representation.
     * <p/>
     * This method will load locally saved json payload and then apply the <code>searchString</code> to limit the data
     * that needs to get converted.
     *
     * @param searchString the search string to filter the files to be loaded.
     * @param resource     the resource object which will describe how to index the json resource to lucene.
     * @param file         the file in the filesystem where the json resource is saved.
     * @see RestAssuredService#loadObjects(java.util.Map, com.muzima.search.api.resource.Resource)
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
     * Search for an object with matching <code>key</code> and <code>clazz</code> type from the local repository. This
     * method will only return single object or null if no object match the key.
     * <p/>
     * Internally, this method will go through every registered resources to find which resources can be used to convert
     * the json payload to the an instance of <code>clazz</code> object. The method then extract the unique field from
     * each resource and then perform the lucene query for that resource. If the resource doesn't specify unique
     * searchable field, all searchable fields for that resource will be used for searching.
     *
     * @param key   the key to distinguish the object
     * @param clazz the expected return type of the object
     * @return object with matching key and clazz or null
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
     * Search for an object with matching <code>key</code> and <code>clazz</code> type from the local repository. This
     * method will only return single object or null if no object match the key.
     * <p/>
     * Internally, this method will pull unique searchable fields from the resource and then create the query for that
     * fields and passing the key as the value. If the resource doesn't specify unique searchable field, all
     * searchable fields for that resource will be used for searching.
     *
     * @param key      the key to distinguish the object
     * @param resource the resource object which will describe how to index the json resource to lucene.
     * @return object with matching key and clazz or null
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
     * Search for objects with matching <code>filter</code> and <code>clazz</code> type from the local repository.
     * This method will return list of all matching object or empty list if no object match the search query.
     *
     * @param filters the search filter to limit the number of returned object
     * @param clazz   the expected return type of the object
     * @return list of all object with matching <code>query</code> and <code>clazz</code> or empty list
     * @should return all object matching the search query string and class
     * @should return empty list when no object match the search query and class
     */
    @Override
    public <T> List<T> getObjects(final List<Filter> filters, final Class<T> clazz) throws IOException {
        BooleanQuery booleanQuery = null;
        if (!CollectionUtil.isEmpty(filters)) {
            booleanQuery = new BooleanQuery();
            for (Filter filter : filters) {
                String sanitizedValue = StringUtil.sanitize(filter.getFieldValue());
                TermQuery termQuery = new TermQuery(new Term(filter.getFieldName(), sanitizedValue));
                booleanQuery.add(termQuery, BooleanClause.Occur.MUST);
            }
        }
        return indexer.getObjects(booleanQuery, clazz);
    }

    @Override
    public <T> Integer countObjects(final List<Filter> filters, final Class<T> clazz) throws IOException {
        BooleanQuery booleanQuery = null;
        if (!CollectionUtil.isEmpty(filters)) {
            booleanQuery = new BooleanQuery();
            for (Filter filter : filters) {
                String sanitizedValue = StringUtil.sanitize(filter.getFieldValue());
                TermQuery termQuery = new TermQuery(new Term(filter.getFieldName(), sanitizedValue));
                booleanQuery.add(termQuery, BooleanClause.Occur.MUST);
            }
        }
        return indexer.countObjects(booleanQuery, clazz);
    }

    /**
     * Search for objects with matching <code>filter</code> and <code>resource</code> type from the local repository.
     * This method will return list of all matching object or empty list if no object match the search query.
     *
     * @param filters  the search filter to limit the number of returned object
     * @param resource the resource descriptor used to register the object
     * @return list of all object with matching <code>query</code> and <code>resource</code> or empty list
     * @should return all object matching the search query and resource
     * @should return empty list when no object match the search query and resource
     */
    @Override
    public List<Searchable> getObjects(final List<Filter> filters, final Resource resource) throws IOException {
        BooleanQuery booleanQuery = null;
        if (!CollectionUtil.isEmpty(filters)) {
            booleanQuery = new BooleanQuery();
            for (Filter filter : filters) {
                String sanitizedValue = StringUtil.sanitize(filter.getFieldValue());
                TermQuery termQuery = new TermQuery(new Term(filter.getFieldName(), sanitizedValue));
                booleanQuery.add(termQuery, BooleanClause.Occur.MUST);
            }
        }
        return indexer.getObjects(booleanQuery, resource);
    }

    @Override
    public Integer countObjects(final List<Filter> filters, final Resource resource) throws IOException {
        BooleanQuery booleanQuery = null;
        if (!CollectionUtil.isEmpty(filters)) {
            booleanQuery = new BooleanQuery();
            for (Filter filter : filters) {
                String sanitizedValue = StringUtil.sanitize(filter.getFieldValue());
                TermQuery termQuery = new TermQuery(new Term(filter.getFieldName(), sanitizedValue));
                booleanQuery.add(termQuery, BooleanClause.Occur.MUST);
            }
        }
        return indexer.countObjects(booleanQuery, resource);
    }

    /**
     * Search for objects with matching <code>searchString</code> and <code>clazz</code> type from the local repository.
     * This method will return list of all matching object or empty list if no object match the search string.
     *
     * @param clazz        the expected return type of the object
     * @param searchString the search string to limit the number of returned object
     * @return list of all object with matching <code>searchString</code> and <code>clazz</code> or empty list
     */
    @Override
    public <T> List<T> getObjects(final String searchString, final Class<T> clazz) throws ParseException, IOException {
        return indexer.getObjects(searchString, clazz);
    }

    /**
     * Search for objects with matching <code>searchString</code> and <code>resource</code> type from the local
     * repository. This method will return list of all matching object or empty list if no object match the search
     * string.
     *
     * @param searchString the search string to limit the number of returned object
     * @param resource     the resource descriptor used to register the object
     * @return list of all object with matching <code>searchString</code> and <code>resource</code> or empty list
     */
    @Override
    public List<Searchable> getObjects(final String searchString, final Resource resource)
            throws ParseException, IOException {
        return indexer.getObjects(searchString, resource);
    }

    /**
     * Remove objects based on the resource from the local repository. The method will determine if there's unique
     * <code>object</code> in the local repository and then remove it. This method will return null if there's no
     * object in the local repository match the object passed to this method.
     * <p/>
     * Internally, this method will serialize the object to json and then using the resource object, the method will
     * recreate unique key query to find the entry in the local lucene repository. If no unique searchable field is
     * specified in the resource configuration, this method will use all searchable index to find the entry.
     *
     * @param objects  the objects to be removed if the objects exist.
     * @param resource the resource object which will describe how to index the json resource to lucene.
     */
    @Override
    public void deleteObjects(final List<Searchable> objects, final Resource resource) throws IOException {
        indexer.deleteObjects(objects, resource);
    }

    /**
     * Create instances of objects in the local repository.
     * <p/>
     * Internally, this method will serialize the object and using the resource configuration to create an entry in
     * the lucene local repository.
     * <p/>
     * Internally, this method will also add the following field:
     * <pre>
     * _class : the expected representation of the json when serialized
     * _resource : the resource configuration used to convert the json to lucene
     * </pre>
     *
     * @param objects  the objects to be created
     * @param resource the resource object which will describe how to index the json resource to lucene.
     */
    @Override
    public void createObjects(final List<Searchable> objects, final Resource resource) throws IOException {
        indexer.createObjects(objects, resource);
    }

    /**
     * Update instances of objects in the local repository.
     * <p/>
     * Internally, this method will perform invalidation of the object and then recreate the object in the local lucene
     * repository. If the changes are performed on the unique searchable field, this method will end up creating a new
     * entry in the lucene local repository.
     *
     * @param objects  the objects to be updated
     * @param resource the resource object which will describe how to index the json resource to lucene.
     */
    @Override
    public void updateObjects(final List<Searchable> objects, final Resource resource) throws IOException {
        indexer.updateObjects(objects, resource);
    }
}
