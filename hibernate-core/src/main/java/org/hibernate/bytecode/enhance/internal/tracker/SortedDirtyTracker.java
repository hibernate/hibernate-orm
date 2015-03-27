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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * small low memory class to keep track of changed fields
 *
 * similar to BasicTracker but where the array is kept ordered to reduce the cost of verifying duplicates
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public final class SortedDirtyTracker {

	private String[] names;

	public SortedDirtyTracker() {
		names = new String[0];
	}

	public void add(String name) {
		// we do a binary search: even if we don't find the name at least we get the position to insert into the array
		int insert = 0;
		for ( int low = 0, high = names.length - 1; low <= high; ) {
			final int middle = low + ( ( high - low ) / 2 );
			if ( names[middle].compareTo( name ) > 0 ) {
				// bottom half: higher bound in (middle - 1) and insert position in middle
				high = middle - 1;
				insert = middle;
			}
			else if( names[middle].compareTo( name ) < 0 ) {
				// top half: lower bound in (middle + 1) and insert position after middle
				insert = low = middle + 1;
			}
			else {
				return;
			}
		}
		final String[] newNames = new String[names.length + 1];
		System.arraycopy( names, 0, newNames, 0, insert);
		System.arraycopy( names, insert, newNames, insert + 1, names.length - insert);
		newNames[insert] = name;
		names = newNames;
	}

	public void clear() {
		names = new String[0];
	}

	public boolean isEmpty() {
		return names.length == 0;
	}

	public Set<String> asSet() {
		return new CopyOnWriteArraySet<String>( Arrays.asList( names ) );
	}

}
