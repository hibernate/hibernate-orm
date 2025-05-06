/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.TimeZoneStorageStrategy;

/**
 * Describes the storage of timezone information for zoned datetime types,
 * in particular, for the types {@link java.time.OffsetDateTime} and
 * {@link java.time.ZonedDateTime}.
 * <p>
 * A default {@code TimeZoneStorageType} may be configured explicitly using
 * {@value org.hibernate.cfg.AvailableSettings#TIMEZONE_DEFAULT_STORAGE}.
 * Otherwise, the storage type may be overridden for a given field or
 * property of an entity using the {@link TimeZoneStorage} annotation.
 * <p>
 * In choosing a {@code TimeZoneStorageType} we must consider whether a
 * round trip to the database, writing and then reading a zoned datetime,
 * preserves:
 * <ul>
 * <li>the {@linkplain java.time.OffsetDateTime#toInstant() instant}
 *     represented by the zoned datetime, and/or
 * <li>the {@linkplain java.time.OffsetDateTime#getOffset() offset} or
 *     {@linkplain java.time.ZonedDateTime#getZone() zone} in which the
 *     instant is represented.
 * </ul>
 * <p>
 * We must also consider the physical representation of the zoned datetime
 * in the database table.
 * <p>
 * The {@linkplain #DEFAULT default strategy} guarantees that a round trip
 * preserves the instant. Whether the zone or offset is preserved depends
 * on whether the underlying database has a {@code timestamp with time zone}
 * type which preserves offsets:
 * <ul>
 * <li>if the database does indeed have such an ANSI-compliant type, then
 *     both instant and zone or offset are preserved by round trips, but
 * <li>if not, it's guaranteed that the physical representation is in UTC,
 *     so that datetimes retrieved from the database will be represented in
 *     UTC.
 * </ul>
 * <p>
 * When this default strategy is not appropriate, recommended alternatives
 * are:
 * <ul>
 * <li>{@link #AUTO} or {@link #COLUMN}, which each guarantee that both
 *     instant and zone or offset are preserved by round trips on every
 *     platform, or
 * <li>{@link #NORMALIZE_UTC}, which guarantees that only the instant is
 *     preserved, and that datetimes retrieved from the database will always
 *     be represented in UTC.
 * </ul>
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @since 6.0
 *
 * @see TimeZoneStorage
 * @see TimeZoneStorageStrategy
 */
@Incubating
public enum TimeZoneStorageType {
	/**
	 * Stores the timezone by using the {@code with time zone}
	 * SQL column type.
	 * <p>
	 * Error if {@link Dialect#getTimeZoneSupport()} is not
	 * {@link org.hibernate.dialect.TimeZoneSupport#NATIVE}.
	 */
	NATIVE,
	/**
	 * Does not store the time zone, and instead:
	 * <ul>
	 * <li>when persisting to the database, normalizes JDBC timestamps to the
	 * {@linkplain org.hibernate.cfg.AvailableSettings#JDBC_TIME_ZONE}
	 * or to the JVM default time zone if not set.
	 * <li>when reading back from the database, sets the offset or zone
	 * of {@code OffsetDateTime}/{@code ZonedDateTime} values
	 * to the JVM default time zone.
	 * </ul>
	 * <p>
	 * Provided partly for backward compatibility with older
	 * versions of Hibernate.
	 */
	NORMALIZE,
	/**
	 * Does not preserve the time zone, and instead normalizes
	 * timestamps to UTC.
	 * <p>
	 * The DDL column type depends on the setting
	 * {@value org.hibernate.cfg.AvailableSettings#PREFERRED_INSTANT_JDBC_TYPE}.
	 */
	NORMALIZE_UTC,
	/**
	 * Stores the time zone in a separate column; works in
	 * conjunction with {@link TimeZoneColumn}.
	 */
	COLUMN,
	/**
	 * Stores the time zone either with {@link #NATIVE} if
	 * {@link Dialect#getTimeZoneSupport()} is
	 * {@link org.hibernate.dialect.TimeZoneSupport#NATIVE},
	 * otherwise uses the {@link #COLUMN} strategy.
	 * <p>
	 * This option automatically picks an appropriate strategy
	 * for the database dialect which preserves both the instant
	 * represented by a zoned datetime type, and the offset or
	 * timezone.
	 */
	AUTO,
	/**
	 * Stores the time zone either with {@link #NATIVE} if
	 * {@link Dialect#getTimeZoneSupport()} is
	 * {@link org.hibernate.dialect.TimeZoneSupport#NATIVE},
	 * otherwise uses the {@link #NORMALIZE_UTC} strategy.
	 * <p>
	 * This option automatically picks an appropriate strategy
	 * for the database dialect which preserves the instant
	 * represented by a zoned datetime type. It does not promise
	 * that the offset or timezone is preserved by a round trip
	 * to the database.
	 *
	 * @since 6.2
	 */
	DEFAULT
}
