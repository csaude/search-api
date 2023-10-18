/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.sample.algorithm;

import com.jayway.jsonpath.JsonPath;
import com.muzima.search.api.model.object.Searchable;
import com.muzima.search.api.model.serialization.Algorithm;
import com.muzima.search.api.sample.domain.Patient;
import net.minidev.json.JSONObject;

import java.io.IOException;

public class PatientAlgorithm implements Algorithm {

    /**
     * Implementation of this method will define how the patient will be serialized from the JSON representation.
     *
     * @param serialized the json representation
     * @return the concrete patient object
     */
    @Override
    public Searchable deserialize(final String serialized, final boolean isFullDeserialization) throws IOException {
        Patient patient = new Patient();

        // get the full json object representation and then pass this around to the next JsonPath.read()
        // this should minimize the time for the subsequent read() call
        Object jsonObject = JsonPath.read(serialized, "$");

        String uuid = JsonPath.read(jsonObject, "$['uuid']");
        patient.setUuid(uuid);

        String givenName = JsonPath.read(jsonObject, "$['personName.givenName']");
        patient.setGivenName(givenName);

        String middleName = JsonPath.read(jsonObject, "$['personName.middleName']");
        patient.setMiddleName(middleName);

        String familyName = JsonPath.read(jsonObject, "$['personName.familyName']");
        patient.setFamilyName(familyName);

        String identifier = JsonPath.read(jsonObject, "$['patientIdentifier.identifier']");
        patient.setIdentifier(identifier);

        String gender = JsonPath.read(jsonObject, "$['gender']");
        patient.setGender(gender);

        return patient;
    }

    /**
     * Implementation of this method will define how the patient will be deserialized into the JSON representation.
     *
     * @param object the patient
     * @return the json representation
     */
    @Override
    public String serialize(final Searchable object, final boolean isFullSerialization) throws IOException {
        Patient patient = (Patient) object;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uuid", patient.getUuid());
        jsonObject.put("personName.givenName", patient.getGivenName());
        jsonObject.put("personName.middleName", patient.getMiddleName());
        jsonObject.put("personName.familyName", patient.getFamilyName());
        jsonObject.put("patientIdentifier.identifier", patient.getIdentifier());
        jsonObject.put("gender", patient.getGender());
        return jsonObject.toJSONString();
    }
}
