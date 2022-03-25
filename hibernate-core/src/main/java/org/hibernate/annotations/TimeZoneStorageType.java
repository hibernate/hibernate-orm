/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;

/**
 * Describes the storage of timezone information for zoned datetime types.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
@Incubating
public enum TimeZoneStorageType {
	/**
	 * Stores the timezone by using the {@code with time zone}
	 * SQL column type.
	 *
	 * Error if {@link Dialect#getTimeZoneSupport()} is not
	 * {@link org.hibernate.dialect.TimeZoneSupport#NATIVE}.
	 */
	NATIVE,
	/**
	 * Does not store the time zone, and instead normalizes
	 * timestamps to the JDBC timezone.
	 */
	NORMALIZE,
	/**
	 * Does not store the time zone, and instead normalizes
	 * timestamps to UTC.
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
	 */
	AUTO

}
