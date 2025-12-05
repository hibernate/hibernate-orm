/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.util.Comparator;

/**
 * Comparator for things that cannot be compared (in a way we know about).
 *
 * @author Steve Ebersole
 */
public class IncomparableComparator implements Comparator {
	public static final IncomparableComparator INSTANCE = new IncomparableComparator();

	@Override
	public int compare(Object o1, Object o2) {
		return 0;
	}
}
