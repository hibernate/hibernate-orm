/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.Incubating;

/**
 * Describes the support for "with time zone" types.
 *
 * @author Christian Beikov
 */
@Incubating
public enum TimeZoneSupport {
	/**
	 * The "with time zone" types retain the time zone information.
	 */
	NATIVE,
	/**
	 * The "with time zone" types normalize to UTC.
	 */
	NORMALIZE,
	/**
	 * No support for "with time zone" types.
	 */
	NONE;
}
