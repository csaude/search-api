/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.model.serialization;

import com.muzima.search.api.model.object.Searchable;

import java.io.IOException;

/**
 * Base interface to serialize and de-serialize String into the correct object representation.
 */
public interface Algorithm {

    /**
     * Implementation of this method will define how the object will be serialized from the String representation.
     *
     * @param serialized the string representation
     * @param isFullDeserialization  determines whether to perform full deserialization. If set to false,
     *                             the implementation of this method can perform deserialization of minimal fields
     * @return the concrete object
     */
    Searchable deserialize(final String serialized, final boolean isFullDeserialization) throws IOException;

    /**
     * Implementation of this method will define how the object will be de-serialized into the String representation.
     *
     * @param object the object
     * @param isFullSerialization determines whether to perform full serialization. If set to false,
     *                             the implementation of this method can perform serialization of minimal fields
     * @return the string representation
     */
    String serialize(final Searchable object, final boolean isFullSerialization) throws IOException;
}
