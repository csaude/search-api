/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.aspect;

/**
 * TODO: Write brief description about the class here.
 */
public aspect JoinPointTraceAspect {

    private int callDepth;

    pointcut traced(): !within(JoinPointTraceAspect);

    before(): traced() {
        print("Before", thisJoinPoint);
        callDepth++;
    }

    after(): traced() {
        callDepth--;
        print("After", thisJoinPoint);
    }

    private void print(String prefix, Object message) {
        for (int i = 0; i < callDepth; i++) {
            System.out.print("  ");
        }
        System.out.println(prefix + ": " + message);
    }
}
