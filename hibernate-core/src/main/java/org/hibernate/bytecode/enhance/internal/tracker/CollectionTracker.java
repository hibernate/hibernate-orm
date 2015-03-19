/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.bytecode.enhance.internal.tracker;

import java.util.Arrays;

/**
 * small low memory class to keep track of the number of elements in a collection
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class CollectionTracker {

	private String[] names;
	private int[] sizes;

	public CollectionTracker() {
		names = new String[0];
		sizes = new int[0];
	}

	public void add(String name, int size) {
		for ( int i = 0; i < names.length; i++ ) {
			if ( names[i].equals( name ) ) {
				sizes[i] = size;
				return;
			}
		}
		names = Arrays.copyOf( names, names.length + 1 );
		names[names.length - 1] = name;
		sizes = Arrays.copyOf( sizes, sizes.length + 1 );
		sizes[sizes.length - 1] = size;
	}

	public int getSize(String name) {
		for ( int i = 0; i < names.length; i++ ) {
			if ( name.equals( names[i] ) ) {
				return sizes[i];
			}
		}
		return -1;
	}

}
