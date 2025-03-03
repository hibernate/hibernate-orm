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
public class ComponentArrayComparator implements Comparator<Object[]> {

	private final JavaType<Object>[] components;

	public ComponentArrayComparator(JavaType<Object>[] components) {
		this.components = components;
	}

	@Override
	public int compare(Object[] o1, Object[] o2) {
		for ( int i = 0; i < components.length; i++ ) {
			final int cmp = components[i].getComparator().compare( o1[i], o2[i] );
			if ( cmp != 0 ) {
				return cmp;
			}
		}

		return 0;
	}
}
