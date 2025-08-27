/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.semantics;

import java.util.Collection;
import java.util.List;

import org.hibernate.collection.spi.PersistentList;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * todo (6.0) : much of this is copy-paste from PersistentList
 * 		this is true for most attempts at a custom PersistentCollection;
 * 		we need to clean these up so it is easier to provide custom
 * 		implementations
 */
//tag::collections-custom-semantics-ex[]
public class UniqueListWrapper<E> extends PersistentList<E> {
	public UniqueListWrapper(SharedSessionContractImplementor session) {
		super( session );
	}

	public UniqueListWrapper(SharedSessionContractImplementor session, List<E> list) {
		super( session, list );
	}

	// ...
//end::collections-custom-semantics-ex[]

	@Override
	public boolean add(E element) {
		if ( element == null ) {
			// per java.util.List requirements
			throw new NullPointerException( "Passed collection cannot be null" );
		}

		final Boolean exists = isOperationQueueEnabled() ? readElementExistence( element ) : null;
		if ( exists == null ) {
			initialize( true );
			if ( getRawList().contains( element ) ) {
				// per java.util.List requirements
				throw new IllegalArgumentException( "Cannot add given element to unique List as it already existed" );
			}
			getRawList().add( element );
			dirty();
			return true;
		}
		else if ( exists ) {
			return false;
		}
		else {
			queueOperation( new SimpleAdd( element ) );
			return true;
		}
	}

	@Override
	public boolean addAll(Collection<? extends E> values) {
		if ( values == null ) {
			// per java.util.List requirements
			throw new NullPointerException( "Passed collection cannot be null" );
		}
		boolean changed = false;
		for ( E value : values ) {
			final boolean added = add( value );
			if ( ! added ) {
				// per java.util.List requirements
				throw new IllegalArgumentException( "Cannot add given element to unique List as it already existed" );
			}
			changed = changed || added;
		}
		return changed;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> values) {
		if ( values == null ) {
			// per java.util.List requirements
			throw new NullPointerException( "Passed collection cannot be null" );
		}

		return addAllStartingAt( index, values );
	}

	private boolean addAllStartingAt(int index, Collection<? extends E> values) {
		if ( values == null ) {
			// per java.util.List requirements
			throw new NullPointerException( "Passed collection cannot be null" );
		}

		boolean changed = false;
		int position = index;
		for ( E value : values ) {
			final boolean added = addAt( position++, value );
			changed = changed || added;
		}
		return changed;
	}

	private boolean addAt(int index, E value) {
		if ( value == null ) {
			// per java.util.List requirements
			throw new NullPointerException( "Passed collection cannot be null" );
		}

		final Boolean exists = isOperationQueueEnabled() ? readElementExistence( value ) : null;
		if ( exists == null ) {
			initialize( true );
			if ( getRawList().contains( value ) ) {
				return false;
			}
			getRawList().add( index, value );
			dirty();
			return true;
		}
		else if ( exists ) {
			return false;
		}
		else {
			queueOperation( new SimpleAdd( value ) );
			return true;
		}
	}

	@Override
	public E set(int index, E value) {
		initialize( true );
		return getRawList().set( index, value );
	}

	@Override
	public void add(int index, E value) {
		final boolean added = addAt( index, value );
		if ( !added ) {
			throw new IllegalArgumentException( "Cannot add given element to unique List as it already existed" );
		}
	}
//tag::collections-custom-semantics-ex[]
}
//end::collections-custom-semantics-ex[]
