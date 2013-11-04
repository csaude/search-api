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

package com.muzima.search.api.internal.lucene;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jayway.jsonpath.JsonPath;
import com.muzima.search.api.filter.Filter;
import com.muzima.search.api.internal.provider.SearcherProvider;
import com.muzima.search.api.internal.provider.WriterProvider;
import com.muzima.search.api.model.object.Searchable;
import com.muzima.search.api.resource.Resource;
import com.muzima.search.api.resource.SearchableField;
import com.muzima.search.api.util.CollectionUtil;
import com.muzima.search.api.util.StreamUtil;
import com.muzima.search.api.util.StringUtil;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultIndexer implements Indexer {

    private final Logger logger = LoggerFactory.getLogger(DefaultIndexer.class.getSimpleName());

    private String defaultField;

    private IndexSearcher indexSearcher;

    @Inject
    private WriterProvider writerProvider;

    @Inject
    private SearcherProvider searcherProvider;

    @Inject
    private Map<String, Resource> resourceRegistry;

    private final QueryParser parser;

    private static final String DEFAULT_FIELD_JSON = "_json";

    private static final String DEFAULT_FIELD_CLASS = "_class";

    private static final String DEFAULT_FIELD_RESOURCE = "_resource";

    private static final Integer DEFAULT_MAX_DOCUMENTS = 1;

    @Inject
    protected DefaultIndexer(final @Named("configuration.lucene.field.key") String defaultField,
                             final Version version, final Analyzer analyzer) {
        this.defaultField = defaultField;
        this.parser = new QueryParser(version, defaultField, analyzer);
        this.parser.setAllowLeadingWildcard(true);
    }

    /**
     * Get the singleton object of the resource registry.
     *
     * @return the resource registry object.
     */
    public Map<String, Resource> getResourceRegistry() {
        return resourceRegistry;
    }

    /*
     * Private Getter and Setter section **
     */

    private IndexWriter createIndexWriter() throws IOException {
        // might need to make this a synchronized call.
        // or allowing the caller to get new index searcher every time.
        return writerProvider.get();
    }

    private IndexSearcher createIndexSearcher() throws IOException {
        // might need to make this a synchronized call.
        // or allowing the caller to get new index searcher every time.
        try {
            if (indexSearcher == null) {
                indexSearcher = searcherProvider.get();
            }
        } catch (IOException e) {
            // silently ignoring this exception.
        }
        return indexSearcher;
    }

    private void commit(final IndexWriter writer, final IndexSearcher searcher) throws IOException {
        if (writer != null) {
            writer.commit();
            writer.close();
            searcher.close();
        }
        indexSearcher = null;
    }

    /*
     * Low level lucene operation **
     */

    /**
     * Create a single term lucene query. The value for the query will be surrounded with single quote.
     *
     * @param field the field on which the query should be performed.
     * @param value the value for the field
     * @return the valid lucene query for single term.
     */
    private TermQuery createQuery(final String field, final String value) {
        return new TermQuery(new Term(field, StringUtil.lowerCase(value)));
    }

    /**
     * Create lucene query string based on the searchable field name and value. The value for the searchable field
     * will be retrieved from the <code>object</code>. This method will try to create a unique query in the case
     * where a searchable field is marked as unique. Otherwise the method will create a query string using all
     * available searchable fields.
     *
     * @param object the json object from which the value for each field can be retrieved from.
     * @param fields the searchable fields definition
     * @return query string which could be either a unique or full searchable field based query.
     */
    private BooleanQuery createObjectQuery(final Object object, final List<SearchableField> fields) {
        boolean uniqueExists = false;
        BooleanQuery fullBooleanQuery = new BooleanQuery();
        BooleanQuery uniqueBooleanQuery = new BooleanQuery();
        for (SearchableField searchableField : fields) {
            Object valueObject = JsonPath.read(object, searchableField.getExpression());
            if (valueObject != null) {
                String value = valueObject.toString();
                TermQuery query = createQuery(searchableField.getName(), value);

                fullBooleanQuery.add(query, BooleanClause.Occur.MUST);

                if (searchableField.isUnique()) {
                    uniqueBooleanQuery.add(query, BooleanClause.Occur.MUST);
                    uniqueExists = true;
                }
            }
        }

        if (uniqueExists) {
            return uniqueBooleanQuery;
        } else {
            return fullBooleanQuery;
        }
    }

    /**
     * Create query fragment for a certain class. Calling this method will ensure the documents returned will of the
     * <code>clazz</code> type meaning the documents can be converted into object of type <code>clazz</code>. Converting
     * the documents need to be performed by getting the correct resource object from the document and then calling the
     * serialize method or getting the algorithm class and perform the serialization process from the algorithm object.
     * <p/>
     * Example use case: please retrieve all patients data. This should be performed by querying all object of certain
     * class type  because the caller is interested only in the type of object, irregardless of the resources from
     * which the objects are coming from.
     *
     * @param clazz the clazz for which the query is based on
     * @return the base query for a resource
     */
    private TermQuery createClassQuery(final Class clazz) {
        return new TermQuery(new Term(DEFAULT_FIELD_CLASS, clazz.getName()));
    }

    /**
     * Create query for a certain resource object. Calling this method will ensure the documents returned will be
     * documents that was indexed using the resource object.
     *
     * @param resource the resource for which the query is based on
     * @return the base query for a resource
     */
    private TermQuery createResourceQuery(final Resource resource) {
        return new TermQuery(new Term(DEFAULT_FIELD_RESOURCE, resource.getName()));
    }

    /**
     * Search the local lucene repository for documents with similar information with information inside the
     * <code>query</code>. Search can return multiple documents with similar information or empty list when no
     * document have similar information with the <code>query</code>.
     *
     * @param query the lucene query.
     * @return objects with similar information with the query.
     * @throws IOException when the search encounter error.
     */
    private List<Document> findDocuments(final Query query) throws IOException {
        List<Document> documents = new ArrayList<Document>();
        IndexSearcher searcher = createIndexSearcher();
        // Iterating over all hits:
        // * http://stackoverflow.com/questions/3300265/lucene-3-iterating-over-all-hits
        if (searcher != null) {
            TopDocs countDocs = searcher.search(query, DEFAULT_MAX_DOCUMENTS);
            TopDocs docs = searcher.search(query, countDocs.totalHits > 0 ? countDocs.totalHits : DEFAULT_MAX_DOCUMENTS);
            ScoreDoc[] hits = docs.scoreDocs;
            for (ScoreDoc hit : hits) {
                documents.add(searcher.doc(hit.doc));
            }
        }
        return documents;
    }

    /**
     * Search the local lucene repository for documents with similar information with information inside the
     * <code>query</code>. Search can return multiple documents with similar information or empty list when no
     * document have similar information with the <code>query</code>.
     *
     * @param query    the lucene query.
     * @param page     the page number.
     * @param pageSize the size of the page.
     * @return objects with similar information with the query.
     * @throws IOException when the search encounter error.
     */
    private List<Document> findDocuments(final Query query, final Integer page, Integer pageSize) throws IOException {
        List<Document> documents = new ArrayList<Document>();
        IndexSearcher searcher = createIndexSearcher();
        // Iterating over all hits:
        // * http://stackoverflow.com/questions/3300265/lucene-3-iterating-over-all-hits
        if (searcher != null) {
            TopDocs countDocs = searcher.search(query, DEFAULT_MAX_DOCUMENTS);
            TopDocs docs = searcher.search(query, countDocs.totalHits > 0 ? countDocs.totalHits : DEFAULT_MAX_DOCUMENTS);
            ScoreDoc[] hits = docs.scoreDocs;
            for (int i = (pageSize * (page - 1)); i < pageSize * page; i++) {
                ScoreDoc hit = hits[i];
                documents.add(searcher.doc(hit.doc));
            }
        }
        return documents;
    }

    /**
     * Write json representation of a single object as a single document entry inside Lucene index.
     *
     * @param jsonObject the json object to be written to the index
     * @param resource   the configuration to transform json to lucene document
     * @param writer     the lucene index writer
     * @throws java.io.IOException when writing document failed
     */
    private void writeObject(final Object jsonObject, final Resource resource, final IndexWriter writer)
            throws IOException {

        Document document = new Document();
        document.add(new Field(DEFAULT_FIELD_JSON, jsonObject.toString(), Field.Store.YES, Field.Index.NO));
        document.add(new Field(DEFAULT_FIELD_CLASS, resource.getSearchable().getName(), Field.Store.NO,
                Field.Index.NOT_ANALYZED));
        document.add(new Field(DEFAULT_FIELD_RESOURCE, resource.getName(), Field.Store.YES,
                Field.Index.NOT_ANALYZED));

        /*
         * TODO: a better way to write this to lucene probably using the algorithm.
         * Approach:
         * - Get the algorithm object.
         * - Pass the jsonObject to the algorithm object to create the actual object.
         * - Iterate over each property of the class (using bean utils?) and add each of them to the document.
         */
        for (SearchableField searchableField : resource.getSearchableFields()) {
            Object valueObject = JsonPath.read(jsonObject, searchableField.getExpression());
            if (valueObject instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) valueObject;
                for (Object arrayElement : jsonArray) {
                    String value = StringUtil.lowerCase(String.valueOf(arrayElement));
                    if (logger.isDebugEnabled()) {
                        logger.debug("Adding field name '{}' from path '{}' with value: {}",
                                searchableField.getName(), searchableField.getExpression(), value);
                    }
                    document.add(new Field(searchableField.getName(), value, Field.Store.NO, Field.Index.NOT_ANALYZED));
                }
            } else {
                String value = StringUtil.lowerCase(String.valueOf(valueObject));
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding field name '{}' from path '{}' with value: {}",
                            searchableField.getName(), searchableField.getExpression(), value);
                }
                document.add(new Field(searchableField.getName(), value, Field.Store.NO, Field.Index.NOT_ANALYZED));
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Writing document: {}", document);
        }

        writer.addDocument(document);
    }

    /**
     * Delete an entry from the lucene index. The method will search for a single entry in the index (throwing
     * IOException when more than one index match the object).
     *
     * @param jsonObject  the json object to be deleted.
     * @param resource    the resource definition used to register the json to lucene index.
     * @param indexWriter the index writer used to delete the index.
     * @throws IOException when other error happens during the deletion process.
     */
    private void deleteObject(final Object jsonObject, final Resource resource, final IndexWriter indexWriter)
            throws IOException {
        BooleanQuery query = createObjectQuery(jsonObject, resource.getSearchableFields());
        query.add(createResourceQuery(resource), BooleanClause.Occur.MUST);

        if (logger.isDebugEnabled()) {
            logger.debug("Query deleteObject(): {}", query.toString());
        }

        IndexSearcher searcher = createIndexSearcher();
        TopDocs docs = searcher.search(query, DEFAULT_MAX_DOCUMENTS);
        // only delete object if we can uniquely identify the object
//        if (docs.totalHits == 1) {
            indexWriter.deleteDocuments(query);
//        } else if (docs.totalHits > 1) {
//            throw new IOException("Unable to uniquely identify an object using the json object in the repository.");
//        }
    }

    /**
     * Update an object inside the lucene index with a new data. Updating process practically means deleting old object
     * and then adding the new object.
     *
     * @param jsonObject  the json object to be updated.
     * @param resource    the resource definition used to register the json to lucene index.
     * @param indexWriter the index writer used to delete the index.
     * @throws IOException when other error happens during the deletion process.
     */
    private void updateObject(final Object jsonObject, final Resource resource, final IndexWriter indexWriter)
            throws IOException {
        // search for the same object, if they exists, delete them :)
        deleteObject(jsonObject, resource, indexWriter);
        // write the new object
        writeObject(jsonObject, resource, indexWriter);
    }

    @Override
    public List<Searchable> loadObjects(final Resource resource, final Reader reader)
            throws IOException {
        List<Searchable> searchableList = new ArrayList<Searchable>();
        String json = StreamUtil.readAsString(reader);
        Object jsonObject = JsonPath.read(json, resource.getRootNode());
        if (jsonObject instanceof JSONArray) {
            JSONArray array = (JSONArray) jsonObject;
            for (Object element : array) {
                searchableList.add(resource.deserialize(element.toString()));
            }
        } else if (jsonObject instanceof JSONObject) {
            searchableList.add(resource.deserialize(jsonObject.toString()));
        }
        return searchableList;
    }

    @Override
    public <T> T getObject(final String key, final Class<T> clazz) throws IOException {
        T object = null;

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createClassQuery(clazz), BooleanClause.Occur.MUST);
        if (!StringUtil.isEmpty(key)) {
            booleanQuery.add(createQuery(defaultField, key), BooleanClause.Occur.MUST);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObject(String, Class): {}", booleanQuery.toString());
        }

        List<Document> documents = findDocuments(booleanQuery);

        if (!CollectionUtil.isEmpty(documents) && documents.size() > 1) {
            throw new IOException("Unable to uniquely identify an object using key: '" + key + "' in the repository.");
        }

        for (Document document : documents) {
            String resourceName = document.get(DEFAULT_FIELD_RESOURCE);
            Resource resource = getResourceRegistry().get(resourceName);
            String json = document.get(DEFAULT_FIELD_JSON);
            object = clazz.cast(resource.deserialize(json));
        }

        return object;
    }

    @Override
    public <T> Boolean objectExists(final String key, final Class<T> clazz) throws IOException {
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createClassQuery(clazz), BooleanClause.Occur.MUST);
        if (!StringUtil.isEmpty(key)) {
            booleanQuery.add(createQuery(defaultField, key), BooleanClause.Occur.MUST);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Query objectExists(String, Class): {}", booleanQuery.toString());
        }

        IndexSearcher searcher = createIndexSearcher();
        TopDocs docs = searcher.search(booleanQuery, DEFAULT_MAX_DOCUMENTS);
        if (docs.totalHits > 1) {
            throw new IOException("Unable to uniquely identify an object using key: '" + key + "' in the repository.");
        }
        return (docs.totalHits == 1);
    }

    @Override
    public Searchable getObject(final String key, final Resource resource) throws IOException {
        Searchable object = null;

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createResourceQuery(resource), BooleanClause.Occur.MUST);
        if (!StringUtil.isEmpty(key)) {
            booleanQuery.add(createQuery(defaultField, key), BooleanClause.Occur.MUST);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObject(String,  Resource): {}", booleanQuery.toString());
        }

        List<Document> documents = findDocuments(booleanQuery);

        if (!CollectionUtil.isEmpty(documents) && documents.size() > 1) {
            throw new IOException("Unable to uniquely identify an object using key: '" + key + "' in the repository.");
        }

        for (Document document : documents) {
            String json = document.get(DEFAULT_FIELD_JSON);
            object = resource.deserialize(json);
        }

        return object;
    }

    @Override
    public Boolean objectExists(final String key, final Resource resource) throws IOException {
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createResourceQuery(resource), BooleanClause.Occur.MUST);
        if (!StringUtil.isEmpty(key)) {
            booleanQuery.add(createQuery(defaultField, key), BooleanClause.Occur.MUST);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Query objectExists(String,  Resource): {}", booleanQuery.toString());
        }

        IndexSearcher searcher = createIndexSearcher();
        TopDocs docs = searcher.search(booleanQuery, DEFAULT_MAX_DOCUMENTS);
        if (docs.totalHits > 1) {
            throw new IOException("Unable to uniquely identify an object using key: '" + key + "' in the repository.");
        }
        return (docs.totalHits == 1);
    }

    private void addFilters(final List<Filter> filters, final BooleanQuery booleanQuery) {
        for (Filter filter : filters) {
            String lowerCaseValue = StringUtil.lowerCase(filter.getFieldValue());
            if (!StringUtil.isEmpty(lowerCaseValue)) {
                if (lowerCaseValue.contains("*") || lowerCaseValue.contains("?")) {
                    WildcardQuery wildcardQuery = new WildcardQuery(new Term(filter.getFieldName(), lowerCaseValue));
                    booleanQuery.add(wildcardQuery, BooleanClause.Occur.MUST);
                } else {
                    TermQuery termQuery = new TermQuery(new Term(filter.getFieldName(), lowerCaseValue));
                    booleanQuery.add(termQuery, BooleanClause.Occur.MUST);
                }
            }
        }
    }

    @Override
    public <T> List<T> getObjects(final List<Filter> filters, final Class<T> clazz) throws IOException {
        List<T> objects = new ArrayList<T>();

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createClassQuery(clazz), BooleanClause.Occur.MUST);
        addFilters(filters, booleanQuery);

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObject(String, Class): {}", booleanQuery.toString());
        }

        List<Document> documents = findDocuments(booleanQuery);
        for (Document document : documents) {
            String resourceName = document.get(DEFAULT_FIELD_RESOURCE);
            Resource resource = getResourceRegistry().get(resourceName);
            String json = document.get(DEFAULT_FIELD_JSON);
            objects.add(clazz.cast(resource.deserialize(json)));
        }
        return objects;
    }

    @Override
    public <T> List<T> getObjects(final List<Filter> filters, final Class<T> clazz,
                                  final Integer page, final Integer pageSize) throws IOException {
        List<T> objects = new ArrayList<T>();

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createClassQuery(clazz), BooleanClause.Occur.MUST);
        addFilters(filters, booleanQuery);

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObject(String, Class): {}", booleanQuery.toString());
        }

        List<Document> documents = findDocuments(booleanQuery, page, pageSize);
        for (Document document : documents) {
            String resourceName = document.get(DEFAULT_FIELD_RESOURCE);
            Resource resource = getResourceRegistry().get(resourceName);
            String json = document.get(DEFAULT_FIELD_JSON);
            objects.add(clazz.cast(resource.deserialize(json)));
        }
        return objects;
    }

    @Override
    public <T> Integer countObjects(final List<Filter> filters, final Class<T> clazz) throws IOException {
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createClassQuery(clazz), BooleanClause.Occur.MUST);
        addFilters(filters, booleanQuery);

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObject(String, Class): {}", booleanQuery.toString());
        }

        IndexSearcher searcher = createIndexSearcher();
        TopDocs docs = searcher.search(booleanQuery, DEFAULT_MAX_DOCUMENTS);
        return docs.totalHits;
    }

    @Override
    public List<Searchable> getObjects(final List<Filter> filters, final Resource resource) throws IOException {
        List<Searchable> objects = new ArrayList<Searchable>();

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createResourceQuery(resource), BooleanClause.Occur.MUST);
        addFilters(filters, booleanQuery);

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObject(String, Class): {}", booleanQuery.toString());
        }

        List<Document> documents = findDocuments(booleanQuery);
        for (Document document : documents) {
            String json = document.get(DEFAULT_FIELD_JSON);
            objects.add(resource.deserialize(json));
        }
        return objects;
    }

    @Override
    public List<Searchable> getObjects(final List<Filter> filters, final Resource resource,
                                       final Integer page, final Integer pageSize) throws IOException {
        List<Searchable> objects = new ArrayList<Searchable>();

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createResourceQuery(resource), BooleanClause.Occur.MUST);
        addFilters(filters, booleanQuery);

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObject(String, Class): {}", booleanQuery.toString());
        }

        List<Document> documents = findDocuments(booleanQuery, page, pageSize);
        for (Document document : documents) {
            String json = document.get(DEFAULT_FIELD_JSON);
            objects.add(resource.deserialize(json));
        }
        return objects;
    }

    @Override
    public Integer countObjects(final List<Filter> filters, final Resource resource) throws IOException {
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createResourceQuery(resource), BooleanClause.Occur.MUST);
        addFilters(filters, booleanQuery);

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObject(String, Class): {}", booleanQuery.toString());
        }

        IndexSearcher searcher = createIndexSearcher();
        TopDocs docs = searcher.search(booleanQuery, DEFAULT_MAX_DOCUMENTS);
        return docs.totalHits;
    }

    @Override
    public <T> List<T> getObjects(final String searchString, final Class<T> clazz)
            throws ParseException, IOException {
        List<T> objects = new ArrayList<T>();

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createClassQuery(clazz), BooleanClause.Occur.MUST);
        if (!StringUtil.isEmpty(searchString)) {
            Query query = parser.parse(searchString);
            booleanQuery.add(query, BooleanClause.Occur.MUST);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObjects(String, Class): {}", booleanQuery.toString());
        }

        List<Document> documents = findDocuments(booleanQuery);
        for (Document document : documents) {
            String resourceName = document.get(DEFAULT_FIELD_RESOURCE);
            Resource resource = getResourceRegistry().get(resourceName);
            String json = document.get(DEFAULT_FIELD_JSON);
            objects.add(clazz.cast(resource.deserialize(json)));
        }
        return objects;
    }

    @Override
    public <T> List<T> getObjects(final String searchString, final Class<T> clazz, final Integer page,
                                  final Integer pageSize) throws ParseException, IOException {
        List<T> objects = new ArrayList<T>();

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createClassQuery(clazz), BooleanClause.Occur.MUST);
        if (!StringUtil.isEmpty(searchString)) {
            Query query = parser.parse(searchString);
            booleanQuery.add(query, BooleanClause.Occur.MUST);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObjects(String, Class): {}", booleanQuery.toString());
        }

        List<Document> documents = findDocuments(booleanQuery, page, pageSize);
        for (Document document : documents) {
            String resourceName = document.get(DEFAULT_FIELD_RESOURCE);
            Resource resource = getResourceRegistry().get(resourceName);
            String json = document.get(DEFAULT_FIELD_JSON);
            objects.add(clazz.cast(resource.deserialize(json)));
        }
        return objects;
    }

    @Override
    public List<Searchable> getObjects(final String searchString, final Resource resource)
            throws ParseException, IOException {
        List<Searchable> objects = new ArrayList<Searchable>();

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createResourceQuery(resource), BooleanClause.Occur.MUST);
        if (!StringUtil.isEmpty(searchString)) {
            Query query = parser.parse(searchString);
            booleanQuery.add(query, BooleanClause.Occur.MUST);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObjects(String, Resource): {}", booleanQuery.toString());
        }

        List<Document> documents = findDocuments(booleanQuery);
        for (Document document : documents) {
            String json = document.get(DEFAULT_FIELD_JSON);
            objects.add(resource.deserialize(json));
        }
        return objects;
    }

    @Override
    public List<Searchable> getObjects(final String searchString, final Resource resource, final Integer page,
                                       final Integer pageSize) throws ParseException, IOException {
        List<Searchable> objects = new ArrayList<Searchable>();

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(createResourceQuery(resource), BooleanClause.Occur.MUST);
        if (!StringUtil.isEmpty(searchString)) {
            Query query = parser.parse(searchString);
            booleanQuery.add(query, BooleanClause.Occur.MUST);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Query getObjects(String, Resource): {}", booleanQuery.toString());
        }

        List<Document> documents = findDocuments(booleanQuery, page, pageSize);
        for (Document document : documents) {
            String json = document.get(DEFAULT_FIELD_JSON);
            objects.add(resource.deserialize(json));
        }
        return objects;
    }

    @Override
    public void deleteObjects(final List<Searchable> objects, final Resource resource) throws IOException {
        IndexSearcher searcher = createIndexSearcher();
        IndexWriter writer = createIndexWriter();
        for (Searchable object : objects) {
            String jsonString = resource.serialize(object);
            Object jsonObject = JsonPath.read(jsonString, "$");
            deleteObject(jsonObject, resource, writer);
        }
        commit(writer, searcher);
    }

    @Override
    public void createObjects(final List<Searchable> objects, final Resource resource) throws IOException {
        IndexSearcher searcher = createIndexSearcher();
        IndexWriter writer = createIndexWriter();
        for (Searchable object : objects) {
            String jsonString = resource.serialize(object);
            Object jsonObject = JsonPath.read(jsonString, "$");
            BooleanQuery query = createObjectQuery(jsonObject, resource.getSearchableFields());
            query.add(createResourceQuery(resource), BooleanClause.Occur.MUST);
            TopDocs docs = searcher.search(query, DEFAULT_MAX_DOCUMENTS);
            if (docs.totalHits == 0) {
                writeObject(jsonObject, resource, writer);
            }
        }
        commit(writer, searcher);
    }

    @Override
    public void updateObjects(final List<Searchable> objects, final Resource resource) throws IOException {
        IndexSearcher searcher = createIndexSearcher();
        IndexWriter writer = createIndexWriter();
        for (Searchable object : objects) {
            String jsonString = resource.serialize(object);
            Object jsonObject = JsonPath.read(jsonString, "$");
            updateObject(jsonObject, resource, writer);
        }
        commit(writer, searcher);
    }
}
