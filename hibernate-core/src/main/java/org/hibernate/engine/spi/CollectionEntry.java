/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.persister.collection.CollectionPersister;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * We need an entry to tell us all about the current state
 * of a collection with respect to its persistent state
 *
 * @author Gavin King
 */
public final class CollectionEntry implements Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( CollectionEntry.class );

	//ATTRIBUTES MAINTAINED BETWEEN FLUSH CYCLES

	// session-start/post-flush persistent state
	private @Nullable Serializable snapshot;
	// allow the CollectionSnapshot to be serialized
	private @Nullable String role;

	// "loaded" means the reference that is consistent
	// with the current database state
	private transient @Nullable CollectionPersister loadedPersister;
	private @Nullable Object loadedKey;

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
	private transient @Nullable CollectionPersister currentPersister;
	private transient @Nullable Object currentKey;

	/**
	 * For newly wrapped collections, or dereferenced collection wrappers
	 */
	public CollectionEntry(CollectionPersister persister, PersistentCollection<?> collection) {
		// new collections that get found + wrapped
		// during flush shouldn't be ignored
		ignore = false;

		collection.clearDirty(); //a newly wrapped collection is NOT dirty (or we get unnecessary version updates)

		snapshot = persister.isMutable() ?
				collection.getSnapshot( persister ) :
				null;
		collection.setSnapshot( loadedKey, role, snapshot );
	}

	/**
	 * For collections just loaded from the database
	 */
	public CollectionEntry(
			final PersistentCollection<?> collection,
			final CollectionPersister loadedPersister,
			final Object loadedKey,
			final boolean ignore ) {
		this.ignore = ignore;

		//collection.clearDirty()

		this.loadedKey = loadedKey;

		this.loadedPersister = loadedPersister;
		this.role = ( loadedPersister == null ? null : loadedPersister.getRole() );

		collection.setSnapshot( loadedKey, role, null );

		//postInitialize() will be called after initialization
	}

	/**
	 * For uninitialized detached collections
	 */
	public CollectionEntry(CollectionPersister loadedPersister, Object loadedKey) {
		// detached collection wrappers that get found + reattached
		// during flush shouldn't be ignored
		ignore = false;

		//collection.clearDirty()

		this.loadedKey = loadedKey;
		this.loadedPersister = loadedPersister;
		this.role = ( loadedPersister == null ? null : loadedPersister.getRole() );
	}

	/**
	 * For initialized detached collections
	 */
	public CollectionEntry(PersistentCollection<?> collection, SessionFactoryImplementor factory) {
		// detached collections that get found + reattached
		// during flush shouldn't be ignored
		ignore = false;

		loadedKey = collection.getKey();
		role = collection.getRole();
		loadedPersister = factory.getRuntimeMetamodels().getMappingMetamodel().getCollectionDescriptor( NullnessUtil.castNonNull( role ) );

		snapshot = collection.getStoredSnapshot();
	}

	/**
	 * Used from custom serialization.
	 *
	 * @see #serialize
	 * @see #deserialize
	 */
	private CollectionEntry(
			@Nullable String role,
			Serializable snapshot,
			Object loadedKey,
			@Nullable SessionFactoryImplementor factory) {
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
	private void dirty(PersistentCollection<?> collection) throws HibernateException {

		final CollectionPersister loadedPersister = getLoadedPersister();
		final boolean forceDirty =
				collection.wasInitialized()
			&& !collection.isDirty() //optimization
			&& loadedPersister != null
			&& loadedPersister.isMutable() //optimization
			&& ( collection.isDirectlyAccessible() || loadedPersister.getElementType().isMutable() ) //optimization
			&& !collection.equalsSnapshot( loadedPersister );

		if ( forceDirty ) {
			collection.dirty();
		}

	}

	public void preFlush(PersistentCollection<?> collection) throws HibernateException {
		if ( loadedKey == null && collection.getKey() != null ) {
			loadedKey = collection.getKey();
		}

		final CollectionPersister loadedPersister = getLoadedPersister();
		boolean nonMutableChange = collection.isDirty()
				&& loadedPersister != null
				&& !loadedPersister.isMutable();
		if ( nonMutableChange ) {
			throw new HibernateException(
					"changed an immutable collection instance: " +
					collectionInfoString( NullnessUtil.castNonNull( loadedPersister ).getRole(), getLoadedKey() )
			);
		}

		dirty( collection );

		if ( LOG.isDebugEnabled() && collection.isDirty() && loadedPersister != null ) {
			LOG.debugf(
					"Collection dirty: %s",
					collectionInfoString( loadedPersister.getRole(), getLoadedKey() )
			);
		}

		setReached( false );
		setProcessed( false );

		setDoupdate( false );
		setDoremove( false );
		setDorecreate( false );
	}

	public void postInitialize(PersistentCollection<?> collection, SharedSessionContractImplementor session)
			throws HibernateException {
		final CollectionPersister loadedPersister = getLoadedPersister();
		snapshot = loadedPersister != null && loadedPersister.isMutable()
				? collection.getSnapshot( loadedPersister )
				: null;
		collection.setSnapshot( loadedKey, role, snapshot );
		if ( loadedPersister != null && session.getLoadQueryInfluencers().effectivelyBatchLoadable( loadedPersister ) ) {
			session.getPersistenceContextInternal()
					.getBatchFetchQueue()
					.removeBatchLoadableCollection( this );
		}
	}

	/**
	 * Called after a successful flush
	 */
	public void postFlush(PersistentCollection<?> collection) throws HibernateException {
		if ( isIgnore() ) {
			ignore = false;
		}
		else if ( !isProcessed() ) {
			throw new HibernateException( LOG.collectionNotProcessedByFlush( collection.getRole() ) );
		}
		collection.setSnapshot( loadedKey, role, snapshot );
	}

	/**
	 * Called after execution of an action
	 */
	public void afterAction(PersistentCollection<?> collection) {
		loadedKey = getCurrentKey();
		setLoadedPersister( getCurrentPersister() );

		boolean resnapshot = collection.wasInitialized()
				&&  ( isDoremove() || isDorecreate() || isDoupdate() );
		if ( resnapshot ) {
			snapshot = loadedPersister == null || !loadedPersister.isMutable() ?
					null :
					collection.getSnapshot( NullnessUtil.castNonNull( loadedPersister ) ); //re-snapshot
		}

		collection.postAction();
	}

	public @Nullable Object getKey() {
		return getLoadedKey();
	}

	public @Nullable String getRole() {
		return role;
	}

	public @Nullable Serializable getSnapshot() {
		return snapshot;
	}

	private boolean fromMerge;

	/**
	 * Reset the stored snapshot for both the persistent collection and this collection entry. 
	 * Used during the merge of detached collections.
	 * 
	 * @param collection the persistentcollection to be updated
	 * @param storedSnapshot the new stored snapshot
	 */
	public void resetStoredSnapshot(PersistentCollection<?> collection, Serializable storedSnapshot) {
		LOG.debugf("Reset storedSnapshot to %s for %s", storedSnapshot, this);

		if ( fromMerge ) {
			return; // EARLY EXIT!
		}

		snapshot = storedSnapshot;
		collection.setSnapshot( loadedKey, role, snapshot );
		fromMerge = true;
	}

	private void setLoadedPersister(@Nullable CollectionPersister persister) {
		loadedPersister = persister;
		setRole( persister == null ? null : persister.getRole() );
	}

	void afterDeserialize(@Nullable SessionFactoryImplementor factory) {
		if ( factory == null ) {
			loadedPersister = null;
		}
		else {
			loadedPersister = factory.getRuntimeMetamodels().getMappingMetamodel().getCollectionDescriptor( NullnessUtil.castNonNull( role ) );
		}
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

	public @Nullable CollectionPersister getCurrentPersister() {
		return currentPersister;
	}

	public void setCurrentPersister(@Nullable CollectionPersister currentPersister) {
		this.currentPersister = currentPersister;
	}

	/**
	 * This is only available late during the flush
	 * cycle
	 */
	public @Nullable Object getCurrentKey() {
		return currentKey;
	}

	public void setCurrentKey(@Nullable Object currentKey) {
		this.currentKey = currentKey;
	}

	/**
	 * This is only available late during the flush cycle
	 */
	public @Nullable CollectionPersister getLoadedPersister() {
		return loadedPersister;
	}

	public @Nullable Object getLoadedKey() {
		return loadedKey;
	}

	public void setRole(@Nullable String role) {
		this.role = role;
	}

	@Override
	public String toString() {
		String result = "CollectionEntry" +
				collectionInfoString( role, loadedKey );
		if ( currentPersister != null ) {
			result += "->" +
					collectionInfoString( currentPersister.getRole(), currentKey );
		}
		return result;
	}

	/**
	 * Get the collection orphans (entities which were removed from the collection)
	 */
	public Collection<?> getOrphans(String entityName, PersistentCollection<?> collection) throws HibernateException {
		if ( snapshot == null ) {
			throw new AssertionFailure( "no collection snapshot for orphan delete" );
		}
		return collection.getOrphans( snapshot, entityName );
	}

	public boolean isSnapshotEmpty(PersistentCollection<?> collection) {
		//TODO: does this really need to be here?
		//      does the collection already have
		//      it's own up-to-date snapshot?
		final CollectionPersister loadedPersister = getLoadedPersister();
		final Serializable snapshot = getSnapshot();
		return collection.wasInitialized()
			&& ( loadedPersister == null || loadedPersister.isMutable() )
			&& ( snapshot == null || collection.isSnapshotEmpty(snapshot) );
	}



	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
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
	 *
	 * @return The deserialized CollectionEntry
	 */
	public static CollectionEntry deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		return new CollectionEntry(
				(String) ois.readObject(),
				(Serializable) ois.readObject(),
				ois.readObject(),
				session == null ? null : session.getFactory()
		);
	}
}
