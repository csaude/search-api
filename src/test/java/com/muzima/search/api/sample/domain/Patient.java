/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.sample.domain;

import com.muzima.search.api.model.object.Searchable;

public class Patient implements Searchable, Comparable<Patient> {

    private String uuid;

    private String givenName;

    private String middleName;

    private String familyName;

    private String identifier;

    private String gender;

    /**
     * Get the patient internal uuid
     *
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Set the patient internal uuid
     *
     * @param uuid the uuid
     */
    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    /**
     * Get the given name for the patient.
     *
     * @return the given name for the patient.
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * Set the given name for the patient.
     *
     * @param givenName the given name for the patient.
     */
    public void setGivenName(final String givenName) {
        this.givenName = givenName;
    }

    /**
     * Get the middle name for the patient.
     *
     * @return the middle name for the patient.
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Set the middle name for the patient.
     *
     * @param middleName the middle name for the patient.
     */
    public void setMiddleName(final String middleName) {
        this.middleName = middleName;
    }

    /**
     * Get the family name for the patient.
     *
     * @return the family name for the patient.
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Set the family name for the patient.
     *
     * @param familyName the family name for the patient.
     */
    public void setFamilyName(final String familyName) {
        this.familyName = familyName;
    }

    /**
     * Get the patient identifier
     *
     * @return the patient identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the patient identifier
     *
     * @param identifier the patient identifier
     */
    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    /**
     * Get the patient gender
     *
     * @return the patient gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * Set the patient gender
     *
     * @param gender the patient gender
     */
    public void setGender(final String gender) {
        this.gender = gender;
    }

    public String getDisplayName() {
        if (getFamilyName() == null && getGivenName() == null && getMiddleName() == null)
            return null;
        return getFamilyName() + ", " + getGivenName() + (getMiddleName() != null ? " " + getMiddleName(): "");
    }

    @Override
    public int compareTo(Patient patient) {
        if (this.getDisplayName() != null && patient.getDisplayName() != null) {
            return this.getDisplayName().toLowerCase().compareTo(patient.getDisplayName().toLowerCase());
        }
        return 0;
    }
}
