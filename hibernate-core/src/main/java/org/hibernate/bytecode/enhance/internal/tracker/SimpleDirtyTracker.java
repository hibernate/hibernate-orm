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
 * uses an array as a set (under the assumption that the number of elements will be low) to avoid having to instantiate an HashSet.
 * if the assumption does not, hold the array can be kept ordered to reduce the cost of verifying duplicates
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public final class SimpleDirtyTracker {

	private String[] names;

	public SimpleDirtyTracker() {
		names = new String[0];
	}

	public void add(String name) {
		for (String existing : names) {
			if ( existing.equals( name ) ) {
				return;
			}
		}
		names = Arrays.copyOf( names, names.length + 1 );
		names[names.length - 1] = name;
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
