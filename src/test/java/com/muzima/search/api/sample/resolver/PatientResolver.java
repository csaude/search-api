/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.sample.resolver;

import java.io.IOException;
import java.util.Map;

public class PatientResolver extends AbstractResolver {

    private static final String REPRESENTATION =
            "?v=custom:(uuid,gender,birthdate,personName.givenName,personName.middleName,personName.familyName," +
                    "patientIdentifier.identifier,patientIdentifier.identifierType.name)";

    /**
     * Return the full REST resource based on the search string passed to the method.
     *
     * @param resourceParams the search string
     * @return full URI to the REST resource
     */
    @Override
    public String resolve(final Map<String, String> resourceParams) throws IOException {
        return WEB_SERVER + WEB_CONTEXT + "ws/rest/v1/patient" + REPRESENTATION + "&q=" + resourceParams.get("q");
    }
}
