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
public class ComparableComparator<T extends Comparable<T>> implements Comparator<T>, Serializable {

	@SuppressWarnings("rawtypes")
	public static final Comparator INSTANCE = new ComparableComparator();

	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> Comparator<T> instance() {
		return INSTANCE;
	}

	public int compare(T one, T another) {
		return one.compareTo( another );
	}
}
