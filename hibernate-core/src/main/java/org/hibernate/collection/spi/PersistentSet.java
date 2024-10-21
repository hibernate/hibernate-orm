/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;


/**
 * A persistent wrapper for a {@link java.util.Set}. The underlying
 * collection is a {@code HashSet}.
 *
 * @apiNote Incubating in terms of making this non-internal.
 *          These contracts will be getting cleaned up in following
 *          releases.
 *
 * @author Gavin King
 */
@Incubating
public class PersistentSet<E> extends AbstractPersistentCollection<E> implements Set<E> {
	protected Set<E> set;

	/**
	 * Empty constructor.
	 * <p>
	 * Note: this form is not ever ever ever used by Hibernate; it is, however,
	 * needed for SOAP libraries and other such marshalling code.
	 */
	public PersistentSet() {
		// intentionally empty
	}

	/**
	 * Constructor matching super.  Instantiates a lazy set (the underlying
	 * set is un-initialized).
	 *
	 * @param session The session to which this set will belong.
	 */
	public PersistentSet(SharedSessionContractImplementor session) {
		super( session );
	}

	/**
	 * Instantiates a non-lazy set (the underlying set is constructed
	 * from the incoming set reference).
	 *
	 * @param session The session to which this set will belong.
	 * @param set The underlying set data.
	 */
	public PersistentSet(SharedSessionContractImplementor session, Set<E> set) {
		super( session );
		// Sets can be just a view of a part of another collection.
		// do we need to copy it to be sure it won't be changing
		// underneath us?
		// ie. this.set.addAll(set);
		this.set = set;
		setInitialized();
		setDirectlyAccessible( true );
	}

	@Override
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		final HashMap<E,E> clonedSet = CollectionHelper.mapOfSize( set.size() );
		for ( E aSet : set ) {
			final E copied = (E) persister.getElementType().deepCopy( aSet, persister.getFactory() );
			clonedSet.put( copied, copied );
		}
		return clonedSet;
	}

	@Override
	public Collection<E> getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		final java.util.Map<E,E> sn = (java.util.Map<E,E>) snapshot;
		return getOrphans( sn.keySet(), set, entityName, getSession() );
	}

	@Override
	public void initializeEmptyCollection(CollectionPersister persister) {
		assert set == null;
		//noinspection unchecked
		set = (Set<E>) persister.getCollectionSemantics().instantiateRaw( 0, persister );
		endRead();
	}

	@Override
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final java.util.Map<?,?> sn = (java.util.Map<?,?>) getSnapshot();
		if ( sn.size()!=set.size() ) {
			return false;
		}
		else {
			for ( Object test : set ) {
				final Object oldValue = sn.get( test );
				if ( oldValue == null || elementType.isDirty( oldValue, test, getSession() ) ) {
					return false;
				}
			}
			return true;
		}
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (java.util.Map<?,?>) snapshot ).isEmpty();
	}

	@Override
	public void initializeFromCache(CollectionPersister persister, Object disassembled, Object owner)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;

		this.set = (Set<E>) persister.getCollectionSemantics().instantiateRaw( size, persister );

		for ( Serializable arrayElement : array ) {
			final Object assembledArrayElement = persister.getElementType().assemble( arrayElement, getSession(), owner );
			if ( assembledArrayElement != null ) {
				set.add( (E) assembledArrayElement );
			}
		}
	}

	@Override
	public boolean empty() {
		return set.isEmpty();
	}

	@Override
	public int size() {
		return readSize() ? getCachedSize() : set.size();
	}

	@Override
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : set.isEmpty();
	}

	@Override
	public boolean contains(Object object) {
		final Boolean exists = readElementExistence( object );
		return exists == null
				? set.contains( object )
				: exists;
	}

	@Override
	public Iterator<E> iterator() {
		read();
		return new IteratorProxy<>( set.iterator() );
	}

	@Override
	public Object[] toArray() {
		read();
		return set.toArray();
	}

	@Override
	public <A> A[] toArray(A[] array) {
		read();
		return set.toArray( array );
	}

	@Override
	public boolean add(E value) {
		final Boolean exists = isOperationQueueEnabled() ? readElementExistence( value ) : null;
		if ( exists == null ) {
			initialize( true );
			if ( set.add( value ) ) {
				dirty();
				return true;
			}
			else {
				return false;
			}
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
	public boolean remove(Object value) {
		final Boolean exists = isPutQueueEnabled() ? readElementExistence( value ) : null;
		if ( exists == null ) {
			initialize( true );
			if ( set.remove( value ) ) {
				elementRemoved = true;
				dirty();
				return true;
			}
			else {
				return false;
			}
		}
		else if ( exists ) {
			elementRemoved = true;
			queueOperation( new SimpleRemove( (E) value ) );
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void queueRemoveOperation(Object o) {
		if ( !isInitialized() ) {
			final DelayedOperation operation = new DelayedOperation() {
				@Override
				public void operate() {
					remove( o );
				}

				@Override
				public Object getAddedInstance() {
					return null;
				}

				@Override
				public Object getOrphan() {
					return null;
				}
			};
			queueOperation( operation );
		}
		else {
			remove( o );
		}
	}

	@Override
	public void queueAddOperation(E o){
		if ( !isInitialized() ) {
			final DelayedOperation operation = new DelayedOperation() {
				@Override
				public void operate() {
					add( o );
				}

				@Override
				public Object getAddedInstance() {
					return null;
				}

				@Override
				public Object getOrphan() {
					return null;
				}
			};
			queueOperation( operation );
		}
		else {
			add( o );
		}
	}

	@Override
	public boolean containsAll(Collection<?> coll) {
		read();
		return set.containsAll( coll );
	}

	@Override
	public boolean addAll(Collection<? extends E> coll) {
		if ( coll.size() > 0 ) {
			initialize( true );
			if ( set.addAll( coll ) ) {
				dirty();
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}

	@Override
	public boolean retainAll(Collection<?> coll) {
		initialize( true );
		if ( set.retainAll( coll ) ) {
			dirty();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> coll) {
		if ( coll.size() > 0 ) {
			initialize( true );
			if ( set.removeAll( coll ) ) {
				elementRemoved = true;
				dirty();
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}

	@Override
	public void clear() {
		if ( isClearQueueEnabled() ) {
			queueOperation( new Clear() );
		}
		else {
			initialize( true );
			if ( !set.isEmpty() ) {
				set.clear();
				dirty();
			}
		}
	}

	@Override
	public String toString() {
		read();
		return set.toString();
	}

	public void injectLoadedState(
			PluralAttributeMapping attributeMapping,
			List<?> loadingStateList) {
		final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();
		if ( loadingStateList != null ) {
			this.set = (Set<E>) attributeMapping.getCollectionDescriptor().getCollectionSemantics().instantiateRaw(
					loadingStateList.size(),
					collectionDescriptor
			);

			this.set.addAll( (List<E>) loadingStateList );
		}
		else {
			this.set = (Set<E>) attributeMapping.getCollectionDescriptor().getCollectionSemantics().instantiateRaw(
					0,
					collectionDescriptor
			);
		}
	}

	@Override
	public Iterator<E> entries(CollectionPersister persister) {
		return set.iterator();
	}

	@Override
	public Object disassemble(CollectionPersister persister) throws HibernateException {
		final Serializable[] result = new Serializable[ set.size() ];
		final Iterator<E> itr = set.iterator();
		int i=0;
		while ( itr.hasNext() ) {
			result[i++] = persister.getElementType().disassemble( itr.next(), getSession(), null );
		}
		return result;
	}

	@Override
	public Iterator<?> getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final Type elementType = persister.getElementType();
		final java.util.Map<?,?> sn = (java.util.Map<?,?>) getSnapshot();
		final ArrayList<Object> deletes = new ArrayList<>( sn.size() );

		Iterator<?> itr = sn.keySet().iterator();
		while ( itr.hasNext() ) {
			final Object test = itr.next();
			if ( !set.contains( test ) ) {
				// the element has been removed from the set
				deletes.add( test );
			}
		}

		itr = set.iterator();
		while ( itr.hasNext() ) {
			final Object test = itr.next();
			final Object oldValue = sn.get( test );
			if ( oldValue!=null && elementType.isDirty( test, oldValue, getSession() ) ) {
				// the element has changed
				deletes.add( oldValue );
			}
		}

		return deletes.iterator();
	}

	@Override
	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		final Object oldValue = ( (java.util.Map<?,?>) getSnapshot() ).get( entry );
		// note that it might be better to iterate the snapshot but this is safe,
		// assuming the user implements equals() properly, as required by the Set
		// contract!
		return oldValue == null && entry != null
			|| elemType.isDirty( oldValue, entry, getSession() );
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elemType) {
		return false;
	}

	@Override
	public boolean isRowUpdatePossible() {
		return false;
	}

	@Override
	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		throw new UnsupportedOperationException("Sets don't have indexes");
	}

	@Override
	public Object getElement(Object entry) {
		return entry;
	}

	@Override
	public Object getSnapshotElement(Object entry, int i) {
		throw new UnsupportedOperationException("Sets don't support updating by element");
	}

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object other) {
		read();
		return set.equals( other );
	}

	@Override
	public int hashCode() {
		read();
		return set.hashCode();
	}

	@Override
	public boolean entryExists(Object key, int i) {
		return key != null;
	}

	@Override
	public boolean isWrapper(Object collection) {
		return set==collection;
	}

	final class Clear implements DelayedOperation<E> {
		@Override
		public void operate() {
			set.clear();
		}

		@Override
		public E getAddedInstance() {
			return null;
		}

		@Override
		public E getOrphan() {
			throw new UnsupportedOperationException("queued clear cannot be used with orphan delete");
		}
	}

	final class SimpleAdd extends AbstractValueDelayedOperation {

		public SimpleAdd(E addedValue) {
			super( addedValue, null );
		}

		@Override
		public void operate() {
			set.add( getAddedInstance() );
		}
	}

	final class SimpleRemove extends AbstractValueDelayedOperation {

		public SimpleRemove(E orphan) {
			super( null, orphan );
		}

		@Override
		public void operate() {
			set.remove( getOrphan() );
		}
	}
}
