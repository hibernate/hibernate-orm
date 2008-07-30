/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.collection;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
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
	 */
	public Object getOwner();
	/**
	 * Set the reference to the owning entity
	 */
	public void setOwner(Object entity);
	
	/**
	 * Is the collection empty? (don't try to initialize the collection)
	 */
	public boolean empty();

	/**
	 * After flushing, re-init snapshot state.
	 */
	public void setSnapshot(Serializable key, String role, Serializable snapshot);
	
	/**
	 * After flushing, clear any "queued" additions, since the
	 * database state is now synchronized with the memory state.
	 */
	public void postAction();
	
	/**
	 * return the user-visible collection (or array) instance
	 */
	public Object getValue();

	/**
	 * Called just before reading any rows from the JDBC result set
	 */
	public void beginRead();

	/**
	 * Called after reading all rows from the JDBC result set
	 */
	public boolean endRead();
	
	/**
	 * Called after initializing from cache
	 */
	public boolean afterInitialize();

	/**
	 * Could the application possibly have a direct reference to
	 * the underlying collection implementation?
	 */
	public boolean isDirectlyAccessible();

	/**
	 * Disassociate this collection from the given session.
	 * @return true if this was currently associated with the given session
	 */
	public boolean unsetSession(SessionImplementor currentSession);

	/**
	 * Associate the collection with the given session.
	 * @return false if the collection was already associated with the session
	 * @throws HibernateException if the collection was already associated
	 * with another open session
	 */
	public boolean setCurrentSession(SessionImplementor session)
			throws HibernateException;

	/**
	 * Read the state of the collection from a disassembled cached value
	 */
	public void initializeFromCache(CollectionPersister persister,
			Serializable disassembled, Object owner) throws HibernateException;

	/**
	 * Iterate all collection entries, during update of the database
	 */
	public Iterator entries(CollectionPersister persister);

	/**
	 * Read a row from the JDBC result set
	 */
	public Object readFrom(ResultSet rs, CollectionPersister role, CollectionAliases descriptor, Object owner)
			throws HibernateException, SQLException;

	/**
	 * Get the index of the given collection entry
	 */
	public Object getIdentifier(Object entry, int i);
	
	/**
	 * Get the index of the given collection entry
	 * @param persister it was more elegant before we added this...
	 */
	public Object getIndex(Object entry, int i, CollectionPersister persister);
	
	/**
	 * Get the value of the given collection entry
	 */
	public Object getElement(Object entry);
	
	/**
	 * Get the snapshot value of the given collection entry
	 */
	public Object getSnapshotElement(Object entry, int i);

	/**
	 * Called before any elements are read into the collection,
	 * allowing appropriate initializations to occur.
	 *
	 * @param persister The underlying collection persister.
	 * @param anticipatedSize The anticipated size of the collection after initilization is complete.
	 */
	public void beforeInitialize(CollectionPersister persister, int anticipatedSize);

	/**
	 * Does the current state exactly match the snapshot?
	 */
	public boolean equalsSnapshot(CollectionPersister persister) 
		throws HibernateException;

	/**
	 * Is the snapshot empty?
	 */
	public boolean isSnapshotEmpty(Serializable snapshot);
	
	/**
	 * Disassemble the collection, ready for the cache
	 */
	public Serializable disassemble(CollectionPersister persister)
	throws HibernateException;

	/**
	 * Do we need to completely recreate this collection when it changes?
	 */
	public boolean needsRecreate(CollectionPersister persister);

	/**
	 * Return a new snapshot of the current state of the collection
	 */
	public Serializable getSnapshot(CollectionPersister persister)
			throws HibernateException;

	/**
	 * To be called internally by the session, forcing
	 * immediate initialization.
	 */
	public void forceInitialization() throws HibernateException;

	/**
	 * Does an element exist at this entry in the collection?
	 */
	public boolean entryExists(Object entry, int i); //note that i parameter is now unused (delete it?)

	/**
	 * Do we need to insert this element?
	 */
	public boolean needsInserting(Object entry, int i, Type elemType)
			throws HibernateException;

	/**
	 * Do we need to update this element?
	 */
	public boolean needsUpdating(Object entry, int i, Type elemType)
			throws HibernateException;
	
	public boolean isRowUpdatePossible();

	/**
	 * Get all the elements that need deleting
	 */
	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) 
			throws HibernateException;

	/**
	 * Is this the wrapper for the given underlying collection instance?
	 */
	public boolean isWrapper(Object collection);

	/**
	 * Is this instance initialized?
	 */
	public boolean wasInitialized();

	/**
	 * Does this instance have any "queued" additions?
	 */
	public boolean hasQueuedOperations();

	/**
	 * Iterate the "queued" additions
	 */
	public Iterator queuedAdditionIterator();
	
	/**
	 * Get the "queued" orphans
	 */
	public Collection getQueuedOrphans(String entityName);
	
	/**
	 * Get the current collection key value
	 */
	public Serializable getKey();
	
	/**
	 * Get the current role name
	 */
	public String getRole();
	
	/**
	 * Is the collection unreferenced?
	 */
	public boolean isUnreferenced();
	
	/**
	 * Is the collection dirty? Note that this is only
	 * reliable during the flush cycle, after the 
	 * collection elements are dirty checked against
	 * the snapshot.
	 */
	public boolean isDirty();
	
	/**
	 * Clear the dirty flag, after flushing changes
	 * to the database.
	 */
	public void clearDirty();
	
	/**
	 * Get the snapshot cached by the collection
	 * instance
	 */
	public Serializable getStoredSnapshot();
	
	/**
	 * Mark the collection as dirty
	 */
	public void dirty();
	
	/**
	 * Called before inserting rows, to ensure that any surrogate keys
	 * are fully generated
	 */
	public void preInsert(CollectionPersister persister)
	throws HibernateException;

	/**
	 * Called after inserting a row, to fetch the natively generated id
	 */
	public void afterRowInsert(CollectionPersister persister, Object entry, int i) 
	throws HibernateException;

	/**
	 * get all "orphaned" elements
	 */
	public Collection getOrphans(Serializable snapshot, String entityName)
	throws HibernateException;
	
}