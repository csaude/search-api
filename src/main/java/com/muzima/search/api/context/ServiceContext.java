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

package com.muzima.search.api.context;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jayway.jsonpath.JsonPath;
import com.muzima.search.api.exception.ServiceException;
import com.muzima.search.api.internal.file.ResourceFileFilter;
import com.muzima.search.api.model.object.Searchable;
import com.muzima.search.api.model.resolver.Resolver;
import com.muzima.search.api.model.serialization.Algorithm;
import com.muzima.search.api.resource.ObjectResource;
import com.muzima.search.api.resource.Resource;
import com.muzima.search.api.resource.ResourceConstants;
import com.muzima.search.api.util.StringUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Singleton
public final class ServiceContext {

    @Inject
    private Map<String, Resource> resourceRegistry;

    private Map<String, Resource> getResourceRegistry() {
        return resourceRegistry;
    }

    /**
     * Register a new resource object.
     * <p/>
     * When registering resources which will be created from file in the filesystem or the project, make sure that the
     * classes inside the resources (searchable object, algorithm or resolver) doesn't require injection from Guice.
     * When any of the class inside the resource object will require injection, the client / user of the search-api
     * must define their own way to create the resource object using Guice's injector and then use
     * <code>registerResource(String, Resource)</code> to register the resource.
     * <p/>
     * Both <code>registerResources(File)</code> or <code>registerResource(InputStream)</code> are using standard Java
     * class loading and class instantiation mechanism to create the classes required by the resource object.
     *
     * @param name     the name of the resource (will be used to retrieve the resource later on).
     * @param resource the resource object.
     * @throws ServiceException when name or resource is invalid (null).
     */
    public void registerResource(final String name, final Resource resource) throws ServiceException {
        if (StringUtil.isEmpty(name))
            throw new ServiceException("Trying to register resource without handle.");

        if (resource == null)
            throw new ServiceException("Trying to register invalid resource object.");

        getResourceRegistry().put(name, resource);
    }

    /**
     * Read the input file and then convert each file into resource object and register them.
     * <p/>
     * When registering resources which will be created from file in the filesystem or the project, make sure that the
     * classes inside the resources (searchable object, algorithm or resolver) doesn't require injection from Guice.
     * When any of the class inside the resource object will require injection, the client / user of the search-api
     * must define their own way to create the resource object using Guice's injector and then use
     * <code>registerResource(String, Resource)</code> to register the resource.
     * <p/>
     * Both <code>registerResources(File)</code> or <code>registerResource(InputStream)</code> are using standard Java
     * class loading and class instantiation mechanism to create the classes required by the resource object.
     *
     * @param file the file (could be a directory too).
     * @throws IOException when the parser fail to read the configuration file.
     * @should recursively register all resources inside directory.
     * @should only register resource files with j2l extension.
     * @should create valid resource object based on the resource file.
     */
    public void registerResources(final File file) throws IOException, ServiceException {
        FileFilter fileFilter = new ResourceFileFilter();
        if (!file.isDirectory() && fileFilter.accept(file)) {
            registerResource(new FileInputStream(file));
        } else {
            File[] files = file.listFiles(fileFilter);
            if (files != null) {
                for (File resourceFile : files)
                    registerResources(resourceFile);
            }
        }
    }

    /**
     * Read the input stream and then convert it into resource object and register them.
     * <p/>
     * When registering resources which will be created from file in the filesystem or the project, make sure that the
     * classes inside the resources (searchable object, algorithm or resolver) doesn't require injection from Guice.
     * When any of the class inside the resource object will require injection, the client / user of the search-api
     * must define their own way to create the resource object using Guice's injector and then use
     * <code>registerResource(String, Resource)</code> to register the resource.
     * <p/>
     * Both <code>registerResources(File)</code> or <code>registerResource(InputStream)</code> are using standard Java
     * class loading and class instantiation mechanism to create the classes required by the resource object.
     *
     * @param inputStream the input stream to the configuration.
     * @throws IOException when the parser fail to read the configuration input stream.
     * @should create valid resource object based on the resource input stream.
     */
    public void registerResource(final InputStream inputStream) throws IOException, ServiceException {
        createResources(inputStream);
    }

    /**
     * Internal method to convert the actual resource input stream into the resource object.
     *
     * @param inputStream the configuration's input stream.
     * @throws IOException when the parser fail to read the configuration file
     */
    private void createResources(final InputStream inputStream) throws IOException {
        try {
            List<Object> configurations = JsonPath.read(inputStream, "$['configurations']");
            for (Object configuration : configurations) {
                Resource resource = createResource(configuration.toString());
                registerResource(resource.getName(), resource);
            }
        } catch (Exception e) {
            throw new IOException("Unable to register one or two configuration!", e);
        }
    }

    /**
     * Internal method to convert the actual resource string into the resource object.
     *
     * @param configuration the configuration.
     * @return the resource object
     * @throws IOException when the parser fail to read the configuration file
     */
    private Resource createResource(final String configuration) throws Exception {

        String name = JsonPath.read(configuration, ResourceConstants.RESOURCE_NAME);
        String root = JsonPath.read(configuration, ResourceConstants.ROOT_NODE);
        if (StringUtil.isEmpty(root)) {
            throw new ServiceException("Unable to create resource because of missing root node.");
        }

        String searchableName = JsonPath.read(configuration, ResourceConstants.SEARCHABLE_CLASS);
        if (StringUtil.isEmpty(root)) {
            throw new ServiceException("Unable to create resource because of missing searchable node.");
        }
        Class searchableClass = Class.forName(searchableName);
        Searchable searchable = (Searchable) searchableClass.newInstance();

        String algorithmName = JsonPath.read(configuration, ResourceConstants.ALGORITHM_CLASS);
        if (StringUtil.isEmpty(root)) {
            throw new ServiceException("Unable to create resource because of missing algorithm node.");
        }
        Class algorithmClass = Class.forName(algorithmName);
        Algorithm algorithm = (Algorithm) algorithmClass.newInstance();

        String resolverName = JsonPath.read(configuration, ResourceConstants.RESOLVER_CLASS);
        if (StringUtil.isEmpty(root)) {
            throw new ServiceException("Unable to create resource because of missing resolver node.");
        }
        Class resolverClass = Class.forName(resolverName);
        Resolver resolver = (Resolver) resolverClass.newInstance();

        List<String> uniqueFields = new ArrayList<String>();
        String uniqueField = JsonPath.read(configuration, ResourceConstants.UNIQUE_FIELD);
        if (uniqueField != null) {
            uniqueFields = Arrays.asList(StringUtil.split(uniqueField, ","));
        }
        Resource resource = new ObjectResource(name, root, searchable.getClass(), algorithm, resolver);
        Object searchableFields = JsonPath.read(configuration, ResourceConstants.SEARCHABLE_FIELD);
        if (searchableFields instanceof Map) {
            Map map = (Map) searchableFields;
            for (Object fieldName : map.keySet()) {
                Boolean unique = Boolean.FALSE;
                if (uniqueFields.contains(fieldName.toString())) {
                    unique = Boolean.TRUE;
                }
                String expression = map.get(fieldName).toString();
                resource.addFieldDefinition(fieldName.toString(), expression, unique);
            }
        }
        return resource;
    }

    /**
     * Get all registered resources from the resource registry.
     *
     * @return all registered resources.
     * @should return all registered resource object.
     */
    public Collection<Resource> getResources() {
        return getResourceRegistry().values();
    }

    /**
     * Get resource with the name denoted by the parameter.
     *
     * @param name the name of the resource
     * @return the matching resource object or null if no resource match have the matching name.
     * @should return resource object based on the name of the resource.
     */
    public Resource getResource(final String name) {
        return getResourceRegistry().get(name);
    }

    /**
     * Remove a resource from the resource registry.
     *
     * @param resource the resource to be removed
     * @return the removed resource or null if no resource was removed
     * @should return removed resource object
     */
    public Resource removeResource(final Resource resource) {
        return getResourceRegistry().remove(resource.getName());
    }
}
