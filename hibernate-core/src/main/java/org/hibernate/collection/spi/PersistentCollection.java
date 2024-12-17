/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Persistent collections are treated as value objects by Hibernate.
 * They have no independent existence beyond the entity holding a
 * reference to them. Unlike instances of entity classes, they are
 * automatically deleted when unreferenced and automatically become
 * persistent when held by a persistent object. Collections can be
 * passed between different objects (change "roles") and this might
 * cause their elements to move from one database table to another.
 * <p>
 * Hibernate "wraps" a Java collection in an instance of
 * {@code PersistentCollection}. This mechanism is allows for
 * tracking of changes to the persistent state of the collection and
 * lazy fetching of the collection elements. The downside is that
 * only certain abstract collection types are supported and any
 * extra semantics associated with the particular implementation of
 * the generic collection type are lost. For example, every
 * {@link java.util.List} behaves like an {@code ArrayList}, and
 * every {@link java.util.SortedMap} behaves like a {@code TreeMap}.
 * <p>
 * Applications should <em>never</em> use classes in this package
 * directly, unless extending the "framework" here.
 * <p>
 * Changes to <em>structure</em> of the collection are recorded by
 * the collection calling back to the session. Changes to mutable
 * elements (composite elements) are discovered by cloning their
 * state when the collection is initialized and comparing at flush
 * time.
 *
 * @param <E> the collection element type, or map value type
 *
 * @author Gavin King
 */
@Incubating
public interface PersistentCollection<E> extends LazyInitializable {
	/**
	 * Get the owning entity. Note that the owner is only
	 * set during the flush cycle, and when a new collection
	 * wrapper is created while loading an entity.
	 *
	 * @return The owner
	 */
	@Nullable Object getOwner();

	/**
	 * Set the reference to the owning entity
	 *
	 * @param entity The owner
	 */
	void setOwner(Object entity);

	/**
	 * Is the collection empty? (don't try to initialize the collection)
	 *
	 * @return {@code false} if the collection is non-empty; {@code true} otherwise.
	 */
	boolean empty();

	/**
	 * After flushing, re-init snapshot state.
	 *  @param key The collection instance key (fk value).
	 * @param role The collection role
	 * @param snapshot The snapshot state
	 */
	void setSnapshot(@Nullable Object key, @Nullable String role, @Nullable Serializable snapshot);

	/**
	 * After flushing, clear any "queued" additions, since the
	 * database state is now synchronized with the memory state.
	 */
	void postAction();

	/**
	 * Return the user-visible collection (or array) instance
	 *
	 * @return The underlying collection/array
	 */
	Object getValue();

	/**
	 * Could the application possibly have a direct reference to
	 * the underlying collection implementation?
	 *
	 * @return {@code true} indicates that the application might have access to the underlying collection/array.
	 */
	boolean isDirectlyAccessible();

	/**
	 * Disassociate this collection from the given session.
	 *
	 * @param currentSession The session we are disassociating from.  Used for validations.
	 *
	 * @return true if this was currently associated with the given session
	 */
	boolean unsetSession(SharedSessionContractImplementor currentSession);

	/**
	 * Associate the collection with the given session.
	 *
	 * @param session The session to associate with
	 *
	 * @return false if the collection was already associated with the session
	 *
	 * @throws HibernateException if the collection was already associated
	 * with another open session
	 */
	boolean setCurrentSession(SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Iterate all collection entries, during update of the database
	 *
	 * @param persister The collection persister.
	 *
	 * @return The iterator
	 */
	Iterator<?> entries(CollectionPersister persister);

	/**
	 * Get the identifier of the given collection entry.  This refers to the collection identifier, not the
	 * identifier of the (possibly) entity elements.  This is only valid for invocation on the
	 * {@code idbag} collection.
	 *
	 * @param entry The collection entry/element
	 * @param i The assumed identifier (?)
	 *
	 * @return The identifier value
	 */
	Object getIdentifier(Object entry, int i);

	/**
	 * Get the index of the given collection entry
	 *
	 * @param entry The collection entry/element
	 * @param i The assumed index
	 * @param persister it was more elegant before we added this...
	 *
	 * @return The index value
	 */
	Object getIndex(Object entry, int i, CollectionPersister persister);

	/**
	 * Get the value of the given collection entry.  Generally the given entry parameter value will just be returned.
	 * Might get a different value for a duplicate entries in a Set.
	 *
	 * @param entry The object instance for which to get the collection element instance.
	 *
	 * @return The corresponding object that is part of the collection elements.
	 */
	Object getElement(Object entry);

	/**
	 * Get the snapshot value of the given collection entry
	 *
	 * @param entry The entry
	 * @param i The index
	 *
	 * @return The snapshot state for that element
	 */
	Object getSnapshotElement(Object entry, int i);

	/**
	 * Does the current state exactly match the snapshot?
	 *
	 * @param persister The collection persister
	 *
	 * @return {@code true} if the current state and the snapshot state match.
	 *
	 */
	boolean equalsSnapshot(CollectionPersister persister);

	/**
	 * Is the snapshot empty?
	 *
	 * @param snapshot The snapshot to check
	 *
	 * @return {@code true} if the given snapshot is empty
	 */
	boolean isSnapshotEmpty(Serializable snapshot);

	/**
	 * Do we need to completely recreate this collection when it changes?
	 *
	 * @param persister The collection persister
	 *
	 * @return {@code true} if a change requires a recreate.
	 */
	boolean needsRecreate(CollectionPersister persister);

	/**
	 * Return a new snapshot of the current state of the collection
	 *
	 * @param persister The collection persister
	 *
	 * @return The snapshot
	 */
	Serializable getSnapshot(CollectionPersister persister);

	/**
	 * Does the given element/entry exist in the collection?
	 *
	 * @param entry The object to check if it exists as a collection element
	 * @param i Unused
	 *
	 * @return {@code true} if the given entry is a collection element
	 */
	boolean entryExists(Object entry, int i);

	/**
	 * Whether the given entry should be included in recreation events
	 *
	 * @apiNote Defined to match signature of {@link InsertRowsCoordinator.EntryFilter#include}
	 */
	default boolean includeInRecreate(
			Object entry,
			int i,
			PersistentCollection<?> collection,
			PluralAttributeMapping attributeDescriptor) {
		assert collection == this;
		assert attributeDescriptor != null;

		return entryExists( entry, i );
	}

	/**
	 * Do we need to insert this element?
	 *
	 * @param entry The collection element to check
	 * @param i The index (for indexed collections)
	 * @param elemType The type for the element
	 *
	 * @return {@code true} if the element needs inserting
	 */
	boolean needsInserting(Object entry, int i, Type elemType);

	/**
	 * Whether to include the entry for insertion operations
	 *
	 * @apiNote Defined to match signature of {@link InsertRowsCoordinator.EntryFilter#include}
	 */
	default boolean includeInInsert(
			Object entry,
			int entryPosition,
			PersistentCollection<?> collection,
			PluralAttributeMapping attributeDescriptor) {
		assert collection == this;
		assert attributeDescriptor != null;

		return needsInserting( entry, entryPosition, attributeDescriptor.getCollectionDescriptor().getElementType() );
	}

	/**
	 * Do we need to update this element?
	 *
	 * @param entry The collection element to check
	 * @param entryPosition The index (for indexed collections)
	 * @param attributeDescriptor The type for the element
	 * @return {@code true} if the element needs updating
	 */
	default boolean needsUpdating(
			Object entry,
			int entryPosition,
			PluralAttributeMapping attributeDescriptor) {
		assert attributeDescriptor != null;

		return needsUpdating( entry, entryPosition, attributeDescriptor.getCollectionDescriptor().getElementType() );
	}

	/**
	 * Do we need to update this element?
	 *
	 * @param entry The collection element to check
	 * @param i The index (for indexed collections)
	 * @param elemType The type for the element
	 *
	 * @return {@code true} if the element needs updating
	 */
	boolean needsUpdating(Object entry, int i, Type elemType);

	/**
	 * Can each element in the collection be mapped unequivocally to a single row in the database?  Generally
	 * bags and sets are the only collections that cannot be.
	 *
	 * @return {@code true} if the row for each element is known
	 */
	boolean isRowUpdatePossible();

	/**
	 * Get all the elements that need deleting
	 *
	 * @param persister The collection persister
	 * @param indexIsFormula For indexed collections, tells whether the index is a formula (calculated value) mapping
	 *
	 * @return An iterator over the elements to delete
	 */
	Iterator<?> getDeletes(CollectionPersister persister, boolean indexIsFormula);

	/**
	 * Is this the wrapper for the given collection instance?
	 *
	 * @param collection The collection to check whether this is wrapping it
	 *
	 * @return  {@code true} if this is a wrapper around that given collection instance.
	 */
	boolean isWrapper(Object collection);

	/**
	 * Is this PersistentCollection in the process of being initialized?
	 */
	boolean isInitializing();

	/**
	 * Called prior to the initialization of this yet-uninitialized collection.  Pairs
	 * with {@link #afterInitialize}
	 */
	void beforeInitialize(CollectionPersister persister, int anticipatedSize);

	/**
	 * Read the state of the collection from a disassembled cached value
	 *
	 * @param persister The collection persister
	 * @param disassembled The disassembled cached state
	 * @param owner The collection owner
	 */
	void initializeFromCache(CollectionPersister persister, Object disassembled, Object owner);

	/**
	 * Called just before reading any rows from the JDBC result set.  Pairs with {@link #endRead}
	 */
	void beginRead();

	/**
	 * Inject the state loaded for a collection instance.
	 */
	void injectLoadedState(PluralAttributeMapping attributeMapping, List<?> loadingState);

	/**
	 * Called after reading all rows from the JDBC result set.  Pairs with {@link #beginRead}
	 *
	 * @see #injectLoadedState
	 */
	@SuppressWarnings("UnusedReturnValue")
	boolean endRead();

	/**
	 * Called after initialization is complete.  Pairs with {@link #beforeInitialize}
	 */
	boolean afterInitialize();

	/**
	 * Disassemble the collection to get it ready for the cache
	 *
	 * @param persister The collection persister
	 *
	 * @return The disassembled state
	 */
	Object disassemble(CollectionPersister persister) ;

	/**
	 * Does this instance have any "queued" operations?
	 *
	 * @return {@code true} indicates there are pending, queued, delayed operations
	 */
	boolean hasQueuedOperations();

	/**
	 * Iterator over the "queued" additions
	 *
	 * @return The iterator
	 */
	Iterator<E> queuedAdditionIterator();

	/**
	 * Get the "queued" orphans
	 *
	 * @param entityName The name of the entity that makes up the elements
	 *
	 * @return The orphaned elements
	 */
	Collection<E> getQueuedOrphans(String entityName);

	/**
	 * Get the current collection key value
	 *
	 * @return the current collection key value
	 */
	@Nullable Object getKey();

	/**
	 * Get the current role name
	 *
	 * @return the collection role name
	 */
	@Nullable String getRole();

	/**
	 * Is the collection unreferenced?
	 *
	 * @return {@code true} if the collection is no longer referenced by an owner
	 */
	boolean isUnreferenced();

	/**
	 * Is the collection dirty? Note that this is only
	 * reliable during the flush cycle, after the
	 * collection elements are dirty checked against
	 * the snapshot.
	 *
	 * @return {@code true} if the collection is dirty
	 */
	boolean isDirty();

	default boolean isElementRemoved(){
		return false;
	}

	/**
	 * Was {@code collection} provided directly to this PersistentCollection
	 * (i.e., provided as an argument to a constructor)?
	 * <p>
	 * Implementors that can copy elements out of a directly provided
	 * collection into the wrapped collection should override this method.
	 * <p>
	 * @param collection The collection
	 * @return true, if {@code collection} was provided directly to this
	 * PersistentCollection; false, otherwise.
	 */
	default boolean isDirectlyProvidedCollection(Object collection) {
		return isDirectlyAccessible() && isWrapper( collection );
	}

	/**
	 * Clear the dirty flag, after flushing changes
	 * to the database.
	 */
	void clearDirty();

	/**
	 * Get the snapshot cached by the collection instance
	 *
	 * @return The internally stored snapshot state
	 */
	@Nullable Serializable getStoredSnapshot();

	/**
	 * Mark the collection as dirty
	 */
	void dirty();

	/**
	 * Called before inserting rows, to ensure that any surrogate keys
	 * are fully generated
	 *
	 * @param persister The collection persister
	 */
	void preInsert(CollectionPersister persister);

	/**
	 * Called after inserting a row, to fetch the natively generated id
	 *
	 * @param persister The collection persister
	 * @param entry The collection element just inserted
	 * @param i The element position/index
	 */
	void afterRowInsert(CollectionPersister persister, Object entry, int i);

	/**
	 * get all "orphaned" elements
	 *
	 * @param snapshot The snapshot state
	 * @param entityName The name of the entity that are the elements of the collection
	 *
	 * @return The orphans
	 */
	Collection<E> getOrphans(Serializable snapshot, String entityName);

	/**
	 * Obtain the size of this collection without initializing it
	 */
	int getSize();

	/**
	 * Determine if the given element belongs to this collection without initializing it
	 */
	boolean elementExists(Object element);

	/**
	 * Obtain the element os this collection associated with the given index without initializing it
	 */
	Object elementByIndex(Object index);

	void initializeEmptyCollection(CollectionPersister persister);

	/**
	 * Is the collection newly instantiated?
	 *
	 * @return {@code true} if the collection is newly instantiated
	 */
	default boolean isNewlyInstantiated() {
		return false;
	}

	/**
	 * Like {@link Object#toString()} but without the silliness of rendering the elements
	 */
	default String render() {
		return getRole() + "#" + getKey() + "(initialized: " + wasInitialized() + ")";
	}

	default void queueRemoveOperation(Object o){

	}

	default void queueAddOperation(E o){

	}

}
