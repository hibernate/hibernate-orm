/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Describes the storage strategies understood by Hibernate.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
@Incubating
public enum TimeZoneStorageStrategy {
	/**
	 * Stores the time zone through the "with time zone" types which retain the information.
	 */
	NATIVE,
	/**
	 * Stores the time zone in a separate column.
	 */
	COLUMN,
	/**
	 * Doesn't store the time zone, but instead normalizes to UTC.
	 */
	NORMALIZE
}
