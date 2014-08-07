/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.internal.lucene;

import com.muzima.search.api.filter.Filter;
import com.muzima.search.api.model.object.Searchable;
import com.muzima.search.api.resource.Resource;
import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public interface Indexer {

    List<Searchable> loadObjects(final Resource resource, final Reader reader) throws IOException;

    <T> T getObject(final String key, final Class<T> clazz) throws IOException;

    <T> Boolean objectExists(final String key, final Class<T> clazz) throws IOException;

    Searchable getObject(final String key, final Resource resource) throws IOException;

    Boolean objectExists(final String key, final Resource resource) throws IOException;

    <T> List<T> getObjects(final List<Filter> filters, final Class<T> clazz) throws IOException;

    <T> List<T> getObjects(final List<Filter> filters, final Class<T> clazz,
                           final Integer page, final Integer pageSize) throws IOException;

    <T> Integer countObjects(final List<Filter> filters, final Class<T> clazz) throws IOException;

    List<Searchable> getObjects(final List<Filter> filters, final Resource resource) throws IOException;

    List<Searchable> getObjects(final List<Filter> filters, final Resource resource,
                                final Integer page, final Integer pageSize) throws IOException;

    Integer countObjects(final List<Filter> filters, final Resource resource) throws IOException;

    <T> List<T> getObjects(final String searchString, final Class<T> clazz) throws ParseException, IOException;

    <T> List<T> getObjects(final String searchString, final Class<T> clazz,
                           final Integer page, final Integer pageSize) throws ParseException, IOException;

    List<Searchable> getObjects(final String searchString, final Resource resource) throws ParseException, IOException;

    List<Searchable> getObjects(final String searchString, final Resource resource,
                                final Integer page, final Integer pageSize) throws ParseException, IOException;

    void deleteObjects(final List<Searchable> objects, final Resource resource) throws IOException;

    void createObjects(final List<Searchable> objects, Resource resource) throws IOException;

    void updateObjects(final List<Searchable> objects, Resource resource) throws IOException;
}
