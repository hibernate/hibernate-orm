/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.util.Comparator;

/**
 * Comparator for component arrays.
 *
 * @author Christian Beikov
 */
public class ComponentArrayComparator<E> implements Comparator<E[]> {

	private final JavaType<E>[] components;

	public ComponentArrayComparator(JavaType<E>[] components) {
		this.components = components;
	}

	@Override
	public int compare(E[] o1, E[] o2) {
		for ( int i = 0; i < components.length; i++ ) {
			final int cmp = components[i].getComparator().compare( o1[i], o2[i] );
			if ( cmp != 0 ) {
				return cmp;
			}
		}

		return 0;
	}
}
