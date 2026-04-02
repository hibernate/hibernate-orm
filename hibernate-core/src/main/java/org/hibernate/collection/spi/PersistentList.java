/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.Optional.Defined;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * A persistent wrapper for a {@link java.util.List}. Underlying
 * collection is an {@code ArrayList}.
 *
 * @apiNote Incubating in terms of making this non-internal.
 *          These contracts will be getting cleaned up in following
 *          releases.
 *
 * @author Gavin King
 */
@Incubating
public class PersistentList<E> extends AbstractPersistentCollection<E> implements List<E> {
	protected List<E> list;

	/**
	 * Constructs a PersistentList.  This form needed for SOAP libraries, etc
	 */
	public PersistentList() {
	}

	/**
	 * Constructs a PersistentList.
	 *
	 * @param session The session
	 */
	public PersistentList(SharedSessionContractImplementor session) {
		super( session );
	}

	/**
	 * Constructs a PersistentList.
	 *
	 * @param session The session
	 * @param list The raw list
	 */
	public PersistentList(SharedSessionContractImplementor session, List<E> list) {
		super( session );
		this.list = list;
		setInitialized();
		setDirectlyAccessible( true );
	}

	protected List<E> getRawList() {
		return list;
	}

	@Override
	public Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		final ArrayList<Object> clonedList = new ArrayList<>( list.size() );
		for ( Object element : list ) {
			final Object deepCopy = persister.getElementType().deepCopy( element, persister.getFactory() );
			clonedList.add( deepCopy );
		}
		return clonedList;
	}

	@Override
	public Collection<E> getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		return getOrphans( (List<E>) snapshot, list, entityName, getSession() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initializeEmptyCollection(CollectionPersister persister) {
		assert list == null;
		//noinspection unchecked
		list = (List<E>) persister.getCollectionSemantics().instantiateRaw( 0, persister );
		endRead();
	}

	@Override
	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		final Type elementType = persister.getElementType();
		final List<?> sn = (List<?>) getSnapshot();
		if ( sn.size() != this.list.size() ) {
			return false;
		}
		final Iterator<?> itr = list.iterator();
		final Iterator<?> snapshotItr = sn.iterator();
		while ( itr.hasNext() ) {
			if ( elementType.isDirty( itr.next(), snapshotItr.next(), getSession() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return ( (Collection<?>) snapshot ).isEmpty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initializeFromCache(CollectionPersister persister, Object disassembled, Object owner)
			throws HibernateException {
		final Serializable[] array = (Serializable[]) disassembled;
		final int size = array.length;

		assert list == null;
		list = (List<E>) persister.getCollectionSemantics().instantiateRaw( size, persister );

		for ( Serializable arrayElement : array ) {
			list.add( (E) persister.getElementType().assemble( arrayElement, getSession(), owner ) );
		}
	}

	@Override
	public void injectLoadedState(PluralAttributeMapping attributeMapping, List<?> loadingStateList) {
		assert isInitializing();
		assert list == null;

		final var collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final var collectionSemantics = collectionDescriptor.getCollectionSemantics();

		//noinspection unchecked
		list = (List<E>) collectionSemantics.instantiateRaw( loadingStateList.size(), collectionDescriptor );
		//noinspection unchecked
		list.addAll( (List<E>) loadingStateList );
	}

	@Override
	public boolean isWrapper(Object collection) {
		return list==collection;
	}

	@Override
	public int size() {
		return readSize() ? getCachedSize() : list.size();
	}

	@Override
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : list.isEmpty();
	}

	@Override
	public boolean contains(Object object) {
		final Boolean exists = readElementExistence( object );
		return exists == null
				? list.contains( object )
				: exists;
	}

	@Override
	public Iterator<E> iterator() {
		read();
		return new IteratorProxy<>( list.iterator() );
	}

	@Override
	public Object[] toArray() {
		read();
		return list.toArray();
	}

	@Override
	public <A> A[] toArray(A[] array) {
		read();
		return list.toArray( array );
	}

	@Override
	public boolean add(E object) {
		if ( !isOperationQueueEnabled() ) {
			write();
			return list.add( object );
		}
		else {
			queueOperation( new SimpleAdd( object ) );
			return true;
		}
	}

	@Override
	public boolean remove(Object value) {
		final Boolean exists = isPutQueueEnabled() ? readElementExistence( value ) : null;
		if ( exists == null ) {
			initialize( true );
			if ( list.remove( value ) ) {
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
	public boolean containsAll(Collection<?> coll) {
		read();
		return list.containsAll( coll );
	}

	@Override
	public boolean addAll(Collection<? extends E> values) {
		if ( values.isEmpty() ) {
			return false;
		}
		if ( !isOperationQueueEnabled() ) {
			write();
			return list.addAll( values );
		}
		else {
			for ( E value : values ) {
				queueOperation( new SimpleAdd( value ) );
			}
			return !values.isEmpty();
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> coll) {
		if ( coll.size() > 0 ) {
			write();
			return list.addAll( index, coll );
		}
		else {
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> coll) {
		if ( coll.size() > 0 ) {
			initialize( true );
			if ( list.removeAll( coll ) ) {
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
	public boolean retainAll(Collection<?> coll) {
		initialize( true );
		if ( list.retainAll( coll ) ) {
			dirty();
			return true;
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
			if ( ! list.isEmpty() ) {
				list.clear();
				dirty();
			}
		}
	}

	@Override
	public E get(int index) {
		if ( index < 0 ) {
			throw new ArrayIndexOutOfBoundsException( "negative index" );
		}
		return readElementByIndex( index ).evaluate( () -> list.get( index ) );
	}

	@Override
	public E set(int index, E value) {
		if (index<0) {
			throw new ArrayIndexOutOfBoundsException("negative index");
		}
		if ( isPutQueueEnabled()
				&& readElementByIndex( index ) instanceof Defined<E> element ) {
			final E old = element.result();
			queueOperation( new Set( index, value, old ) );
			return old;
		}
		else {
			write();
			return list.set( index, value );
		}
	}

	@Override
	public E remove(int index) {
		if ( index < 0 ) {
			throw new ArrayIndexOutOfBoundsException( "negative index" );
		}
		elementRemoved = true;
		if ( isPutQueueEnabled()
				&& readElementByIndex( index ) instanceof Defined<E> element ) {
			final E old = element.result();
			queueOperation( new Remove( index, old ) );
			return old;
		}
		else {
			write();
			dirty();
			return list.remove( index );
		}
	}

	@Override
	public void add(int index, E value) {
		if ( index < 0 ) {
			throw new ArrayIndexOutOfBoundsException( "negative index" );
		}
		write();
		list.add( index, value );
	}

	@Override
	public int indexOf(Object value) {
		read();
		return list.indexOf( value );
	}

	@Override
	public int lastIndexOf(Object value) {
		read();
		return list.lastIndexOf( value );
	}

	@Override
	public ListIterator<E> listIterator() {
		read();
		return new ListIteratorProxy( list.listIterator() );
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		read();
		return new ListIteratorProxy( list.listIterator( index ) );
	}

	@Override
	public List<E> subList(int from, int to) {
		read();
		return new ListProxy( list.subList( from, to ) );
	}

	@Override
	public boolean empty() {
		return list.isEmpty();
	}

	@Override
	public String toString() {
		read();
		return list.toString();
	}

	@Override
	public Iterator<E> entries(CollectionPersister persister) {
		return list.iterator();
	}

	@Override
	public Object disassemble(CollectionPersister persister) throws HibernateException {
		final int length = list.size();
		final Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementType().disassemble( list.get( i ), getSession(), null );
		}
		return result;
	}

	@Override
	public Iterator<?> getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		final List<Object> deletes = new ArrayList<>();
		final List<?> sn = (List<?>) getSnapshot();
		int end;
		final int snSize = sn.size();
		if ( snSize > list.size() ) {
			for ( int i = list.size(); i < snSize; i++ ) {
				deletes.add( indexIsFormula ? sn.get( i ) : i );
			}
			end = list.size();
		}
		else {
			end = snSize;
		}
		for ( int i=0; i<end; i++ ) {
			final Object item = list.get( i );
			final Object snapshotItem = sn.get( i );
			if ( item == null && snapshotItem != null ) {
				deletes.add( indexIsFormula ? snapshotItem : i );
			}
		}
		return deletes.iterator();
	}

	@Override
	public boolean hasDeletes(CollectionPersister persister) {
		final List<?> sn = (List<?>) getSnapshot();
		int snSize = sn.size();
		if ( snSize > list.size() ) {
			return true;
		}
		for ( int i=0; i<snSize; i++ ) {
			if ( list.get( i ) == null && sn.get( i ) != null ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<?> getRemovedEntities(CollectionPersister persister) {
		final List<?> sn = (List<?>) getSnapshot();
		if ( sn == null || sn.isEmpty() ) {
			return java.util.Collections.emptyIterator();
		}

		// Find entities in snapshot that are no longer in current list (by identity)
		final List<Object> removedEntities = new ArrayList<>();
		for ( Object snapshotEntity : sn ) {
			if ( snapshotEntity != null ) {
				boolean found = false;
				for ( Object currentEntity : list ) {
					if ( currentEntity == snapshotEntity ) {
						found = true;
						break;
					}
				}
				if ( !found ) {
					removedEntities.add( snapshotEntity );
				}
			}
		}
		return removedEntities.iterator();
	}

	@Override
	public Iterator<?> getAddedEntities(CollectionPersister persister) {
		final List<?> sn = (List<?>) getSnapshot();

		// Find entities in current list that were not in snapshot (by identity)
		final List<Object> addedEntities = new ArrayList<>();
		for ( Object currentEntity : list ) {
			if ( currentEntity != null ) {
				boolean found = false;
				if ( sn != null ) {
					for ( Object snapshotEntity : sn ) {
						if ( snapshotEntity == currentEntity ) {
							found = true;
							break;
						}
					}
				}
				if ( !found ) {
					addedEntities.add( currentEntity );
				}
			}
		}
		return addedEntities.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public CollectionChangeSet getChangeSet(CollectionPersister persister) {
		final List<?> sn = (List<?>) getSnapshot();
		if (sn == null) {
			return CollectionChangeSet.EMPTY;
		}

		final boolean isEntityCollection = persister.getElementType().isEntityType();

		if (isEntityCollection) {
			return computeEntityListChangeSet(sn);
		}
		else {
			return computeElementListChangeSet(sn, persister);
		}
	}

	/**
	 * Compute change set for entity collections (join tables).
	 * Uses identity-based comparison to track which entities were added, removed, or shifted.
	 */
	private CollectionChangeSet computeEntityListChangeSet(List<?> snapshot) {
		// Build position map for O(1) lookup (identity-based for entities)
		final java.util.Map<Object, Integer> snapshotPositions = new java.util.IdentityHashMap<>();
		for (int i = 0; i < snapshot.size(); i++) {
			snapshotPositions.put(snapshot.get(i), i);
		}

		final java.util.Set<Object> processedElements = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		final java.util.List<CollectionChangeSet.Removal> removals = new ArrayList<>();
		final java.util.List<CollectionChangeSet.Addition> additions = new ArrayList<>();
		final java.util.List<CollectionChangeSet.Shift> shifts = new ArrayList<>();

		// Single pass through current collection
		for (int currentPos = 0; currentPos < list.size(); currentPos++) {
			final Object element = list.get(currentPos);
			final Integer snapshotPos = snapshotPositions.get(element);

			if (snapshotPos == null) {
				// Not in snapshot → ADDITION
				additions.add(new CollectionChangeSet.Addition(element, currentPos));
			}
			else {
				// In snapshot → check if position changed
				processedElements.add(element);
				if (snapshotPos != currentPos) {
					// Position changed → SHIFT
					shifts.add(new CollectionChangeSet.Shift(element, snapshotPos, currentPos));
				}
				// else: no change, do nothing
			}
		}

		// Find removals: elements in snapshot but not processed
		for (int i = 0; i < snapshot.size(); i++) {
			final Object element = snapshot.get(i);
			if (!processedElements.contains(element)) {
				// In snapshot but not in current → REMOVAL
				removals.add(new CollectionChangeSet.Removal(element, i));
			}
		}

		return new CollectionChangeSet(removals, additions, shifts, List.of());
	}

	/**
	 * Compute change set for element collections.
	 * Uses equals-based comparison to detect value changes at the same position.
	 */
	private CollectionChangeSet computeElementListChangeSet(List<?> snapshot, CollectionPersister persister) {
		final Type elementType = persister.getElementType();
		final java.util.List<CollectionChangeSet.Removal> removals = new ArrayList<>();
		final java.util.List<CollectionChangeSet.Addition> additions = new ArrayList<>();
		final java.util.List<CollectionChangeSet.ValueChange> valueChanges = new ArrayList<>();

		final int snapshotSize = snapshot.size();
		final int currentSize = list.size();
		final int commonSize = Math.min(snapshotSize, currentSize);

		// Check positions that exist in both snapshot and current
		for (int i = 0; i < commonSize; i++) {
			final Object snapshotElement = snapshot.get(i);
			final Object currentElement = list.get(i);

			if (snapshotElement == null && currentElement != null) {
				// Was null, now has value → ADDITION (at same position)
				additions.add(new CollectionChangeSet.Addition(currentElement, i));
			}
			else if (snapshotElement != null && currentElement == null) {
				// Had value, now null → REMOVAL (at same position)
				removals.add(new CollectionChangeSet.Removal(snapshotElement, i));
			}
			else if (snapshotElement != null && currentElement != null) {
				// Both non-null → check if value changed
				if (elementType.isDirty(currentElement, snapshotElement, getSession())) {
					valueChanges.add(new CollectionChangeSet.ValueChange(snapshotElement, currentElement, i));
				}
			}
			// else: both null, no change
		}

		// Elements beyond common size
		if (currentSize > snapshotSize) {
			// Current is longer → additions
			for (int i = snapshotSize; i < currentSize; i++) {
				final Object element = list.get(i);
				if (element != null) {
					additions.add(new CollectionChangeSet.Addition(element, i));
				}
			}
		}
		else if (snapshotSize > currentSize) {
			// Snapshot was longer → removals
			for (int i = currentSize; i < snapshotSize; i++) {
				final Object element = snapshot.get(i);
				if (element != null) {
					removals.add(new CollectionChangeSet.Removal(element, i));
				}
			}
		}

		// Note: Element collections don't have shifts in the same sense as entity collections
		// Position changes are handled as removal + addition
		return new CollectionChangeSet(removals, additions, List.of(), valueChanges);
	}

	@Override
	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		final List<?> sn = (List<?>) getSnapshot();
		return list.get( i ) != null && ( i >= sn.size() || sn.get( i ) == null );
	}

	@Override
	public boolean needsUpdating(Object entry, int i, Type elemType) throws HibernateException {
		final List<?> sn = (List<?>) getSnapshot();
		return i < sn.size()
				&& sn.get( i ) != null
				&& list.get( i ) != null
				&& elemType.isDirty( list.get( i ), sn.get( i ), getSession() );
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
		final List<?> sn = (List<?>) getSnapshot();
		return sn.get( i );
	}

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object other) {
		read();
		return list.equals( other );
	}

	@Override
	public int hashCode() {
		read();
		return list.hashCode();
	}

	@Override
	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

	final class Clear implements DelayedOperation<E> {
		@Override
		public void operate() {
			list.clear();
		}

		@Override
		public E getAddedInstance() {
			return null;
		}

		@Override
		public E getOrphan() {
			throw new UnsupportedOperationException( "queued clear cannot be used with orphan delete" );
		}
	}

	protected final class SimpleAdd extends AbstractValueDelayedOperation {

		public SimpleAdd(E addedValue) {
			super( addedValue, null );
		}

		@Override
		public void operate() {
			list.add( getAddedInstance() );
		}
	}

	abstract class AbstractListValueDelayedOperation extends AbstractValueDelayedOperation {
		private final int index;

		AbstractListValueDelayedOperation(Integer index, E addedValue, E orphan) {
			super( addedValue, orphan );
			this.index = index;
		}

		protected final int getIndex() {
			return index;
		}
	}

	final class Add extends AbstractListValueDelayedOperation {

		public Add(int index, E addedValue) {
			super( index, addedValue, null );
		}

		@Override
		public void operate() {
			list.add( getIndex(), getAddedInstance() );
		}
	}

	final class Set extends AbstractListValueDelayedOperation {

		public Set(int index, E addedValue, E orphan) {
			super( index, addedValue, orphan );
		}

		@Override
		public void operate() {
			list.set( getIndex(), getAddedInstance() );
		}
	}

	final class Remove extends AbstractListValueDelayedOperation {

		public Remove(int index, E orphan) {
			super( index, null, orphan );
		}

		@Override
		public void operate() {
			list.remove( getIndex() );
		}
	}

	final class SimpleRemove extends AbstractValueDelayedOperation {

		public SimpleRemove(E orphan) {
			super( null, orphan );
		}

		@Override
		public void operate() {
			list.remove( getOrphan() );
		}
	}
}
