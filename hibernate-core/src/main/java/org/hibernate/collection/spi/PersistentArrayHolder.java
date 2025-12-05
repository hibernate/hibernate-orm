/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Array.set;
import static java.util.Collections.addAll;

/**
 * A dummy collection wrapper for an array. Lazy initialization is
 * <em>not</em> supported. The use of arrays to represent persistent
 * collections in Hibernate is discouraged.
 *
 * @apiNote Incubating in terms of making this non-internal.
 *          These contracts will be getting cleaned up in following
 *          releases.
 *
 * @author Gavin King
 */
@Incubating
@AllowReflection // We need the ability to create arrays of the same type as in the model.
public class PersistentArrayHolder<E> extends AbstractPersistentCollection<E> {

	protected Object array;

	//just to help out during the load (ugly, i know)
	private transient Class<?> elementClass;

	/**
	 * Constructs a PersistentCollection instance for holding an array.
	 *
	 * @param session The session
	 * @param array The array (the persistent "collection").
	 */
	public PersistentArrayHolder(SharedSessionContractImplementor session, Object array) {
		super( session );
		this.array = array;
		setInitialized();
	}

	/**
	 * Constructs a PersistentCollection instance for holding an array.
	 *
	 * @param session The session
	 * @param persister The persister for the array
	 */
	public PersistentArrayHolder(SharedSessionContractImplementor session, CollectionPersister persister) {
		super( session );
		elementClass = persister.getElementClass();
	}

	@Override
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
//		final int length = (array==null) ? tempList.size() : Array.getLength( array );
		final int length = getLength( array );
		final var result = (Serializable) newInstance( persister.getElementClass(), length );
		for ( int i=0; i<length; i++ ) {
//			final Object elt = (array==null) ? tempList.get( i ) : Array.get( array, i );
			final Object elt = get( array, i );
			try {
				set( result, i, persister.getElementType().deepCopy( elt, persister.getFactory() ) );
			}
			catch (IllegalArgumentException iae) {
				throw new HibernateException( "Array element type error", iae );
			}
		}
		return result;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return getLength( snapshot ) == 0;
	}

	@Override
	public Collection<E> getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		//noinspection unchecked
		final E[] sn = (E[]) snapshot;
		final Object[] arr = (Object[]) array;
		if ( arr.length == 0 ) {
			return Arrays.asList( sn );
		}
		else {
			final ArrayList<E> result = new ArrayList<>();
			addAll( result, sn );
			for ( int i = 0; i < sn.length; i++ ) {
				identityRemove( result, arr[i], entityName, getSession() );
			}
			return result;
		}
	}

	@Override
	public void initializeEmptyCollection(CollectionPersister persister) {
		assert array == null;
		array = newInstance( persister.getElementClass(), 0 );
		persister.getAttributeMapping().getPropertyAccess().getSetter().set( getOwner(), array );
		endRead();
	}

	@Override
	public void injectLoadedState(PluralAttributeMapping attributeMapping, List loadingState) {
		assert isInitializing();
		if ( loadingState == null ) {
			array = newInstance( elementClass, 0 );
		}
		else {
			array = newInstance( elementClass, loadingState.size() );
			for ( int i = 0; i < loadingState.size(); i++ ) {
				set( array, i, loadingState.get( i ) );
			}
		}
		attributeMapping.getPropertyAccess().getSetter().set( getOwner(), array );
	}

	@SuppressWarnings("unused")
	public Object getArray() {
		return array;
	}

	@Override
	public boolean isWrapper(Object collection) {
		return array==collection;
	}

	@Override
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final var elementType = persister.getElementType();
		final var snapshot = getSnapshot();
		final int length = getLength( snapshot );
		if ( length!= getLength( array ) ) {
			return false;
		}
		for ( int i=0; i<length; i++) {
			if ( elementType.isDirty( get( snapshot, i ), get( array, i ), getSession() ) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get an iterator over the array elements
	 *
	 * @return The iterator
	 */
	public Iterator<?> elements() {
		final int length = getLength( array );
		final List<Object> list = new ArrayList<>( length );
		for ( int i=0; i<length; i++ ) {
			list.add( get( array, i ) );
		}
		return list.iterator();
	}

	@Override
	public boolean empty() {
		return false;
	}

	@Override
	public Iterator<?> entries(CollectionPersister persister) {
		return elements();
	}

	@Override
	public boolean endRead() {
		setInitialized();
		return true;
	}

	@Override
	public boolean isDirectlyAccessible() {
		return true;
	}

	@Override
	public void initializeFromCache(CollectionPersister persister, Object disassembled, Object owner)
			throws HibernateException {
		final var cached = (Serializable[]) disassembled;
		array = newInstance( persister.getElementClass(), cached.length );
		for ( int i=0; i<cached.length; i++ ) {
			set( array, i, persister.getElementType().assemble( cached[i], getSession(), owner ) );
		}
	}

	@Override
	public Object disassemble(CollectionPersister persister) throws HibernateException {
		final int length = getLength( array );
		final var result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementType().disassemble( get( array,i ), getSession(), null );
		}
		return result;
	}

	@Override
	public Object getValue() {
		return array;
	}

	@Override
	public Iterator<?> getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final List<Integer> deletes = new ArrayList<>();
		final var sn = getSnapshot();
		final int length = getLength( sn );
		final int arraySize = getLength( array );
		final int end;
		if ( length > arraySize ) {
			for ( int i=arraySize; i<length; i++ ) {
				deletes.add( i );
			}
			end = arraySize;
		}
		else {
			end = length;
		}
		for ( int i=0; i<end; i++ ) {
			if ( get( array, i ) == null && get( sn, i ) != null ) {
				deletes.add( i );
			}
		}
		return deletes.iterator();
	}

	@Override
	public boolean hasDeletes(CollectionPersister persister) throws HibernateException {
		final var snapshot = getSnapshot();
		final int length = getLength( snapshot );
		final int arraySize = getLength( array );
		if ( length > arraySize ) {
			return true;
		}
		for ( int i=0; i<length; i++ ) {
			if ( get( array, i ) == null && get( snapshot, i ) != null ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		final var snapshot = getSnapshot();
		return get( array, i ) != null
			&& ( i >= getLength( snapshot ) || get( snapshot, i ) == null );
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {
		final var snapshot = getSnapshot();
		return i < getLength( snapshot )
			&& get( snapshot, i ) != null
			&& get( array, i ) != null
			&& elemType.isDirty( get( array, i ), get( snapshot, i ), getSession() );
	}

	@Override
	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		return i;
	}

	@Override
	public Object getElement(Object entry) {
		return entry;
	}

	@Override
	public Object getSnapshotElement(Object entry, int i) {
		final var snapshot = getSnapshot();
		return get( snapshot, i );
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry != null;
	}
}
