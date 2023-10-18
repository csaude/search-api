/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.search.api.util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 */
public class ISO8601UtilTest {
    @Test
    public void shouldReturnADifferentResultWhenTheTimeZoneIsChanged() throws Exception {
        Calendar aCalendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
        Date dateOfCalender = aCalendar.getTime();
        Calendar calendarInDifferentTimeZone = Calendar.getInstance(TimeZone.getTimeZone("Europe/Athens"));
        calendarInDifferentTimeZone.setTime(dateOfCalender);

        String calendarIso8601 = ISO8601Util.fromCalendar(aCalendar);
        String calendarInDifferentTimeZoneIso8601 = ISO8601Util.fromCalendar(calendarInDifferentTimeZone);
        assertThat(calendarIso8601, not(is(calendarInDifferentTimeZoneIso8601)));
    }
}
