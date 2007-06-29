//$Id: EntityEntry.java 9283 2006-02-14 03:24:18Z steveebersole $
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
	private final Serializable id;
	private Object[] loadedState;
	private Object[] deletedState;
	private boolean existsInDatabase;
	private Object version;
	private transient EntityPersister persister; // for convenience to save some lookups
	private final EntityMode entityMode;
	private final String entityName;
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
		this.loadedState=loadedState;
		this.id=id;
		this.rowId=rowId;
		this.existsInDatabase=existsInDatabase;
		this.version=version;
		this.lockMode=lockMode;
		this.isBeingReplicated=disableVersionIncrement;
		this.loadedWithLazyPropertiesUnfetched = lazyPropertiesAreUnfetched;
		this.persister=persister;
		this.entityMode = entityMode;
		this.entityName = persister == null ?
				null : persister.getEntityName();
	}

	/**
	 * Used during custom deserialization
	 */
	private EntityEntry(
			final SessionFactoryImplementor factory,
			final String entityName,
			final Serializable id,
			final EntityMode entityMode,
			final Status status,
			final Object[] loadedState,
	        final Object[] deletedState,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final boolean isBeingReplicated,
			final boolean loadedWithLazyPropertiesUnfetched) {
		this.entityName = entityName;
		this.persister = factory.getEntityPersister( entityName );
		this.id = id;
		this.entityMode = entityMode;
		this.status = status;
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
		this.status = status;
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

	void afterDeserialize(SessionFactoryImplementor factory) {
		persister = factory.getEntityPersister( entityName );
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
	 * After actually updating the database, update the snapshot information,
	 * and escalate the lock mode
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
							.contains( new EntityKey( getId(), getPersister(), entityMode ) )
				);
	}
	
	public Object getLoadedValue(String propertyName) {
		int propertyIndex = ( (UniqueKeyLoadable) persister ).getPropertyIndex(propertyName);
		return loadedState[propertyIndex];
	}
	
	
	public boolean requiresDirtyCheck(Object entity) {
		
		boolean isMutableInstance = 
				status != Status.READ_ONLY && 
				persister.isMutable();
		
		return isMutableInstance && (
				getPersister().hasMutableProperties() ||
				!FieldInterceptionHelper.isInstrumented( entity ) ||
				FieldInterceptionHelper.extractFieldInterceptor( entity).isDirty()
			);
		
	}

	public void forceLocked(Object entity, Object nextVersion) {
		version = nextVersion;
		loadedState[ persister.getVersionProperty() ] = version;
		setLockMode( LockMode.FORCE );
		persister.setPropertyValue(
				entity,
		        getPersister().getVersionProperty(),
		        nextVersion,
		        entityMode
		);
	}

	public void setReadOnly(boolean readOnly, Object entity) {
		if (status!=Status.MANAGED && status!=Status.READ_ONLY) {
			throw new HibernateException("instance was not in a valid state");
		}
		if (readOnly) {
			setStatus(Status.READ_ONLY);
			loadedState = null;
		}
		else {
			setStatus(Status.MANAGED);
			loadedState = getPersister().getPropertyValues(entity, entityMode);
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
	 * @throws java.io.IOException
	 */
	void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( entityName );
		oos.writeObject( id );
		oos.writeObject( entityMode.toString() );
		oos.writeObject( status.toString() );
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
	 * @return The deserialized EntityEntry
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	static EntityEntry deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		return new EntityEntry(
				session.getFactory(),
		        ( String ) ois.readObject(),
				( Serializable ) ois.readObject(),
	            EntityMode.parse( ( String ) ois.readObject() ),
				Status.parse( ( String ) ois.readObject() ),
	            ( Object[] ) ois.readObject(),
	            ( Object[] ) ois.readObject(),
	            ( Object ) ois.readObject(),
	            LockMode.parse( ( String ) ois.readObject() ),
	            ois.readBoolean(),
	            ois.readBoolean(),
	            ois.readBoolean()
		);
	}
}
