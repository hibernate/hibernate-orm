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
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * Specialized encapsulating of the state pertaining to each Executable list.
 * <p/>
 * Manages sorting the executables (lazily)
 * <p/>
 * Manages the querySpaces affected by the executables in the list, and caches this too.
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

	/**
	 * Used to hold the query spaces (table names, roughly) that all the Executable instances contained
	 * in this list define.  This information is ultimately used to invalidate cache regions as it is
	 * exposed from {@link #getQuerySpaces}.  This value being {@code null} indicates that the
	 * query spaces should be calculated.
	 */
	private transient Set<Serializable> querySpaces;

	/**
	 * Creates a new ExecutableList with the default settings.
	 */
	public ExecutableList() {
		this( INIT_QUEUE_LIST_SIZE );
	}

	/**
	 * Creates a new ExecutableList with the specified initialCapacity.
	 *
	 * @param initialCapacity The initial capacity for instantiating the internal List
	 */
	public ExecutableList(int initialCapacity) {
		this( initialCapacity, null );
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
	 * Creates a new ExecutableList with the specified initialCapacity and Sorter.
	 *
	 * @param initialCapacity The initial capacity for instantiating the internal List
	 * @param sorter The Sorter to use; may be {@code null}
	 */
	public ExecutableList(int initialCapacity, ExecutableList.Sorter<E> sorter) {
		this.sorter = sorter;
		this.executables = new ArrayList<E>( initialCapacity );
		this.querySpaces = new HashSet<Serializable>();
		this.sorted = true;
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
		// removals are generally safe in regards to sorting...

		final E e = executables.remove( index );

		// If the executable being removed defined query spaces we need to recalculate the overall query spaces for
		// this list.  The problem is that we don't know how many other executable instances in the list also
		// contributed those query spaces as well.
		//
		// An alternative here is to use a "multiset" which is a specialized set that keeps a reference count
		// associated to each entry.  But that is likely overkill here.
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
	 * Add an Executable to this list.
	 * 
	 * @param executable the executable to add to the list
	 *
	 * @return true if the object was added to the list
	 */
	public boolean add(E executable) {
		final E previousLast = sorter != null || executables.isEmpty() ? null : executables.get( executables.size() - 1 );
		boolean added = executables.add( executable );

		if ( !added ) {
			return false;
		}

		// see if the addition invalidated the sorting
		if ( sorter != null ) {
			// we don't have intrinsic insight into the sorter's algorithm, so invalidate sorting
			sorted = false;
		}
		else {
			// otherwise, we added to the end of the list.  So check the comparison between the incoming
			// executable and the one previously at the end of the list using the Comparable contract
			if ( previousLast != null && previousLast.compareTo( executable ) > 0 ) {
				sorted = false;
			}
		}

		Serializable[] querySpaces = executable.getPropertySpaces();
		if ( this.querySpaces != null && querySpaces != null ) {
			Collections.addAll( this.querySpaces, querySpaces );
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
	 * Write this list out to the given stream as part of serialization
	 * 
	 * @param oos The stream to which to serialize our state
	 */
	@Override
	public void writeExternal(ObjectOutput oos) throws IOException {
		oos.writeBoolean( sorted );

		oos.writeInt( executables.size() );
		for ( E e : executables ) {
			oos.writeObject( e );
		}

		// if the spaces are initialized, write them out for usage after deserialization
		if ( querySpaces == null ) {
			oos.writeInt( -1 );
		}
		else {
			oos.writeInt( querySpaces.size() );
			// these are always String, why we treat them as Serializable instead is beyond me...
			for ( Serializable querySpace : querySpaces ) {
				oos.writeUTF( querySpace.toString() );
			}
		}
	}

	/**
	 * Read this object state back in from the given stream as part of de-serialization
	 * 
	 * @param in The stream from which to read our serial state
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sorted = in.readBoolean();

		final int numberOfExecutables = in.readInt();
		executables.ensureCapacity( numberOfExecutables );
		if ( numberOfExecutables > 0 ) {
			for ( int i = 0; i < numberOfExecutables; i++ ) {
				E e = (E) in.readObject();
				executables.add( e );
			}
		}

		final int numberOfQuerySpaces = in.readInt();
		if ( numberOfQuerySpaces < 0 ) {
			this.querySpaces = null;
		}
		else {
			querySpaces = new HashSet<Serializable>( CollectionHelper.determineProperSizing( numberOfQuerySpaces ) );
			for ( int i = 0; i < numberOfQuerySpaces; i++ ) {
				querySpaces.add( in.readUTF() );
			}
		}
	}

	/**
	 * Allow the Executables to re-associate themselves with the Session after deserialization.
	 * 
	 * @param session The session to which to associate the Executables
	 */
	public void afterDeserialize(SessionImplementor session) {
		for ( E e : executables ) {
			e.afterDeserialize( session );
		}
	}

}
