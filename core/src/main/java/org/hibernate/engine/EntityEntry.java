/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.engine;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;

/**
 * We need an entry to tell us all about the current state
 * of an object with respect to its persistent state
 * 
 * @author Gavin King
 */
public final class EntityEntry implements Serializable {

	private LockMode lockMode;
	private Status status;
	private Status previousStatus;
	private final Serializable id;
	private Object[] loadedState;
	private Object[] deletedState;
	private boolean existsInDatabase;
	private Object version;
	private transient EntityPersister persister; // for convenience to save some lookups
	private final EntityMode entityMode;
	private final String entityName;
	private transient EntityKey cachedEntityKey; // cached EntityKey (lazy-initialized)
	private boolean isBeingReplicated;
	private boolean loadedWithLazyPropertiesUnfetched; //NOTE: this is not updated when properties are fetched lazily!
	private final transient Object rowId;

	EntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final EntityMode entityMode,
			final boolean disableVersionIncrement,
			final boolean lazyPropertiesAreUnfetched) {
		this.status=status;
		this.previousStatus = null;
		// only retain loaded state if the status is not Status.READ_ONLY
		if ( status != Status.READ_ONLY ) { this.loadedState = loadedState; }
		this.id=id;
		this.rowId=rowId;
		this.existsInDatabase=existsInDatabase;
		this.version=version;
		this.lockMode=lockMode;
		this.isBeingReplicated=disableVersionIncrement;
		this.loadedWithLazyPropertiesUnfetched = lazyPropertiesAreUnfetched;
		this.persister=persister;
		this.entityMode = entityMode;
		this.entityName = persister == null ? null : persister.getEntityName();
	}

	private EntityEntry(
			final SessionFactoryImplementor factory,
			final String entityName,
			final Serializable id,
			final EntityMode entityMode,
			final Status status,
			final Status previousStatus,
			final Object[] loadedState,
	        final Object[] deletedState,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final boolean isBeingReplicated,
			final boolean loadedWithLazyPropertiesUnfetched) {
		// Used during custom deserialization
		this.entityName = entityName;
		this.persister = ( factory == null ? null : factory.getEntityPersister( entityName ) );
		this.id = id;
		this.entityMode = entityMode;
		this.status = status;
		this.previousStatus = previousStatus;
		this.loadedState = loadedState;
		this.deletedState = deletedState;
		this.version = version;
		this.lockMode = lockMode;
		this.existsInDatabase = existsInDatabase;
		this.isBeingReplicated = isBeingReplicated;
		this.loadedWithLazyPropertiesUnfetched = loadedWithLazyPropertiesUnfetched;
		this.rowId = null; // this is equivalent to the old behavior...
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	public void setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		if (status==Status.READ_ONLY) {
			loadedState = null; //memory optimization
		}
		if ( this.status != status ) {
			this.previousStatus = this.status;
			this.status = status;
		}
	}

	public Serializable getId() {
		return id;
	}

	public Object[] getLoadedState() {
		return loadedState;
	}

	public Object[] getDeletedState() {
		return deletedState;
	}

	public void setDeletedState(Object[] deletedState) {
		this.deletedState = deletedState;
	}

	public boolean isExistsInDatabase() {
		return existsInDatabase;
	}

	public Object getVersion() {
		return version;
	}

	public EntityPersister getPersister() {
		return persister;
	}

	/**
	 * Get the EntityKey based on this EntityEntry.
	 * @return the EntityKey
	 * @throws  IllegalStateException if getId() is null
	 */
	public EntityKey getEntityKey() {
		if ( cachedEntityKey == null ) {
			if ( getId() == null ) {
				throw new IllegalStateException( "cannot generate an EntityKey when id is null.");
			}
			cachedEntityKey = new EntityKey( getId(), getPersister(), entityMode );
		}
		return cachedEntityKey;
	}

	public String getEntityName() {
		return entityName;
	}

	public boolean isBeingReplicated() {
		return isBeingReplicated;
	}
	
	public Object getRowId() {
		return rowId;
	}
	
	/**
	 * Handle updating the internal state of the entry after actually performing
	 * the database update.  Specifically we update the snapshot information and
	 * escalate the lock mode
	 *
	 * @param entity The entity instance
	 * @param updatedState The state calculated after the update (becomes the
	 * new {@link #getLoadedState() loaded state}.
	 * @param nextVersion The new version.
	 */
	public void postUpdate(Object entity, Object[] updatedState, Object nextVersion) {
		this.loadedState = updatedState;
		setLockMode(LockMode.WRITE);

		if ( getPersister().isVersioned() ) {
			this.version = nextVersion;
			getPersister().setPropertyValue(
					entity,
					getPersister().getVersionProperty(), 
					nextVersion, 
					entityMode 
			);
		}

		FieldInterceptionHelper.clearDirty( entity );
	}

	/**
	 * After actually deleting a row, record the fact that the instance no longer
	 * exists in the database
	 */
	public void postDelete() {
		previousStatus = status;
		status = Status.GONE;
		existsInDatabase = false;
	}
	
	/**
	 * After actually inserting a row, record the fact that the instance exists on the 
	 * database (needed for identity-column key generation)
	 */
	public void postInsert() {
		existsInDatabase = true;
	}
	
	public boolean isNullifiable(boolean earlyInsert, SessionImplementor session) {
		return getStatus() == Status.SAVING || (
				earlyInsert ?
						!isExistsInDatabase() :
						session.getPersistenceContext().getNullifiableEntityKeys()
							.contains( getEntityKey() )
				);
	}
	
	public Object getLoadedValue(String propertyName) {
		int propertyIndex = ( (UniqueKeyLoadable) persister ).getPropertyIndex(propertyName);
		return loadedState[propertyIndex];
	}

	public boolean requiresDirtyCheck(Object entity) {		
		return isModifiableEntity() && (
				getPersister().hasMutableProperties() ||
				!FieldInterceptionHelper.isInstrumented( entity ) ||
				FieldInterceptionHelper.extractFieldInterceptor( entity).isDirty()
			);
	}

	/**
	 * Can the entity be modified?
	 *
	 * The entity is modifiable if all of the following are true:
	 * <ul>
	 * <li>the entity class is mutable</li>
	 * <li>the entity is not read-only</li>
	 * <li>if the current status is Status.DELETED, then the entity was not read-only when it was deleted</li>
	 * </ul>
	 * @return true, if the entity is modifiable; false, otherwise,
	 */
	public boolean isModifiableEntity() {
		return ( status != Status.READ_ONLY ) &&
				! ( status == Status.DELETED && previousStatus == Status.READ_ONLY ) &&
				getPersister().isMutable();
	}

	public void forceLocked(Object entity, Object nextVersion) {
		version = nextVersion;
		loadedState[ persister.getVersionProperty() ] = version;
		//noinspection deprecation
		setLockMode( LockMode.FORCE );  // TODO:  use LockMode.PESSIMISTIC_FORCE_INCREMENT
		persister.setPropertyValue(
				entity,
		        getPersister().getVersionProperty(),
		        nextVersion,
		        entityMode
		);
	}

	public boolean isReadOnly() {
		if (status != Status.MANAGED && status != Status.READ_ONLY) {
			throw new HibernateException("instance was not in a valid state");
		}
		return status == Status.READ_ONLY;
	}

	public void setReadOnly(boolean readOnly, Object entity) {
		if ( readOnly == isReadOnly() ) {
			// simply return since the status is not being changed
			return;
		}
		if ( readOnly ) {
			setStatus( Status.READ_ONLY );
			loadedState = null;
		}
		else {
			if ( ! persister.isMutable() ) {
				throw new IllegalStateException( "Cannot make an immutable entity modifiable." );
			}
			setStatus( Status.MANAGED );
			loadedState = getPersister().getPropertyValues( entity, entityMode );
		}
	}
	
	public String toString() {
		return "EntityEntry" + 
				MessageHelper.infoString(entityName, id) + 
				'(' + status + ')';
	}

	public boolean isLoadedWithLazyPropertiesUnfetched() {
		return loadedWithLazyPropertiesUnfetched;
	}

	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 *
	 * @throws IOException If a stream error occurs
	 */
	void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( entityName );
		oos.writeObject( id );
		oos.writeObject( entityMode.toString() );
		oos.writeObject( status.toString() );
		oos.writeObject( ( previousStatus == null ? "" : previousStatus.toString() ) );
		// todo : potentially look at optimizing these two arrays
		oos.writeObject( loadedState );
		oos.writeObject( deletedState );
		oos.writeObject( version );
		oos.writeObject( lockMode.toString() );
		oos.writeBoolean( existsInDatabase );
		oos.writeBoolean( isBeingReplicated );
		oos.writeBoolean( loadedWithLazyPropertiesUnfetched );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param session The session being deserialized.
	 *
	 * @return The deserialized EntityEntry
	 *
	 * @throws IOException If a stream error occurs
	 * @throws ClassNotFoundException If any of the classes declared in the stream
	 * cannot be found
	 */
	static EntityEntry deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		String previousStatusString = null;
		return new EntityEntry(
				( session == null ? null : session.getFactory() ),
		        ( String ) ois.readObject(),
				( Serializable ) ois.readObject(),
	            EntityMode.parse( ( String ) ois.readObject() ),
				Status.parse( ( String ) ois.readObject() ),
				( ( previousStatusString = ( String ) ois.readObject() ).length() == 0 ?
							null :
							Status.parse( previousStatusString ) 
				),
	            ( Object[] ) ois.readObject(),
	            ( Object[] ) ois.readObject(),
	            ois.readObject(),
	            LockMode.parse( ( String ) ois.readObject() ),
	            ois.readBoolean(),
	            ois.readBoolean(),
	            ois.readBoolean()
		);
	}
}
