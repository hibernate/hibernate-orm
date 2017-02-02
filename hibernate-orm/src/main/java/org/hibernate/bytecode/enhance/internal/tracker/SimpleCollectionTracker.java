/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.tracker;

import java.util.Arrays;

import org.hibernate.bytecode.enhance.spi.CollectionTracker;

/**
 * small low memory class to keep track of the number of elements in a collection
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class SimpleCollectionTracker implements CollectionTracker {

	private String[] names;
	private int[] sizes;

	public SimpleCollectionTracker() {
		names = new String[0];
		sizes = new int[0];
	}

	@Override
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

	@Override
	public int getSize(String name) {
		for ( int i = 0; i < names.length; i++ ) {
			if ( name.equals( names[i] ) ) {
				return sizes[i];
			}
		}
		return -1;
	}

}
