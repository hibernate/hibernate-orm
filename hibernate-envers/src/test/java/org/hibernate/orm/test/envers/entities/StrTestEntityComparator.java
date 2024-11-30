/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities;

import java.util.Comparator;

public class StrTestEntityComparator implements Comparator<StrTestEntity> {
	public static final StrTestEntityComparator INSTANCE = new StrTestEntityComparator();

	@Override
	public int compare(StrTestEntity o1, StrTestEntity o2) {
		return o1.getStr().compareTo( o2.getStr() );
	}
}
