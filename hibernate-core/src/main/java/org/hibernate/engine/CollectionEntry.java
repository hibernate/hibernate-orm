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
package org.hibernate.engine;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * We need an entry to tell us all about the current state
 * of a collection with respect to its persistent state
 * 
 * @author Gavin King
 */
public final class CollectionEntry implements Serializable {

	private static final Logger log = LoggerFactory.getLogger(CollectionEntry.class);
	
	//ATTRIBUTES MAINTAINED BETWEEN FLUSH CYCLES
	
	// session-start/post-flush persistent state
	private Serializable snapshot;
	// allow the CollectionSnapshot to be serialized
	private String role;
	
	// "loaded" means the reference that is consistent 
	// with the current database state
	private transient CollectionPersister loadedPersister;
	private Serializable loadedKey;

	// ATTRIBUTES USED ONLY DURING FLUSH CYCLE
	
	// during flush, we navigate the object graph to
	// collections and decide what to do with them
	private transient boolean reached;
	private transient boolean processed;
	private transient boolean doupdate;
	private transient boolean doremove;
	private transient boolean dorecreate;
	// if we instantiate a collection during the flush() process,
	// we must ignore it for the rest of the flush()
	private transient boolean ignore;
	
	// "current" means the reference that was found during flush() 
	private transient CollectionPersister currentPersister;
	private transient Serializable currentKey;

	/**
	 * For newly wrapped collections, or dereferenced collection wrappers
	 */
	public CollectionEntry(CollectionPersister persister, PersistentCollection collection) {
		// new collections that get found + wrapped
		// during flush shouldn't be ignored
		ignore = false;

		collection.clearDirty(); //a newly wrapped collection is NOT dirty (or we get unnecessary version updates)
		
		snapshot = persister.isMutable() ?
				collection.getSnapshot(persister) :
				null;
		collection.setSnapshot(loadedKey, role, snapshot);
	}

	/**
	 * For collections just loaded from the database
	 */
	public CollectionEntry(
			final PersistentCollection collection, 
			final CollectionPersister loadedPersister, 
			final Serializable loadedKey, 
			final boolean ignore
	) {
		this.ignore=ignore;

		//collection.clearDirty()
		
		this.loadedKey = loadedKey;
		setLoadedPersister(loadedPersister);

		collection.setSnapshot(loadedKey, role, null);

		//postInitialize() will be called after initialization
	}

	/**
	 * For uninitialized detached collections
	 */
	public CollectionEntry(CollectionPersister loadedPersister, Serializable loadedKey) {
		// detached collection wrappers that get found + reattached
		// during flush shouldn't be ignored
		ignore = false;

		//collection.clearDirty()
		
		this.loadedKey = loadedKey;
		setLoadedPersister(loadedPersister);
	}
	
	/**
	 * For initialized detached collections
	 */
	CollectionEntry(PersistentCollection collection, SessionFactoryImplementor factory)
	throws MappingException {
		// detached collections that get found + reattached
		// during flush shouldn't be ignored
		ignore = false;

		loadedKey = collection.getKey();
		setLoadedPersister( factory.getCollectionPersister( collection.getRole() ) );

		snapshot = collection.getStoredSnapshot();		
	}

	/**
	 * Used from custom serialization.
	 *
	 * @see #serialize
	 * @see #deserialize
	 */
	private CollectionEntry(
			String role,
	        Serializable snapshot,
	        Serializable loadedKey,
	        SessionFactoryImplementor factory) {
		this.role = role;
		this.snapshot = snapshot;
		this.loadedKey = loadedKey;
		if ( role != null ) {
			afterDeserialize( factory );
		}
	}

	/**
	 * Determine if the collection is "really" dirty, by checking dirtiness
	 * of the collection elements, if necessary
	 */
	private void dirty(PersistentCollection collection) throws HibernateException {
		
		boolean forceDirty = collection.wasInitialized() &&
				!collection.isDirty() && //optimization
				getLoadedPersister() != null &&
				getLoadedPersister().isMutable() && //optimization
				( collection.isDirectlyAccessible() || getLoadedPersister().getElementType().isMutable() ) && //optimization
				!collection.equalsSnapshot( getLoadedPersister() );
		
		if ( forceDirty ) {
			collection.dirty();
		}
		
	}

	public void preFlush(PersistentCollection collection) throws HibernateException {
		
		boolean nonMutableChange = collection.isDirty() && 
				getLoadedPersister()!=null && 
				!getLoadedPersister().isMutable();
		if (nonMutableChange) {
			throw new HibernateException(
					"changed an immutable collection instance: " + 
					MessageHelper.collectionInfoString( getLoadedPersister().getRole(), getLoadedKey() )
				);
		}
		
		dirty(collection);
		
		if ( log.isDebugEnabled() && collection.isDirty() && getLoadedPersister() != null ) {
			log.debug(
					"Collection dirty: " +
					MessageHelper.collectionInfoString( getLoadedPersister().getRole(), getLoadedKey() )
				);
		}

		setDoupdate(false);
		setDoremove(false);
		setDorecreate(false);
		setReached(false);
		setProcessed(false);
	}

	public void postInitialize(PersistentCollection collection) throws HibernateException {
		snapshot = getLoadedPersister().isMutable() ?
				collection.getSnapshot( getLoadedPersister() ) :
				null;
		collection.setSnapshot(loadedKey, role, snapshot);
	}

	/**
	 * Called after a successful flush
	 */
	public void postFlush(PersistentCollection collection) throws HibernateException {
		if ( isIgnore() ) {
			ignore = false;
		}
		else if ( !isProcessed() ) {
			throw new AssertionFailure( "collection [" + collection.getRole() + "] was not processed by flush()" );
		}
		collection.setSnapshot(loadedKey, role, snapshot);
	}
	
	/**
	 * Called after execution of an action
	 */
	public void afterAction(PersistentCollection collection) {
		loadedKey = getCurrentKey();
		setLoadedPersister( getCurrentPersister() );
		
		boolean resnapshot = collection.wasInitialized() && 
				( isDoremove() || isDorecreate() || isDoupdate() );
		if ( resnapshot ) {
			snapshot = loadedPersister==null || !loadedPersister.isMutable() ? 
					null : 
					collection.getSnapshot(loadedPersister); //re-snapshot
		}
		
		collection.postAction();
	}

	public Serializable getKey() {
		return getLoadedKey();
	}

	public String getRole() {
		return role;
	}

	public Serializable getSnapshot() {
		return snapshot;
	}

	private void setLoadedPersister(CollectionPersister persister) {
		loadedPersister = persister;
		setRole( persister == null ? null : persister.getRole() );
	}
	
	void afterDeserialize(SessionFactoryImplementor factory) {
		loadedPersister = ( factory == null ? null : factory.getCollectionPersister(role) );
	}

	public boolean wasDereferenced() {
		return getLoadedKey() == null;
	}

	public boolean isReached() {
		return reached;
	}

	public void setReached(boolean reached) {
		this.reached = reached;
	}

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

	public boolean isDoupdate() {
		return doupdate;
	}

	public void setDoupdate(boolean doupdate) {
		this.doupdate = doupdate;
	}

	public boolean isDoremove() {
		return doremove;
	}

	public void setDoremove(boolean doremove) {
		this.doremove = doremove;
	}

	public boolean isDorecreate() {
		return dorecreate;
	}

	public void setDorecreate(boolean dorecreate) {
		this.dorecreate = dorecreate;
	}

	public boolean isIgnore() {
		return ignore;
	}

	public CollectionPersister getCurrentPersister() {
		return currentPersister;
	}

	public void setCurrentPersister(CollectionPersister currentPersister) {
		this.currentPersister = currentPersister;
	}

	/**
	 * This is only available late during the flush
	 * cycle
	 */
	public Serializable getCurrentKey() {
		return currentKey;
	}

	public void setCurrentKey(Serializable currentKey) {
		this.currentKey = currentKey;
	}
	
	/**
	 * This is only available late during the flush cycle
	 */
	public CollectionPersister getLoadedPersister() {
		return loadedPersister;
	}

	public Serializable getLoadedKey() {
		return loadedKey;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String toString() {
		String result = "CollectionEntry" + 
				MessageHelper.collectionInfoString( loadedPersister.getRole(), loadedKey );
		if (currentPersister!=null) {
			result += "->" + 
					MessageHelper.collectionInfoString( currentPersister.getRole(), currentKey );
		}
		return result;
	}

	/**
	 * Get the collection orphans (entities which were removed from the collection)
	 */
	public Collection getOrphans(String entityName, PersistentCollection collection) 
	throws HibernateException {
		if (snapshot==null) {
			throw new AssertionFailure("no collection snapshot for orphan delete");
		}
		return collection.getOrphans( snapshot, entityName );
	}

	public boolean isSnapshotEmpty(PersistentCollection collection) {
		//TODO: does this really need to be here?
		//      does the collection already have
		//      it's own up-to-date snapshot?
		return collection.wasInitialized() && 
			( getLoadedPersister()==null || getLoadedPersister().isMutable() ) &&
			collection.isSnapshotEmpty( getSnapshot() );
	}



	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 * @throws java.io.IOException
	 */
	void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( role );
		oos.writeObject( snapshot );
		oos.writeObject( loadedKey );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param session The session being deserialized.
	 * @return The deserialized CollectionEntry
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	static CollectionEntry deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		return new CollectionEntry(
				( String ) ois.readObject(),
		        ( Serializable ) ois.readObject(),
		        ( Serializable ) ois.readObject(),
		        ( session == null ? null : session.getFactory() )
		);
	}
}