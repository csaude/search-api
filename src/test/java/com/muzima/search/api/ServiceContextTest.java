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

package com.muzima.search.api;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jayway.jsonpath.JsonPath;
import com.muzima.search.api.context.ServiceContext;
import com.muzima.search.api.exception.ServiceException;
import com.muzima.search.api.internal.file.ResourceFileFilter;
import com.muzima.search.api.model.resolver.Resolver;
import com.muzima.search.api.model.serialization.Algorithm;
import com.muzima.search.api.module.JUnitModule;
import com.muzima.search.api.module.SearchModule;
import com.muzima.search.api.resource.Resource;
import com.muzima.search.api.resource.ResourceConstants;
import com.muzima.search.api.resource.SearchableField;
import com.muzima.search.api.util.StringUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class ServiceContextTest {

    private ServiceContext serviceContext;

    @Before
    public void prepare() throws Exception {
        Injector injector = Guice.createInjector(new SearchModule(), new JUnitModule());
        serviceContext = injector.getInstance(ServiceContext.class);
    }

    /**
     * @verifies register resource object.
     * @see ServiceContext#registerResource(String, com.muzima.search.api.resource.Resource)
     */
    @Test
    public void registerResource_shouldRegisterResourceObject() throws Exception {
        String resourceName = "Example Resource";

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getName()).thenReturn(resourceName);

        serviceContext.registerResource(resource.getName(), resource);
        // check the registration process
        Assert.assertTrue(serviceContext.getResources().size() > 0);

        // check the registered resource internal property
        Resource registeredResource = serviceContext.getResource(resourceName);
        Assert.assertNotNull(registeredResource);
    }

    /**
     * @verifies not register resource without resource name.
     * @see ServiceContext#registerResource(String, com.muzima.search.api.resource.Resource)
     */
    @Test(expected = ServiceException.class)
    public void registerResource_shouldNotRegisterResourceWithoutResourceName() throws Exception {
        String resourceName = null;

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getName()).thenReturn(resourceName);

        // should throw exception here
        serviceContext.registerResource(resourceName, resource);
    }

    /**
     * @verifies only register resource files with j2l extension.
     * @see ServiceContext#registerResources(java.io.File)
     */
    @Test
    public void registerResources_shouldOnlyRegisterResourceFilesWithJ2lExtension() throws Exception {

        URL url = ServiceContextTest.class.getResource("sample/j2l");
        File resourceFile = new File(url.getPath());
        serviceContext.registerResources(resourceFile);
        Assert.assertEquals(2, serviceContext.getResources().size());
    }

    /**
     * @verifies recursively register all resources inside directory.
     * @see ServiceContext#registerResources(java.io.File)
     */
    @Test
    public void registerResources_shouldRecursivelyRegisterAllResourcesInsideDirectory() throws Exception {

        URL url = ServiceContextTest.class.getResource("sample/j2l");
        File resourceFile = new File(url.getPath());
        serviceContext.registerResources(resourceFile);

        File[] files = resourceFile.listFiles(new ResourceFileFilter());
        for (File file : files) {
            List<Object> configurationObjects = JsonPath.read(file, "$['configurations']");
            for (Object configurationObject : configurationObjects) {
                String resourceName = JsonPath.read(configurationObject, ResourceConstants.RESOURCE_NAME);
                Resource registeredResource = serviceContext.getResource(resourceName);
                Assert.assertNotNull(registeredResource);
                Assert.assertTrue(Algorithm.class.isAssignableFrom(registeredResource.getAlgorithm().getClass()));
                Assert.assertTrue(Resolver.class.isAssignableFrom(registeredResource.getResolver().getClass()));

                String uniqueField = JsonPath.read(configurationObject, ResourceConstants.UNIQUE_FIELD);
                List<String> uniqueKeyFields = Arrays.asList(StringUtil.split(uniqueField, ","));
                for (SearchableField searchableField : registeredResource.getSearchableFields()) {
                    if (uniqueKeyFields.contains(searchableField.getName()))
                        Assert.assertEquals(Boolean.TRUE, searchableField.isUnique());
                }
            }
        }
    }

    /**
     * @verifies return all registered resource object.
     * @see ServiceContext#getResources()
     */
    @Test
    public void getResources_shouldReturnAllRegisteredResourceObject() throws Exception {

        URL url = ServiceContextTest.class.getResource("sample/j2l");
        File resourceFile = new File(url.getPath());
        serviceContext.registerResources(resourceFile);

        File[] files = resourceFile.listFiles(new ResourceFileFilter());
        Assert.assertNotNull(files);
        for (File file : files) {
            List<Object> configurationObjects = JsonPath.read(file, "$['configurations']");
            for (Object configurationObject : configurationObjects) {
                String resourceName = JsonPath.read(configurationObject, ResourceConstants.RESOURCE_NAME);
                Resource registeredResource = serviceContext.getResource(resourceName);
                Assert.assertNotNull(registeredResource);
            }
        }
    }

    /**
     * @verifies return resource object based on the name of the resource.
     * @see ServiceContext#getResource(String)
     */
    @Test
    public void getResource_shouldReturnResourceObjectBasedOnTheNameOfTheResource() throws Exception {

        URL url = ServiceContextTest.class.getResource("sample/j2l");
        File resourceFile = new File(url.getPath());
        serviceContext.registerResources(resourceFile);

        File[] files = resourceFile.listFiles(new ResourceFileFilter());
        Assert.assertNotNull(files);
        for (File file : files) {
            List<Object> configurationObjects = JsonPath.read(file, "$['configurations']");
            for (Object configurationObject : configurationObjects) {
                String resourceName = JsonPath.read(configurationObject, ResourceConstants.RESOURCE_NAME);
                Resource registeredResource = serviceContext.getResource(resourceName);
                Assert.assertNotNull(registeredResource);
            }
        }
    }

    /**
     * @verifies return removed resource object
     * @see ServiceContext#removeResource(com.muzima.search.api.resource.Resource)
     */
    @Test
    public void removeResource_shouldReturnRemovedResourceObject() throws Exception {

        URL url = ServiceContextTest.class.getResource("sample/j2l");
        File resourceFile = new File(url.getPath());
        serviceContext.registerResources(resourceFile);

        int resourceCounter = serviceContext.getResources().size();
        Resource registeredResource = serviceContext.getResource("Patient Resource");
        Resource removedResource = serviceContext.removeResource(registeredResource);
        Assert.assertEquals(registeredResource, removedResource);

        Assert.assertEquals(resourceCounter - 1, serviceContext.getResources().size());
    }
}
