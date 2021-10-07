/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

/**
 * The type of storage to use for the time zone information.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
@Incubating
public enum TimeZoneType {

	/**
	 * Stores the time zone id as String.
	 */
	ZONE_ID,
	/**
	 * Stores the offset seconds of a timestamp as Integer.
	 */
	OFFSET;

}
