/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.tracker;

import java.util.Arrays;

/**
 * small low memory class to keep track of changed fields
 * <p/>
 * uses an array as a set (under the assumption that the number of elements will be low) to avoid having to instantiate an HashSet.
 * if the assumption does not, hold the array can be kept ordered to reduce the cost of verifying duplicates
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public final class SimpleFieldTracker implements DirtyTracker {

	private String[] names;
	private boolean suspended;

	public SimpleFieldTracker() {
		names = new String[0];
	}

	@Override
	public void add(String name) {
		if ( suspended ) {
			return;
		}
		if ( !contains( name ) ) {
			names = Arrays.copyOf( names, names.length + 1 );
			names[names.length - 1] = name;
		}
	}

	@Override
	public boolean contains(String name) {
		for ( String existing : names ) {
			if ( existing.equals( name ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void clear() {
		if ( !isEmpty() ) {
			names = new String[0];
		}
	}

	@Override
	public boolean isEmpty() {
		return names.length == 0;
	}

	@Override
	public String[] get() {
		return names;
	}

	@Override
	public void suspend(boolean suspend) {
		this.suspended = suspend;
	}

}
