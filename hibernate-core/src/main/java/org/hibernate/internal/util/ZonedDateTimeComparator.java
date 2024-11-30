/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Comparator;

/**
 * @author Andrea Boriero
 */
public class ZonedDateTimeComparator implements Comparator<ZonedDateTime>, Serializable {
	public static final Comparator<ZonedDateTime> INSTANCE = new ZonedDateTimeComparator();

	public int compare(ZonedDateTime one, ZonedDateTime another) {
		return one.toInstant().compareTo( another.toInstant() );
	}
}
