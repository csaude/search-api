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
import com.muzima.search.api.model.object.Searchable;
import com.muzima.search.api.model.resolver.Resolver;
import com.muzima.search.api.model.serialization.Algorithm;
import com.muzima.search.api.module.JUnitModule;
import com.muzima.search.api.module.SearchModule;
import com.muzima.search.api.resource.ObjectResource;
import com.muzima.search.api.resource.Resource;
import com.muzima.search.api.resource.ResourceConstants;
import com.muzima.search.api.sample.domain.Patient;
import com.muzima.search.api.service.RestAssuredService;
import com.muzima.search.api.util.StreamUtil;
import com.muzima.search.api.util.StringUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RestAssuredServiceTest {

    private ServiceContext context;

    private RestAssuredService service;

    private static String patientGivenName;

    private static String patientFamilyName;

    private static String patientUuid;

    private static final String CORPUS_DIRECTORY = "sample/corpus";

    private static final String PATIENT_RESOURCE = "Patient Resource";

    private static final String SEARCH_PATIENT_RESOURCE = "Search Patient Resource";

    private static final String CORPUS_CONFIGURATION_FILE = "sample/j2l/patient-template.j2l";

    private final Logger logger = LoggerFactory.getLogger(RestAssuredServiceTest.class.getSimpleName());

    @BeforeClass
    public static void prepareData() throws Exception {
        // we can read this values from the configuration document but it will take more coding haha ...
        String patientUuidPath = "$['uuid']";
        String patientGivenNamePath = "$['personName.givenName']";
        String patientFamilyNamePath = "$['personName.familyName']";

        URL corpusUri = RestAssuredServiceTest.class.getResource(CORPUS_DIRECTORY);
        File corpusDirectory = new File(corpusUri.getPath());
        for (String corpusFile : corpusDirectory.list()) {
            String jsonPayload = StreamUtil.readAsString(new FileReader(new File(corpusDirectory, corpusFile)));
            // read the patient name information :)
            patientUuid = JsonPath.read(jsonPayload, patientUuidPath);
            patientGivenName = JsonPath.read(jsonPayload, patientGivenNamePath);
            patientFamilyName = JsonPath.read(jsonPayload, patientFamilyNamePath);
        }

        Assert.assertNotNull(patientUuid);
        Assert.assertNotNull(patientGivenName);
    }

    protected void registerResources(final Injector injector) throws Exception {
        URL url = RestAssuredServiceTest.class.getResource(CORPUS_CONFIGURATION_FILE);
        List<Object> configurations = JsonPath.read(url, "$['configurations']");

        for (Object configuration : configurations) {
            String name = JsonPath.read(configuration, ResourceConstants.RESOURCE_NAME);
            String root = JsonPath.read(configuration, ResourceConstants.ROOT_NODE);

            String searchableName = JsonPath.read(configuration, ResourceConstants.SEARCHABLE_CLASS);
            Class searchableClass = Class.forName(searchableName);
            Searchable searchable = (Searchable) searchableClass.newInstance();

            String algorithmName = JsonPath.read(configuration, ResourceConstants.ALGORITHM_CLASS);
            Class algorithmClass = Class.forName(algorithmName);
            Algorithm algorithm = (Algorithm) injector.getInstance(algorithmClass);

            String resolverName = JsonPath.read(configuration, ResourceConstants.RESOLVER_CLASS);
            Class resolverClass = Class.forName(resolverName);
            Resolver resolver = (Resolver) injector.getInstance(resolverClass);

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
            context = injector.getInstance(ServiceContext.class);
            context.registerResource(resource.getName(), resource);
        }
    }

    @Before
    public void prepare() throws Exception {
        Injector injector = Guice.createInjector(new SearchModule(), new JUnitModule());

        service = injector.getInstance(RestAssuredService.class);
        Assert.assertNotNull(service);

        registerResources(injector);
        context = injector.getInstance(ServiceContext.class);
        Resource patientResource = context.getResource(PATIENT_RESOURCE);
        Assert.assertNotNull(patientResource);

        URL corpus = RestAssuredServiceTest.class.getResource(CORPUS_DIRECTORY);
        List<Searchable> searchableList = service.loadObjects(StringUtil.EMPTY, patientResource, new File(corpus.getPath()));
        service.createObjects(searchableList, patientResource);
    }

    @After
    public void cleanUp() {
        String tmpDirectory = System.getProperty("java.io.tmpdir");
        String lucenePath = tmpDirectory + JUnitModule.LUCENE_DIRECTORY;

        File luceneDirectory = new File(lucenePath);
        for (String filename : luceneDirectory.list()) {
            File file = new File(luceneDirectory, filename);
            Assert.assertTrue(file.delete());
        }
    }

    /**
     * @verifies load objects based on the resource description
     * @see RestAssuredService#loadObjects(java.util.Map, com.muzima.search.api.resource.Resource)
     */
    @Test
    public void loadObjects_shouldLoadObjectsBasedOnTheResourceDescription() throws Exception {
        /*
         * This part of the unit test use the following assumption:
         * - You have installation of OpenMRS in your local computer
         * - At least a patient have name with letter "a" in the lucene repository
         */
        try {
            Resource resource = context.getResource(SEARCH_PATIENT_RESOURCE);
            Map<String, String> resourceParams = new HashMap<String, String>();
            resourceParams.put("q", "Tho");
            List<Searchable> searchables = service.loadObjects(resourceParams, resource);
            for (Searchable searchable : searchables) {
                logger.info("Patient uuid: {}", ((Patient)searchable).getUuid());
            }
        } catch (IOException e) {
            logger.error("Exception thrown while trying to connect to server through proxy!", e);
        }
    }

    /**
     * @verifies load object from filesystem based on the resource description
     * @see RestAssuredService#loadObjects(String, com.muzima.search.api.resource.Resource, java.io.File)
     */
    @Test
    public void loadObjects_shouldLoadObjectFromFilesystemBasedOnTheResourceDescription() throws Exception {
        // search for multiple patients
        List<Patient> patients = service.getObjects("givenName:test*", Patient.class);
        Assert.assertNotNull(patients);
        Assert.assertEquals(3, patients.size());
        // search for specific patient using the name
        patients = service.getObjects("familyName: " + StringUtil.quote(patientFamilyName), Patient.class);
        Assert.assertNotNull(patients);
        Assert.assertEquals(patientGivenName, patients.get(0).getGivenName());
        Assert.assertEquals(patientFamilyName, patients.get(0).getFamilyName());
    }

    /**
     * @verifies return object with matching key and type
     * @see RestAssuredService#getObject(String, Class)
     */
    @Test
    public void getObject_shouldReturnObjectWithMatchingKeyAndType() throws Exception {
        // search for specific patient using uuid
        Patient patient = service.getObject(patientUuid, Patient.class);
        Assert.assertNotNull(patient);
        Assert.assertEquals(patientUuid, patient.getUuid());
    }

    /**
     * @verifies return null when no object match the key and type
     * @see RestAssuredService#getObject(String, Class)
     */
    @Test
    public void getObject_shouldReturnNullWhenNoObjectMatchTheKeyAndType() throws Exception {
        // passing random uuid into the getObject method
        Patient patient = service.getObject(StringUtil.quote("1234"), Patient.class);
        Assert.assertNull(patient);
    }

    /**
     * @verifies return object with matching key
     * @see RestAssuredService#getObject(String, com.muzima.search.api.resource.Resource)
     */
    @Test
    public void getObject_shouldReturnObjectWithMatchingKey() throws Exception {
        Resource resource = context.getResource(PATIENT_RESOURCE);
        Patient patient = (Patient) service.getObject(patientUuid, resource);
        Assert.assertNotNull(patient);
        Assert.assertEquals(patientUuid, patient.getUuid());
    }

    /**
     * @verifies return null when no object match the key
     * @see RestAssuredService#getObject(String, com.muzima.search.api.resource.Resource)
     */
    @Test
    public void getObject_shouldReturnNullWhenNoObjectMatchTheKey() throws Exception {
        Resource resource = context.getResource(PATIENT_RESOURCE);
        Patient patient = (Patient) service.getObject(StringUtil.quote(UUID.randomUUID().toString()), resource);
        Assert.assertNull(patient);
    }

    /**
     * @verifies return all object matching the search search string and class
     * @see RestAssuredService#getObjects(String, Class)
     */
    @Test
    public void getObjects_shouldReturnAllObjectMatchingTheSearchSearchStringAndClass() throws Exception {
        List<Patient> patients;

        patients = service.getObjects("givenName: T*", Patient.class);
        Assert.assertNotNull(patients);
        Assert.assertEquals(3, patients.size());

//        String baseUri = StringUtil.sanitize("http://149.166.10.181:8081/openmrs-standalone/ws/rest/v1/patient/");
//        String query = "uri:" + StringUtil.quote(baseUri);
//        patients = service.getObjects(query, Patient.class);
//        Assert.assertNotNull(patients);
//        Assert.assertEquals(3, patients.size());
//
//        String patientQuery = "uri:" + StringUtil.quote(baseUri + patientUuid);
//        patients = service.getObjects(patientQuery, Patient.class);
//        Assert.assertNotNull(patients);
//        Assert.assertEquals(1, patients.size());
    }

    /**
     * @verifies return empty list when no object match the search string and class
     * @see RestAssuredService#getObjects(String, Class)
     */
    @Test
    public void getObjects_shouldReturnEmptyListWhenNoObjectMatchTheSearchStringAndClass() throws Exception {
        List<Patient> patients;
        patients = service.getObjects("name: Zz*", Patient.class);
        Assert.assertNotNull(patients);
        Assert.assertEquals(0, patients.size());
    }

    /**
     * @verifies return all object matching the search search string and resource
     * @see RestAssuredService#getObjects(String, com.muzima.search.api.resource.Resource)
     */
    @Test
    public void getObjects_shouldReturnAllObjectMatchingTheSearchSearchStringAndResource() throws Exception {
        Resource resource = context.getResource(PATIENT_RESOURCE);
        List<Searchable> patients = service.getObjects("givenName: T*", resource);
        Assert.assertNotNull(patients);
        Assert.assertEquals(3, patients.size());
        for (Object patient : patients) {
            Assert.assertNotNull(patient);
            Assert.assertEquals(Patient.class, patient.getClass());
        }
    }

    /**
     * @verifies return empty list when no object match the search string and resource
     * @see RestAssuredService#getObjects(String, com.muzima.search.api.resource.Resource)
     */
    @Test
    public void getObjects_shouldReturnEmptyListWhenNoObjectMatchTheSearchStringAndResource() throws Exception {
        Resource resource = context.getResource(PATIENT_RESOURCE);
        List<Searchable> patients = service.getObjects("name: Zz*", resource);
        Assert.assertNotNull(patients);
        Assert.assertEquals(0, patients.size());
    }

    /**
     * @verifies remove an object from the internal index system
     * @see RestAssuredService#deleteObjects(java.util.List, com.muzima.search.api.resource.Resource)
     */
    @Test
    public void invalidate_shouldRemoveAnObjectFromTheInternalIndexSystem() throws Exception {
        Patient patient = service.getObject(patientUuid, Patient.class);
        Assert.assertNotNull(patient);
        Assert.assertEquals(patientUuid, patient.getUuid());

        Resource resource = context.getResource(PATIENT_RESOURCE);
        service.deleteObjects(Arrays.asList((Searchable) patient), resource);

        Patient afterDeletionPatient = service.getObject(patientUuid, Patient.class);
        Assert.assertNull(afterDeletionPatient);
    }
}
