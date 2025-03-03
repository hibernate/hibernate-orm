/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
