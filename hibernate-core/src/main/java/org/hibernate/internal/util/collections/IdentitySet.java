/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Set implementation that use == instead of equals() as its comparison
 * mechanism.  This is achieved by internally using an IdentityHashMap.
 *
 * @author Emmanuel Bernard
 */
public class IdentitySet<E> implements Set<E> {
	private static final Object DUMP_VALUE = new Object();

	private final IdentityHashMap<E,Object> map;

	/**
	 * Create an IdentitySet with default sizing.
	 */
	public IdentitySet() {
		this.map = new IdentityHashMap<>();
	}

	/**
	 * Create an IdentitySet with the given sizing.
	 *
	 * @param sizing The sizing of the set to create.
	 */
	@SuppressWarnings("unused")
	public IdentitySet(int sizing) {
		this.map = new IdentityHashMap<>( sizing );
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public boolean contains(Object o) {
		return map.get( o ) == DUMP_VALUE;
	}

	@Override
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public Object[] toArray() {
		return map.keySet().toArray();
	}

	@SuppressWarnings("SuspiciousToArrayCall")
	@Override
	public <T> T[] toArray(T[] a) {
		return map.keySet().toArray( a );
	}

	@Override
	public boolean add(E o) {
		return map.put( o, DUMP_VALUE ) == null;
	}

	@Override
	public boolean remove(Object o) {
		return map.remove( o ) == DUMP_VALUE;
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public boolean containsAll(Collection<?> checkValues) {
		for ( Object checkValue : checkValues ) {
			if ( ! map.containsKey( checkValue ) ) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> additions) {
		boolean changed = false;

		for ( E addition : additions ) {
			changed = add( addition ) || changed;
		}

		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> keepers) {
		//doable if needed
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> removals) {
		boolean changed = false;

		for ( Object removal : removals ) {
			changed = remove( removal ) || changed;
		}

		return changed;
	}

	@Override
	public void clear() {
		map.clear();
	}
}
