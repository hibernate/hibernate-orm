/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.event.spi.EventSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.addAll;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;

/**
 * A list of {@link Executable executeble actions}. Responsible for
 * {@linkplain #sort() sorting} the executables, and calculating the
 * affected {@linkplain #getQuerySpaces() query spaces}.
 *
 * @author Steve Ebersole
 * @author Anton Marsden
 */
public class ExecutableList<E extends ComparableExecutable>
		implements Serializable, Iterable<E>, Externalizable {

	public static final int INIT_QUEUE_LIST_SIZE = 5;

	/**
	 * Provides a sorting interface for {@link ExecutableList}.
	 */
	public interface Sorter<ComparableExecutable> {
		/**
		 * Sorts the list.
		 */
		void sort(List<ComparableExecutable> l);
	}

	private final ArrayList<E> executables;

	private final @Nullable Sorter<E> sorter;
	private final boolean requiresSorting;
	private boolean sorted;

	/**
	 * Used to hold the query spaces (table names, roughly) that all the {@link Executable}
	 * instances contained in this list define. This information is ultimately used to
	 * invalidate cache regions as it is exposed from {@link #getQuerySpaces}. This value
	 * being {@code null} indicates that the query spaces should be calculated.
	 */
	private transient @Nullable Set<Serializable> querySpaces;

	/**
	 * Creates a new instance with the default settings.
	 */
	public ExecutableList() {
		this( INIT_QUEUE_LIST_SIZE );
	}

	/**
	 * Creates a new instance with the given initial capacity.
	 *
	 * @param initialCapacity The initial capacity for instantiating the internal List
	 */
	public ExecutableList(int initialCapacity) {
		// pass true for requiresSorting argument to maintain original behavior
		this( initialCapacity, true );
	}

	public ExecutableList(boolean requiresSorting) {
		this( INIT_QUEUE_LIST_SIZE, requiresSorting );
	}

	public ExecutableList(int initialCapacity, boolean requiresSorting) {
		this.sorter = null;
		this.executables = new ArrayList<>( initialCapacity );
		this.querySpaces = null;
		this.requiresSorting = requiresSorting;
		this.sorted = requiresSorting;
	}

	/**
	 * Creates a new instance using the given {@link Sorter}.
	 *
	 * @param sorter The Sorter to use; may be {@code null}
	 */
	public ExecutableList(Sorter<E> sorter) {
		this( INIT_QUEUE_LIST_SIZE, sorter );
	}

	/**
	 * Creates a new instance with the given initial capacity and {@link Sorter}.
	 *
	 * @param initialCapacity The initial capacity for instantiating the internal List
	 * @param sorter The Sorter to use; may be {@code null}
	 */
	public ExecutableList(int initialCapacity, Sorter<E> sorter) {
		this.sorter = sorter;
		this.executables = new ArrayList<>( initialCapacity );
		this.querySpaces = null;
		// require sorting by default, even if sorter is null to maintain original behavior
		this.requiresSorting = true;
		this.sorted = true;
	}

	/**
	 * Lazily constructs the querySpaces affected by the actions in the list.
	 *
	 * @return the querySpaces affected by the actions in this list
	 */
	public Set<Serializable> getQuerySpaces() {
		if ( querySpaces == null ) {
			for ( var executable : executables ) {
				final var propertySpaces = executable.getPropertySpaces();
				if ( propertySpaces != null && propertySpaces.length > 0 ) {
					if( querySpaces == null ) {
						querySpaces = new HashSet<>();
					}
					addAll( querySpaces, propertySpaces );
				}
			}
			if ( querySpaces == null ) {
				return emptySet();
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
	public ComparableExecutable remove(int index) {
		// removals are generally safe with regard to sorting
		final var executable = executables.remove( index );
		// If the executable being removed defined query spaces we need to recalculate the overall
		// query spaces for this list. The problem is that we don't know how many other executable
		// instances in the list also contributed those query spaces as well.
		//
		// An alternative here is to use a "multiset" which is a specialized set that keeps a
		// reference count associated with each entry. But that is likely overkill here.
		if ( isNotEmpty( executable.getPropertySpaces() ) ) {
			querySpaces = null;
		}
		return executable;
	}

	/**
	 * Clears the list of executions.
	 */
	public void clear() {
		executables.clear();
		querySpaces = null;
		sorted = requiresSorting;
	}

	/**
	 * Removes the last n entries from the list.
	 *
	 * @param n The number of elements to remove.
	 */
	public void removeLastN(int n) {
		if ( n > 0 ) {
			final int size = executables.size();
			for ( var executable : executables.subList( size - n, size ) ) {
				final var propertySpaces = executable.getPropertySpaces();
				if ( isNotEmpty( propertySpaces ) ) {
					// querySpaces could now be incorrect
					querySpaces = null;
					break;
				}
			}
			executables.subList( size - n, size ).clear();
		}
	}

	/**
	 * Add an {@link Executable} to this list.
	 *
	 * @param executable the executable to add to the list
	 *
	 * @return true if the object was added to the list
	 */
	public boolean add(E executable) {
		final var previousLast =
				sorter != null || executables.isEmpty()
						? null
						: executables.get( executables.size() - 1 );
		final boolean added = executables.add( executable );
		if ( !added ) {
			return false;
		}
		else {
			// if it was sorted before the addition, then check if the addition invalidated the sorting
			if ( sorted ) {
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
			}

			final var addedQuerySpaces = executable.getPropertySpaces();
			if ( querySpaces != null && addedQuerySpaces != null ) {
				addAll( querySpaces, addedQuerySpaces );
			}

			return true;
		}
	}

	/**
	 * Sorts the list using the natural ordering or using the {@link Sorter}
	 * if it's not null.
	 */
	public void sort() {
		if ( !sorted && requiresSorting ) {
			if ( sorter != null ) {
				sorter.sort( executables );
			}
			else {
				Collections.sort( executables );
			}
			sorted = true;
		}
		// else nothing to do
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
		return unmodifiableList( executables ).iterator();
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
		for ( ComparableExecutable e : executables ) {
			oos.writeObject( e );
		}

		// if the spaces are initialized, write them out for usage after deserialization
		if ( querySpaces == null ) {
			oos.writeInt( -1 );
		}
		else {
			final Set<Serializable> spaces = querySpaces;
			oos.writeInt( querySpaces.size() );
			// these are always String, why we treat them as Serializable instead is beyond me...
			for ( var querySpace : spaces ) {
				oos.writeUTF( querySpace.toString() );
			}
		}
	}

	/**
	 * Read this object state back in from the given stream as part of
	 * the deserialization process.
	 *
	 * @param in The stream from which to read our serial state
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sorted = in.readBoolean();

		final int numberOfExecutables = in.readInt();
		executables.ensureCapacity( numberOfExecutables );
		if ( numberOfExecutables > 0 ) {
			for ( int i = 0; i < numberOfExecutables; i++ ) {
				@SuppressWarnings("unchecked")
				E e = (E) in.readObject();
				executables.add( e );
			}
		}

		final int numberOfQuerySpaces = in.readInt();
		if ( numberOfQuerySpaces < 0 ) {
			this.querySpaces = null;
		}
		else {
			// The line below is for CF nullness checking purposes.
			final Set<Serializable> querySpaces = setOfSize( numberOfQuerySpaces );
			for ( int i = 0; i < numberOfQuerySpaces; i++ ) {
				querySpaces.add( in.readUTF() );
			}
			this.querySpaces = querySpaces;
		}
	}

	/**
	 * Allow the {@link Executable}s to reassociate themselves with the
	 * session after deserialization.
	 *
	 * @param session The session with which to associate the {@code Executable}s
	 */
	public void afterDeserialize(EventSource session) {
		for ( var executable : executables ) {
			executable.afterDeserialize( session );
		}
	}

	public String toString() {
		return "ExecutableList{size=" + executables.size() + "}";
	}
}
