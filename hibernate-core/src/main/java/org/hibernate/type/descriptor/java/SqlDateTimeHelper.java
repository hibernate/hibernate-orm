/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.hibernate.Internal;

/**
 * A helper for correctly converting {@link java.sql.Date} to {@link LocalDate}
 * and {@link java.sql.Timestamp} to {@link LocalDateTime}.
 * <p>
 * This works around the JDK bug <a href="https://bugs.openjdk.org/browse/JDK-8272194">JDK-8272194</a>
 * using the same <a href="https://github.com/openjdk/jdk/commit/2be8e88ce71704825c50246ba028153a5c0376db">fix that was used in the JDK</a>.
 * </p>
 */
@Internal
public final class SqlDateTimeHelper {

	/**
	 * The epoch millisecond value of 0002-01-01T00:00:00.000 at UTC in
	 * the Julian-Gregorian hybrid calendar system.
	 * We cannot use 1st January 1AD as different timezones could possibly
	 * offset the date back into BC, thus resulting in the incorrect BC year.
	 * While a one year margin is considerably larger than any possible
	 * timezone offset, it gives us a comfortable distance while still
	 * providing a faster code path for almost 1970 years.
	 */
	private static final long TWO_AD_AT_UTC_EPOCH_MILLIS = -62104233600000L;

	/**
	 * Should be used instead of {@link java.sql.Date#toLocalDate()} as that does NOT work for BC dates.
	 * @param date The date to convert into a {@link LocalDate}.
	 * @return The given date in the {@link LocalDate} form.
	 */
	@SuppressWarnings("deprecation")
	static LocalDate toLocalDate(final java.sql.Date date) {
		return LocalDate.of(
			toGregorianProlepticYear(date.getYear() + 1900, date.getTime()),
			date.getMonth() + 1,
			date.getDate());
	}

	/**
	 * Should be used instead of {@link java.sql.Timestamp#toLocalDateTime()} as that does NOT work for BC dates.
	 * @param timestamp The timestamp to convert into a {@link LocalDateTime}.
	 * @return The given timestamp in the {@link LocalDateTime} form.
	 */
	@SuppressWarnings("deprecation")
	static LocalDateTime toLocalDateTime(final java.sql.Timestamp timestamp) {
		return LocalDateTime.of(
			toGregorianProlepticYear(timestamp.getYear() + 1900, timestamp.getTime()),
			timestamp.getMonth() + 1,
			timestamp.getDate(),
			timestamp.getHours(),
			timestamp.getMinutes(),
			timestamp.getSeconds(),
			timestamp.getNanos());
	}

	/**
	 * Converts an era-relative calendar year into the correct proleptic year.
	 * <p>
	 * The milliseconds is required to determine if the given year is a BC or AD year.
	 *
	 * @param year the era-relative calendar year.
	 * @param millis the epoch millisecond represented by the date with the given year
	 * used to determine the calendar era.
	 * @return the proleptic year corresponding to {@code year} and {@code millis}
	 */
	static int toGregorianProlepticYear(int year, long millis) {
		// We need to determine the era of the year using the given millis.
		// However, deriving a new calendar is a relatively expensive operation
		// that we would ideally avoid if possible.
		// Given that we can comfortably state that any dates on or after
		// 0002-01-01 are AD, we only need to test dates earlier than this cutoff.
		if (millis < TWO_AD_AT_UTC_EPOCH_MILLIS) {
			final GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTimeInMillis(millis);
			if (calendar.get(Calendar.ERA) == GregorianCalendar.BC) {
				// Adjust the BC date into a negative astronomical date.
				// As there is no year 0 in the Gregorian calendar
				// we also have to adjust the BC year by 1.
				// 1 BC becomes year 0, 2 BC becomes year -1 and so on.
				year = 1 - year;
			}
		}
		return year;
	}

	private SqlDateTimeHelper() { }

}
