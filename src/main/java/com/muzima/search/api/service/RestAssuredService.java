/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.service;

import com.muzima.search.api.filter.Filter;
import com.muzima.search.api.model.object.Searchable;
import com.muzima.search.api.resource.Resource;
import org.apache.lucene.queryParser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface RestAssuredService {

    /**
     * Convert downloaded remote REST resource to the correct object representation.
     * <p/>
     * This method will use the URI resolver to resolve the URI of the REST resources and then apply the
     * <code>searchString</code> to limit the data that needs to get converted.
     * <p/>
     *
     * @param resourceParams the parameters needed to construct the correct REST resource.
     * @param resource       the resource object which will describe how to index the json resource to lucene.
     * @should load objects based on the resource description
     */
    List<Searchable> loadObjects(final Map<String, String> resourceParams, final Resource resource) throws IOException;

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
    List<Searchable> loadObjects(final String searchString, final Resource resource, final File file)
            throws IOException;

    /**
     * Search for an object with matching <code>key</code> and <code>clazz</code> type from the local repository. This
     * method will only return single object or null if no object match the key.
     * <p/>
     * Internally, this method will go through every registered resources to find which resources can be used to convert
     * the json payload to the an instance of <code>clazz</code> object. The method then extract the unique field from
     * each resource and then perform the lucene query for that resource. If the resource doesn't specify unique
     * searchable field, all searchable fields for that resource will be used for searching.
     *
     * @param key   the key to distinguish the object.
     * @param clazz the expected return type of the object.
     * @return object with matching key and clazz or null.
     * @should return object with matching key and type.
     * @should return null when no object match the key and type.
     * @should throw IOException if the key and class unable to return unique object.
     */
    <T> T getObject(final String key, final Class<T> clazz) throws IOException;

    <T> Boolean objectExists(final String key, final Class<T> clazz) throws IOException;

    /**
     * Search for an object with matching <code>key</code> and <code>clazz</code> type from the local repository. This
     * method will only return single object or null if no object match the key.
     * <p/>
     * Internally, this method will pull unique searchable fields from the resource and then create the query for that
     * fields and passing the key as the value. If the resource doesn't specify unique searchable field, all
     * searchable fields for that resource will be used for searching.
     *
     * @param key      the key to distinguish the object.
     * @param resource the resource object which will describe how to index the json resource to lucene.
     * @return object with matching key and clazz or null.
     * @should return object with matching key.
     * @should return null when no object match the key.
     * @should throw IOException if the key and resource unable to return unique object.
     */
    Searchable getObject(final String key, final Resource resource) throws IOException;

    Boolean objectExists(final String key, final Resource resource) throws IOException;

    /**
     * Search for objects with matching <code>filter</code> and <code>clazz</code> type from the local repository.
     * This method will return list of all matching object or empty list if no object match the search query.
     *
     * @param filters the search filter to limit the number of returned object.
     * @param clazz   the expected return type of the object.
     * @return list of all object with matching <code>query</code> and <code>clazz</code> or empty list.
     * @should return all object matching the search query string and class.
     * @should return empty list when no object match the search query and class.
     */
    <T> List<T> getObjects(final List<Filter> filters, final Class<T> clazz) throws IOException;

    /**
     * Search for objects with matching <code>filter</code> and <code>clazz</code> type from the local repository.
     * This method will return list of objects matching the search filter and page number
     * or empty list if no object match the search query.
     *
     * @param filters the search filter to limit the number of returned object.
     * @param clazz   the expected return type of the object.
     * @return list of all object with matching <code>query</code> and <code>clazz</code> or empty list.
     * @should return all object matching the search query string and class.
     * @should return empty list when no object match the search query and class.
     * @should return empty list when the page index exceeds available pages
     */
    <T> List<T> getObjects(final List<Filter> filters, final Class<T> clazz,
                           final Integer page, final Integer pageSize) throws IOException;<T> List<T> getObjects(final List<Filter> filters, final Class<T> clazz,
                           final Integer page, final Integer pageSize, final Resource resource) throws IOException;

    <T> Integer countObjects(final List<Filter> filters, final Class<T> clazz) throws IOException;

    /**
     * Search for objects with matching <code>filter</code> and <code>resource</code> type from the local repository.
     * This method will return list of all matching object or empty list if no object match the search query.
     *
     * @param filters  the search filter to limit the number of returned object.
     * @param resource the resource descriptor used to register the object.
     * @return list of all object with matching <code>query</code> and <code>resource</code> or empty list.
     * @should return all object matching the search query and resource.
     * @should return empty list when no object match the search query and resource.
     */
    List<Searchable> getObjects(final List<Filter> filters, final Resource resource) throws IOException;

    List<Searchable> getObjects(final List<Filter> filters, final Resource resource,
                                final Integer page, final Integer pageSize) throws IOException;

    Integer countObjects(final List<Filter> filters, final Resource resource) throws IOException;

    /**
     * Search for objects with matching <code>searchString</code> and <code>clazz</code> type from the local repository.
     * This method will return list of all matching object or empty list if no object match the search string.
     *
     * @param clazz        the expected return type of the object.
     * @param searchString the search string to limit the number of returned object.
     * @return list of all object with matching <code>searchString</code> and <code>clazz</code> or empty list.
     * @should return all object matching the search string and class.
     * @should return empty list when no object match the search string and class.
     */
    <T> List<T> getObjects(final String searchString, final Class<T> clazz) throws ParseException, IOException;

    <T> List<T> getObjects(final String searchString, final Class<T> clazz,
                           final Integer page, final Integer pageSize) throws ParseException, IOException;

    /**
     * Search for objects with matching <code>searchString</code> and <code>resource</code> type from the local
     * repository. This method will return list of all matching object or empty list if no object match the search
     * string.
     *
     * @param searchString the search string to limit the number of returned object.
     * @param resource     the resource descriptor used to register the object.
     * @return list of all object with matching <code>searchString</code> and <code>resource</code> or empty list.
     * @should return all object matching the search string and resource.
     * @should return empty list when no object match the search string and resource.
     */
    List<Searchable> getObjects(final String searchString, final Resource resource) throws ParseException, IOException;

    List<Searchable> getObjects(final String searchString, final Resource resource,
                                final Integer page, final Integer pageSize) throws ParseException, IOException;

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
     * @should remove an object from the internal index system.
     */
    void deleteObjects(final List<Searchable> objects, final Resource resource) throws IOException;

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
    void createObjects(final List<Searchable> objects, Resource resource) throws IOException;

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
    void updateObjects(final List<Searchable> objects, Resource resource) throws IOException;
}
