/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.compare;

import java.util.Calendar;
import java.util.Comparator;

/**
 * @author Gavin King
 */
public class CalendarComparator implements Comparator<Calendar> {
	public static final CalendarComparator INSTANCE = new CalendarComparator();

	public int compare(Calendar x, Calendar y) {
		if ( x.before( y ) ) {
			return -1;
		}
		if ( x.after( y ) ) {
			return 1;
		}
		return 0;
	}
}
