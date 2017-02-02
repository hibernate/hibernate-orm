/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.tracker;

/**
 * small low memory class to keep track of changed fields
 *
 * similar to BasicTracker but where the array is kept ordered to reduce the cost of verifying duplicates
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public final class SortedFieldTracker implements DirtyTracker {

	private String[] names;
	private boolean suspended;

	public SortedFieldTracker() {
		names = new String[0];
	}

	@Override
	public void add(String name) {
		if ( suspended ) {
			return;
		}
		// we do a binary search: even if we don't find the name at least we get the position to insert into the array
		int insert = 0;
		for ( int low = 0, high = names.length - 1; low <= high; ) {
			final int middle = low + ( ( high - low ) / 2 );
			final int compare = names[middle].compareTo( name );
			if ( compare > 0 ) {
				// bottom half: higher bound in (middle - 1) and insert position in middle
				high = middle - 1;
				insert = middle;
			}
			else if( compare < 0 ) {
				// top half: lower bound in (middle + 1) and insert position afterQuery middle
				insert = low = middle + 1;
			}
			else {
				return;
			}
		}
		final String[] newNames = new String[names.length + 1];
		System.arraycopy( names, 0, newNames, 0, insert );
		System.arraycopy( names, insert, newNames, insert + 1, names.length - insert );
		newNames[insert] = name;
		names = newNames;
	}

	@Override
	public boolean contains(String name) {
		for ( int low = 0, high = names.length - 1; low <= high; ) {
			final int middle = low + ( ( high - low ) / 2 );
			final int compare = names[middle].compareTo( name );
			if ( compare > 0 ) {
				// bottom half: higher bound in (middle - 1) and insert position in middle
				high = middle - 1;
			}
			else if( compare < 0 ) {
				// top half: lower bound in (middle + 1) and insert position afterQuery middle
				low = middle + 1;
			}
			else {
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
