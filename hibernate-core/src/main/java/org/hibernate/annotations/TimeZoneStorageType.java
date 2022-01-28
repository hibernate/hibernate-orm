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
	 * timestamps to UTC.
	 */
	NORMALIZE
}
