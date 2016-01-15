/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Comparator;

/**
 * @author Andrea Boriero
 */
public class ZonedDateTimeComparator implements Comparator<ZonedDateTime>, Serializable {
	public static final Comparator INSTANCE = new ZonedDateTimeComparator();

	public int compare(ZonedDateTime one, ZonedDateTime another) {
		return one.toInstant().compareTo( another.toInstant() );
	}
}
