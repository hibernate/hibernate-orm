/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.util.Comparator;

/**
 * Comparator for component arrays.
 *
 * @author Christian Beikov
 */
public class ComponentArrayComparator implements Comparator<Object[]> {

	private final JavaTypeDescriptor<Object>[] components;

	public ComponentArrayComparator(JavaTypeDescriptor<Object>[] components) {
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
