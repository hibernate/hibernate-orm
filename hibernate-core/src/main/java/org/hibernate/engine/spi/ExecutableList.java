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
 * Encapsulates state relating to each executable list. Lazily sorts the list and caches the sorted state. Lazily
 * calculates the spaces affected by the actions in the list, and caches this too.
 * 
 * @author Anton Marsden
 * @param <E>
 */
@SuppressWarnings("rawtypes")
public class ExecutableList<E extends Executable & Comparable & Serializable> implements Serializable, Iterable<E>, Externalizable {

	/**
	 * Provides a sorting interface for ExecutableList.
	 * 
	 * @author Anton Marsden
	 * @param <E>
	 */
	public interface Sorter<E extends Executable> {

		/**
		 * Sorts the list.
		 */
		void sort(List<E> l);
	}

	private final ExecutableList.Sorter<E> sorter;

	private final ArrayList<E> executables;

	private boolean sorted;

	private transient Set<Serializable> spaces;

	/**
	 * Creates a new ExecutableList.
	 */
	public ExecutableList() {
		this( null );
	}

	/**
	 * Creates a new ExecutableList using the specified Sorter.
	 * 
	 * @param sorter
	 */
	public ExecutableList(ExecutableList.Sorter<E> sorter) {
		this( 10, sorter ); // use the standard ArrayList initialCapacity
	}

	/**
	 * Creates a new ExecutableList with the specified initialCapacity.
	 * 
	 * @param initialCapacity
	 */
	ExecutableList(int initialCapacity) {
		this( initialCapacity, null );
	}

	/**
	 * Creates a new ExecutableList with the specified initialCapacity and Sorter.
	 * 
	 * @param initialCapacity
	 * @param sorter
	 */
	ExecutableList(int initialCapacity, ExecutableList.Sorter<E> sorter) {
		this.sorter = sorter;
		this.executables = new ArrayList<E>( initialCapacity );
		// a non-null spaces value would add to the spaces as the list is added to,
		// but we would like this data to be lazily initialized.
		this.spaces = null;
		this.sorted = true;
	}

	/**
	 * @return true if the list is empty.
	 */
	public boolean isEmpty() {
		return executables.isEmpty();
	}

	/**
	 * Removes the entry at position idx in the list.
	 * 
	 * @param idx
	 * @return the entry that was removed
	 */
	public E remove(int idx) {

		if ( idx < executables.size() - 1 ) {
			sorted = false;
		}
		E e = executables.remove( idx );
		// clear the spaces cache if the removed Executable had property spaces
		if ( e.getPropertySpaces() != null && e.getPropertySpaces().length > 0 ) {
			spaces = null;
		}
		return e;
	}

	/**
	 * Clears the list of executions.
	 */
	public void clear() {
		// Note: another option here is to replace the list with a new one
		executables.clear();
		spaces = null;
		sorted = true;
	}

	/**
	 * Removes the last n entries from the list.
	 * 
	 * @param n
	 */
	public void removeLastN(int n) {
		if ( n > 0 ) {
			int size = executables.size();
			for ( Executable e : executables.subList( size - n, size ) ) {
				if ( e.getPropertySpaces() != null && e.getPropertySpaces().length > 0 ) {
					// spaces could now be incorrect
					spaces = null;
					break;
				}
			}
			executables.subList( size - n, size ).clear();
		}
	}

	/**
	 * Lazily constructs the spaces affected by the actions in the list.
	 * 
	 * @return the spaces affected by the actions in this list
	 */
	public Set<Serializable> getPropertySpaces() {
		if ( spaces == null ) {
			spaces = new HashSet<Serializable>();
			for ( E e : executables ) {
				Serializable[] propertySpaces = e.getPropertySpaces();
				if ( spaces != null && propertySpaces != null ) {
					for ( Serializable s : propertySpaces ) {
						spaces.add( s );
					}
				}
			}
		}
		return spaces;
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
			// we can cheaply keep spaces in sync once they are cached
			if ( spaces != null && propertySpaces != null ) {
				for ( Serializable s : propertySpaces ) {
					spaces.add( s );
				}
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
	 * @param idx
	 * @return the element at index idx
	 */
	public E get(int idx) {
		return executables.get( idx );
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
	 * @param oos 
	 */
	@Override
	public void writeExternal(ObjectOutput oos) throws IOException {
		oos.writeInt( executables.size() );
		for ( E e : executables ) {
			oos.writeObject( e );
		}
	}

	/**
	 * Deserializes the list into this object from in.
	 * 
	 * @param in
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sorted = false;
		spaces = null;
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
	 * Re-attaches the executables to the session after deserialization.
	 * 
	 * @param session
	 */
	public void afterDeserialize(SessionImplementor session) {
		for ( E e : executables ) {
			e.afterDeserialize( session );
		}
	}

}
