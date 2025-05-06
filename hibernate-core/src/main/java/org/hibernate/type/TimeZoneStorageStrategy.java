/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.Incubating;

/**
 * Enumerates the possible storage strategies for offset or zoned datetimes.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @see org.hibernate.annotations.TimeZoneStorageType
 * @see org.hibernate.dialect.TimeZoneSupport
 */
@Incubating
public enum TimeZoneStorageStrategy {
	/**
	 * Stores the time zone via the {@code with time zone} SQL types which retain
	 * the information.
	 */
	NATIVE,
	/**
	 * Stores the time zone in a separate column.
	 */
	COLUMN,
	/**
	 * Does not store the time zone, and instead:
	 * <ul>
	 * <li>when persisting to the database, normalizes JDBC timestamps to the
	 *     {@linkplain org.hibernate.cfg.AvailableSettings#JDBC_TIME_ZONE
	 *     configured JDBC time zone}, or to the JVM default time zone
	 *     id no JDBC time zone is configured, or
	 * <li>when reading back from the database, sets the offset or zone
	 *     of {@code OffsetDateTime}/{@code ZonedDateTime} properties
	 *     to the JVM default time zone.
	 * </ul>
	 * <p>
	 * Provided partly for backward compatibility with older
	 * versions of Hibernate.
	 */
	NORMALIZE,
	/**
	 * Doesn't store the time zone, but instead normalizes to UTC.
	 */
	NORMALIZE_UTC
}
