/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.spi;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * Persistent collections are treated as value objects by Hibernate.
 * ie. they have no independent existence beyond the object holding
 * a reference to them. Unlike instances of entity classes, they are
 * automatically deleted when unreferenced and automatically become
 * persistent when held by a persistent object. Collections can be
 * passed between different objects (change "roles") and this might
 * cause their elements to move from one database table to another.<br>
 * <br>
 * Hibernate "wraps" a java collection in an instance of
 * PersistentCollection. This mechanism is designed to support
 * tracking of changes to the collection's persistent state and
 * lazy instantiation of collection elements. The downside is that
 * only certain abstract collection types are supported and any
 * extra semantics are lost<br>
 * <br>
 * Applications should <em>never</em> use classes in this package
 * directly, unless extending the "framework" here.<br>
 * <br>
 * Changes to <em>structure</em> of the collection are recorded by the
 * collection calling back to the session. Changes to mutable
 * elements (ie. composite elements) are discovered by cloning their
 * state when the collection is initialized and comparing at flush
 * time.
 *
 * @author Gavin King
 */
public interface PersistentCollection {
	/**
	 * Get the owning entity. Note that the owner is only
	 * set during the flush cycle, and when a new collection
	 * wrapper is created while loading an entity.
	 *
	 * @return The owner
	 */
	Object getOwner();

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
	 *
	 * @param key The collection instance key (fk value).
	 * @param role The collection role
	 * @param snapshot The snapshot state
	 */
	void setSnapshot(Serializable key, String role, Serializable snapshot);

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
	 * Called just before reading any rows from the JDBC result set
	 */
	void beginRead();

	/**
	 * Called after reading all rows from the JDBC result set
	 *
	 * @return Whether to end the read.
	 */
	boolean endRead();

	/**
	 * Called after initializing from cache
	 *
	 * @return ??
	 */
	boolean afterInitialize();

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
	 * Read the state of the collection from a disassembled cached value
	 *
	 * @param persister The collection persister
	 * @param disassembled The disassembled cached state
	 * @param owner The collection owner
	 */
	void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner);

	/**
	 * Iterate all collection entries, during update of the database
	 *
	 * @param persister The collection persister.
	 *
	 * @return The iterator
	 */
	Iterator entries(CollectionPersister persister);

	/**
	 * Read a row from the JDBC result set
	 *
	 * @param rs The JDBC ResultSet
	 * @param role The collection role
	 * @param descriptor The aliases used for the columns making up the collection
	 * @param owner The collection owner
	 *
	 * @return The read object
	 *
	 * @throws HibernateException Generally indicates a problem resolving data read from the ResultSet
	 * @throws SQLException Indicates a problem accessing the ResultSet
	 */
	Object readFrom(ResultSet rs, CollectionPersister role, CollectionAliases descriptor, Object owner)
			throws HibernateException, SQLException;

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
	 * Called before any elements are read into the collection,
	 * allowing appropriate initializations to occur.
	 *
	 * @param persister The underlying collection persister.
	 * @param anticipatedSize The anticipated size of the collection after initialization is complete.
	 */
	void beforeInitialize(CollectionPersister persister, int anticipatedSize);

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
	 * Disassemble the collection to get it ready for the cache
	 *
	 * @param persister The collection persister
	 *
	 * @return The disassembled state
	 */
	Serializable disassemble(CollectionPersister persister) ;

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
	 * To be called internally by the session, forcing immediate initialization.
	 */
	void forceInitialization();

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
	Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula);

	/**
	 * Is this the wrapper for the given collection instance?
	 *
	 * @param collection The collection to check whether this is wrapping it
	 *
	 * @return  {@code true} if this is a wrapper around that given collection instance.
	 */
	boolean isWrapper(Object collection);

	/**
	 * Is this instance initialized?
	 *
	 * @return Was this collection initialized?  Or is its data still not (fully) loaded?
	 */
	boolean wasInitialized();

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
	Iterator queuedAdditionIterator();

	/**
	 * Get the "queued" orphans
	 *
	 * @param entityName The name of the entity that makes up the elements
	 *
	 * @return The orphaned elements
	 */
	Collection getQueuedOrphans(String entityName);

	/**
	 * Get the current collection key value
	 *
	 * @return the current collection key value
	 */
	Serializable getKey();

	/**
	 * Get the current role name
	 *
	 * @return the collection role name
	 */
	String getRole();

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
	 * <p/>
	 * Implementors that can copy elements out of a directly provided
	 * collection into the wrapped collection should override this method.
	 * <p/>
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
	Serializable getStoredSnapshot();

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
	Collection getOrphans(Serializable snapshot, String entityName);

}
