/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.aspect;

import com.muzima.search.api.model.object.Searchable;
import com.muzima.search.api.resource.Resource;

/**
 * TODO: The aspect class will assign the checksum value into the searchable object.
 * The point-cut will be the execution of Algorithm.deserialize(String). The advice will be executed after the method.
 * We need to get the serialized value, generate the checksum and then assign the checksum value into the searchable.
 */
public aspect ChecksumAspect {

    pointcut generateChecksum(Resource resource, String serialized):
            execution(Searchable Resource.deserialize(String)) && target(resource) && args(serialized);

    after(Resource resource, String serialized) returning (Searchable searchable):
            generateChecksum(resource, serialized) {
    }
}
