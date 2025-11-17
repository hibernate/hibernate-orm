/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.compare;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Delegates to Comparable
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class ComparableComparator<T extends Comparable> implements Comparator<T>, Serializable {

	public static final Comparator INSTANCE = new ComparableComparator();

	@SuppressWarnings("unchecked")
	public static <T extends Comparable> Comparator<T> instance() {
		return INSTANCE;
	}

	@SuppressWarnings("unchecked")
	public int compare(Comparable one, Comparable another) {
		return one.compareTo( another );
	}
}
