/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.spi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.action.spi.Executable;

/**
 * Specialized encapsulating of the state pertaining to each Executable list.
 * <p/>
 * Lazily sorts the list and caches the sorted state.
 * <p/>
 * Lazily calculates the querySpaces affected by the actions in the list, and caches this too.
 *
 * @author Steve Ebersole
 * @author Anton Marsden
 *
 * @param <E> Intersection type describing Executable implementations
 */
@SuppressWarnings("rawtypes")
public class ExecutableList<E extends Executable & Comparable & Serializable> implements Serializable, Iterable<E>, Externalizable {

	public static final int INIT_QUEUE_LIST_SIZE = 5;

	/**
	 * Provides a sorting interface for ExecutableList.
	 * 
	 * @param <E>
	 */
	public static interface Sorter<E extends Executable> {

		/**
		 * Sorts the list.
		 */
		void sort(List<E> l);
	}

	private final ArrayList<E> executables;

	private final Sorter<E> sorter;
	private boolean sorted;

	private transient Set<Serializable> querySpaces;

	/**
	 * Creates a new ExecutableList.
	 */
	public ExecutableList() {
		this( null );
	}

	/**
	 * Creates a new ExecutableList using the specified Sorter.
	 * 
	 * @param sorter The Sorter to use; may be {@code null}
	 */
	public ExecutableList(ExecutableList.Sorter<E> sorter) {
		this( INIT_QUEUE_LIST_SIZE, sorter );
	}

	/**
	 * Creates a new ExecutableList with the specified initialCapacity.
	 * 
	 * @param initialCapacity The initial capacity for instantiating the internal List
	 */
	ExecutableList(int initialCapacity) {
		this( initialCapacity, null );
	}

	/**
	 * Creates a new ExecutableList with the specified initialCapacity and Sorter.
	 *
	 * @param initialCapacity The initial capacity for instantiating the internal List
	 * @param sorter The Sorter to use; may be {@code null}
	 */
	ExecutableList(int initialCapacity, ExecutableList.Sorter<E> sorter) {
		this.sorter = sorter;
		this.executables = new ArrayList<E>( initialCapacity );
		// a non-null querySpaces value would add to the querySpaces as the list is added to,
		// but we would like this data to be lazily initialized.
		this.querySpaces = null;
		this.sorted = true;
	}

	/**
	 * @return true if the list is empty.
	 */
	public boolean isEmpty() {
		return executables.isEmpty();
	}

	/**
	 * Removes the entry at position index in the list.
	 * 
	 * @param index The index of the element to remove
	 *
	 * @return the entry that was removed
	 */
	public E remove(int index) {
		if ( index < executables.size() - 1 ) {
			sorted = false;
		}

		final E e = executables.remove( index );

		// clear the querySpaces cache if the removed Executable had property querySpaces
		if ( e.getPropertySpaces() != null && e.getPropertySpaces().length > 0 ) {
			querySpaces = null;
		}
		return e;
	}

	/**
	 * Clears the list of executions.
	 */
	public void clear() {
		executables.clear();
		querySpaces = null;
		sorted = true;
	}

	/**
	 * Removes the last n entries from the list.
	 * 
	 * @param n The number of elements to remove.
	 */
	public void removeLastN(int n) {
		if ( n > 0 ) {
			int size = executables.size();
			for ( Executable e : executables.subList( size - n, size ) ) {
				if ( e.getPropertySpaces() != null && e.getPropertySpaces().length > 0 ) {
					// querySpaces could now be incorrect
					querySpaces = null;
					break;
				}
			}
			executables.subList( size - n, size ).clear();
		}
	}

	/**
	 * Lazily constructs the querySpaces affected by the actions in the list.
	 * 
	 * @return the querySpaces affected by the actions in this list
	 */
	public Set<Serializable> getQuerySpaces() {
		if ( querySpaces == null ) {
			querySpaces = new HashSet<Serializable>();
			for ( E e : executables ) {
				Serializable[] propertySpaces = e.getPropertySpaces();
				if ( querySpaces != null && propertySpaces != null ) {
					Collections.addAll( querySpaces, propertySpaces );
				}
			}
		}
		return querySpaces;
	}

	/**
	 * Add an Executable to this list.
	 * 
	 * @param o the executable to add to the list
	 * @return true if the object was added to the list
	 */
	public boolean add(E o) {
		boolean added = executables.add( o );
		if ( added ) {
			// no longer sorted
			sorted = false;
			Serializable[] propertySpaces = o.getPropertySpaces();
			// we can cheaply keep querySpaces in sync once they are cached
			if ( querySpaces != null && propertySpaces != null ) {
				Collections.addAll( querySpaces, propertySpaces );
			}
		}
		return added;
	}

	/**
	 * Sorts the list using the natural ordering or using the Sorter if it's not null.
	 */
	@SuppressWarnings("unchecked")
	public void sort() {
		if ( sorted ) {
			return;
		}

		if ( sorter != null ) {
			sorter.sort( executables );
		}
		else {
			Collections.sort( executables );
		}
		sorted = true;
	}

	/**
	 * @return the current size of the list
	 */
	public int size() {
		return executables.size();
	}

	/**
	 * @param index The index of the element to retrieve
	 *
	 * @return The element at specified index
	 */
	public E get(int index) {
		return executables.get( index );
	}

	/**
	 * Returns an iterator for the list. Wraps the list just in case something tries to modify it.
	 * 
	 * @return an unmodifiable iterator
	 */
	@Override
	public Iterator<E> iterator() {
		return Collections.unmodifiableList( executables ).iterator();
	}

	/**
	 * Serializes the list out to oos.
	 * 
	 * @param oos The stream to which to serialize our state
	 */
	@Override
	public void writeExternal(ObjectOutput oos) throws IOException {
		oos.writeInt( executables.size() );
		for ( E e : executables ) {
			oos.writeObject( e );
		}
	}

	/**
	 * De-serializes the list into this object from in.
	 * 
	 * @param in The stream from which to read our serial state
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sorted = false;
		querySpaces = null;
		int size = in.readInt();
		executables.ensureCapacity( size );
		if ( size > 0 ) {
			for ( int i = 0; i < size; i++ ) {
				E e = (E) in.readObject();
				executables.add( e );
			}
		}
	}

	/**
	 * Re-attaches the Executable elements to the session after deserialization.
	 * 
	 * @param session The session to which to attach the Executable elements
	 */
	public void afterDeserialize(SessionImplementor session) {
		for ( E e : executables ) {
			e.afterDeserialize( session );
		}
	}

}
