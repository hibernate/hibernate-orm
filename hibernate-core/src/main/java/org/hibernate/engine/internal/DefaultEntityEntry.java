/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;

/**
 * Default implementation of {@link org.hibernate.engine.spi.EntityEntry}.
 * <p>
 * Implementation Warning: Hibernate needs to instantiate a high amount of instances of this class,
 * therefore we need to take care of its impact on memory consumption.
 *
 * @author Gavin King
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Gunnar Morling
 * @author Sanne Grinovero  <sanne@hibernate.org>
 */
public final class DefaultEntityEntry implements Serializable, EntityEntry {
	private final Serializable id;
	private Object[] loadedState;
	private Object version;
	private final EntityPersister persister; // permanent but we only need the entityName state in a non transient way
	private transient EntityKey cachedEntityKey; // cached EntityKey (lazy-initialized)
	private final transient Object rowId;
	private final transient PersistenceContext persistenceContext;
	private EntityEntryExtraState next;

	/**
	 * Holds several boolean and enum typed attributes in a very compact manner. Enum values are stored in 4 bits
	 * (where 0 represents {@code null}, and each enum value is represented by its ordinal value + 1), thus allowing
	 * for up to 15 values per enum. Boolean values are stored in one bit.
	 * <p>
	 * The value is structured as follows:
	 *
	 * <pre>
	 * 1 - Lock mode
	 * 2 - Status
	 * 3 - Previous Status
	 * 4 - existsInDatabase
	 * 5 - isBeingReplicated
	 * 6 - loadedWithLazyPropertiesUnfetched; NOTE: this is not updated when properties are fetched lazily!
	 *
	 * 0000 0000 | 0000 0000 | 0654 3333 | 2222 1111
	 * </pre>
	 * Use {@link #setCompressedValue(org.hibernate.engine.internal.DefaultEntityEntry.EnumState, Enum)},
	 * {@link #getCompressedValue(org.hibernate.engine.internal.DefaultEntityEntry.EnumState)} etc
	 * to access the enums and booleans stored in this value.
	 * <p>
	 * Representing enum values by their ordinal value is acceptable for our case as this value itself is never
	 * serialized or deserialized and thus is not affected should ordinal values change.
	 */
	private transient int compressedState;

	public DefaultEntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement,
			final boolean lazyPropertiesAreUnfetched,
			final PersistenceContext persistenceContext) {
		setCompressedValue( EnumState.STATUS, status );
		// not useful strictly speaking but more explicit
		setCompressedValue( EnumState.PREVIOUS_STATUS, null );
		// only retain loaded state if the status is not Status.READ_ONLY
		if ( status != Status.READ_ONLY ) {
			this.loadedState = loadedState;
		}
		this.id=id;
		this.rowId=rowId;
		setCompressedValue( BooleanState.EXISTS_IN_DATABASE, existsInDatabase );
		this.version=version;
		setCompressedValue( EnumState.LOCK_MODE, lockMode );
		setCompressedValue( BooleanState.IS_BEING_REPLICATED, disableVersionIncrement );
		setCompressedValue( BooleanState.LOADED_WITH_LAZY_PROPERTIES_UNFETCHED, lazyPropertiesAreUnfetched );
		this.persister=persister;
		this.persistenceContext = persistenceContext;
	}

	/**
	 * This for is used during custom deserialization handling
	 */
	@SuppressWarnings( {"JavaDoc"})
	private DefaultEntityEntry(
			final SessionFactoryImplementor factory,
			final String entityName,
			final Serializable id,
			final Status status,
			final Status previousStatus,
			final Object[] loadedState,
			final Object[] deletedState,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final boolean isBeingReplicated,
			final boolean loadedWithLazyPropertiesUnfetched,
			final PersistenceContext persistenceContext) {
		this.persister = ( factory == null ? null : factory.getEntityPersister( entityName ) );
		this.id = id;
		setCompressedValue( EnumState.STATUS, status );
		setCompressedValue( EnumState.PREVIOUS_STATUS, previousStatus );
		this.loadedState = loadedState;
		setDeletedState( deletedState );
		this.version = version;
		setCompressedValue( EnumState.LOCK_MODE, lockMode );
		setCompressedValue( BooleanState.EXISTS_IN_DATABASE, existsInDatabase );
		setCompressedValue( BooleanState.IS_BEING_REPLICATED, isBeingReplicated );
		setCompressedValue( BooleanState.LOADED_WITH_LAZY_PROPERTIES_UNFETCHED, loadedWithLazyPropertiesUnfetched );
		// this is equivalent to the old behavior...
		this.rowId = null;
		this.persistenceContext = persistenceContext;
	}

	@Override public LockMode getLockMode() {
		return getCompressedValue( EnumState.LOCK_MODE );
	}

	@Override public void setLockMode(LockMode lockMode) {
		setCompressedValue( EnumState.LOCK_MODE, lockMode );
	}

	@Override public Status getStatus() {
		return getCompressedValue( EnumState.STATUS );
	}

	private Status getPreviousStatus() {
		return getCompressedValue( EnumState.PREVIOUS_STATUS );
	}

	@Override public void setStatus(Status status) {
		if (status==Status.READ_ONLY) {
			loadedState = null; //memory optimization
		}

		Status currentStatus = this.getStatus();

		if ( currentStatus != status ) {
			setCompressedValue( EnumState.PREVIOUS_STATUS, currentStatus );
			setCompressedValue( EnumState.STATUS, status );
		}
	}

	@Override public Serializable getId() {
		return id;
	}

	@Override public Object[] getLoadedState() {
		return loadedState;
	}

	private static final Object[] DEFAULT_DELETED_STATE = null;

	@Override public Object[] getDeletedState() {
		EntityEntryExtraStateHolder extra = getExtraState( EntityEntryExtraStateHolder.class );
		return extra != null ? extra.getDeletedState() : DEFAULT_DELETED_STATE;
	}

	@Override public void setDeletedState(Object[] deletedState) {
		EntityEntryExtraStateHolder extra = getExtraState( EntityEntryExtraStateHolder.class );
		if ( extra == null && deletedState == DEFAULT_DELETED_STATE ) {
			//this is the default value and we do not store the extra state
			return;
		}
		if ( extra == null ) {
			extra = new EntityEntryExtraStateHolder();
			addExtraState( extra );
		}
		extra.setDeletedState( deletedState );
	}

	@Override public boolean isExistsInDatabase() {
		return getCompressedValue( BooleanState.EXISTS_IN_DATABASE );
	}

	@Override public Object getVersion() {
		return version;
	}

	@Override public EntityPersister getPersister() {
		return persister;
	}

	/**
	 * Get the EntityKey based on this EntityEntry.
	 * @return the EntityKey
	 * @throws  IllegalStateException if getId() is null
	 */
	@Override public EntityKey getEntityKey() {
		if ( cachedEntityKey == null ) {
			if ( getId() == null ) {
				throw new IllegalStateException( "cannot generate an EntityKey when id is null.");
			}
			cachedEntityKey = new EntityKey( getId(), getPersister() );
		}
		return cachedEntityKey;
	}

	@Override public String getEntityName() {
		return persister == null ? null : persister.getEntityName();
	}

	@Override public boolean isBeingReplicated() {
		return getCompressedValue( BooleanState.IS_BEING_REPLICATED );
	}

	@Override public Object getRowId() {
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
	@Override public void postUpdate(Object entity, Object[] updatedState, Object nextVersion) {
		this.loadedState = updatedState;
		setLockMode( LockMode.WRITE );

		if ( getPersister().isVersioned() ) {
			this.version = nextVersion;
			getPersister().setPropertyValue( entity, getPersister().getVersionProperty(), nextVersion );
		}

		if ( getPersister().getInstrumentationMetadata().isInstrumented() ) {
			final FieldInterceptor interceptor = getPersister().getInstrumentationMetadata().extractInterceptor( entity );
			if ( interceptor != null ) {
				interceptor.clearDirty();
			}
		}

		if( entity instanceof SelfDirtinessTracker ) {
			((SelfDirtinessTracker) entity).$$_hibernate_clearDirtyAttributes();
		}

		persistenceContext.getSession()
				.getFactory()
				.getCustomEntityDirtinessStrategy()
				.resetDirty( entity, getPersister(), (Session) persistenceContext.getSession() );
	}

	/**
	 * After actually deleting a row, record the fact that the instance no longer
	 * exists in the database
	 */
	@Override public void postDelete() {
		setCompressedValue( EnumState.PREVIOUS_STATUS, getStatus() );
		setCompressedValue( EnumState.STATUS, Status.GONE );
		setCompressedValue( BooleanState.EXISTS_IN_DATABASE, false );
	}

	/**
	 * After actually inserting a row, record the fact that the instance exists on the 
	 * database (needed for identity-column key generation)
	 */
	@Override public void postInsert(Object[] insertedState) {
		setCompressedValue( BooleanState.EXISTS_IN_DATABASE, true );
	}

	@Override public boolean isNullifiable(boolean earlyInsert, SessionImplementor session) {
		if ( getStatus() == Status.SAVING ) {
			return true;
		}
		else if ( earlyInsert ) {
			return !isExistsInDatabase();
		}
		else {
			return session.getPersistenceContext().getNullifiableEntityKeys().contains( getEntityKey() );
		}
	}

	@Override public Object getLoadedValue(String propertyName) {
		if ( loadedState == null || propertyName == null ) {
			return null;
		}
		else {
			int propertyIndex = ( (UniqueKeyLoadable) persister ).getPropertyIndex( propertyName );
			return loadedState[propertyIndex];
		}
	}

	/**
	 * Not sure this is the best method name, but the general idea here is to return {@code true} if the entity can
	 * possibly be dirty.  This can only be the case if it is in a modifiable state (not read-only/deleted) and it
	 * either has mutable properties or field-interception is not telling us it is dirty.  Clear as mud? :/
	 *
	 * A name like canPossiblyBeDirty might be better
	 *
	 * @param entity The entity to test
	 *
	 * @return {@code true} indicates that the entity could possibly be dirty and that dirty check
	 * should happen; {@code false} indicates there is no way the entity can be dirty
	 */
	@Override public boolean requiresDirtyCheck(Object entity) {
		return isModifiableEntity()
				&& ( !isUnequivocallyNonDirty( entity ) );
	}

	@SuppressWarnings( {"SimplifiableIfStatement"})
	private boolean isUnequivocallyNonDirty(Object entity) {

		if(entity instanceof SelfDirtinessTracker)
			return ((SelfDirtinessTracker) entity).$$_hibernate_hasDirtyAttributes();

		final CustomEntityDirtinessStrategy customEntityDirtinessStrategy =
				persistenceContext.getSession().getFactory().getCustomEntityDirtinessStrategy();
		if ( customEntityDirtinessStrategy.canDirtyCheck( entity, getPersister(), (Session) persistenceContext.getSession() ) ) {
			return ! customEntityDirtinessStrategy.isDirty( entity, getPersister(), (Session) persistenceContext.getSession() );
		}

		if ( getPersister().hasMutableProperties() ) {
			return false;
		}

		if ( getPersister().getInstrumentationMetadata().isInstrumented() ) {
			// the entity must be instrumented (otherwise we cant check dirty flag) and the dirty flag is false
			return ! getPersister().getInstrumentationMetadata().extractInterceptor( entity ).isDirty();
		}

		return false;
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
	@Override public boolean isModifiableEntity() {
		Status status = getStatus();
		Status previousStatus = getPreviousStatus();
		return getPersister().isMutable()
				&& status != Status.READ_ONLY
				&& ! ( status == Status.DELETED && previousStatus == Status.READ_ONLY );
	}

	@Override public void forceLocked(Object entity, Object nextVersion) {
		version = nextVersion;
		loadedState[ persister.getVersionProperty() ] = version;
		// TODO:  use LockMode.PESSIMISTIC_FORCE_INCREMENT
		//noinspection deprecation
		setLockMode( LockMode.FORCE );
		persister.setPropertyValue( entity, getPersister().getVersionProperty(), nextVersion );
	}

	@Override public boolean isReadOnly() {
		Status status = getStatus();
		if ( status != Status.MANAGED && status != Status.READ_ONLY ) {
			throw new HibernateException( "instance was not in a valid state" );
		}
		return status == Status.READ_ONLY;
	}

	@Override public void setReadOnly(boolean readOnly, Object entity) {
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
			loadedState = getPersister().getPropertyValues( entity );
			persistenceContext.getNaturalIdHelper().manageLocalNaturalIdCrossReference(
					persister,
					id,
					loadedState,
					null,
					CachedNaturalIdValueSource.LOAD
			);
		}
	}

	@Override
	public String toString() {
		return "DefaultEntityEntry" + MessageHelper.infoString( getPersister().getEntityName(), id ) + '(' + getStatus() + ')';
	}

	@Override public boolean isLoadedWithLazyPropertiesUnfetched() {
		return getCompressedValue( BooleanState.LOADED_WITH_LAZY_PROPERTIES_UNFETCHED );
	}

	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 *
	 * @throws java.io.IOException If a stream error occurs
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		Status previousStatus = getPreviousStatus();
		oos.writeObject( getEntityName() );
		oos.writeObject( id );
		oos.writeObject( getStatus().name() );
		oos.writeObject( (previousStatus == null ? "" : previousStatus.name()) );
		// todo : potentially look at optimizing these two arrays
		oos.writeObject( getLoadedState() );
		oos.writeObject( getDeletedState() );
		oos.writeObject( getVersion() );
		oos.writeObject( getLockMode().toString() );
		oos.writeBoolean( isExistsInDatabase() );
		oos.writeBoolean( isBeingReplicated() );
		oos.writeBoolean( isLoadedWithLazyPropertiesUnfetched() );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param persistenceContext The context being deserialized.
	 *
	 * @return The deserialized EntityEntry
	 *
	 * @throws java.io.IOException If a stream error occurs
	 * @throws ClassNotFoundException If any of the classes declared in the stream
	 * cannot be found
	 */
	public static EntityEntry deserialize(
			ObjectInputStream ois,
			PersistenceContext persistenceContext) throws IOException, ClassNotFoundException {
		String previousStatusString;
		return new DefaultEntityEntry(
				persistenceContext.getSession().getFactory(),
				(String) ois.readObject(),
				(Serializable) ois.readObject(),
				Status.valueOf( (String) ois.readObject() ),
				( previousStatusString = (String) ois.readObject() ).length() == 0
						? null
						: Status.valueOf( previousStatusString ),
				(Object[]) ois.readObject(),
				(Object[]) ois.readObject(),
				ois.readObject(),
				LockMode.valueOf( (String) ois.readObject() ),
				ois.readBoolean(),
				ois.readBoolean(),
				ois.readBoolean(),
				persistenceContext
		);
	}

	//the following methods are handling extraState contracts.
	//they are not shared by a common superclass to avoid alignment padding
	//we are trading off duplication for padding efficiency
	@Override
	public void addExtraState(EntityEntryExtraState extraState) {
		if ( next == null ) {
			next = extraState;
		}
		else {
			next.addExtraState( extraState );
		}
	}

	@Override
	public <T extends EntityEntryExtraState> T getExtraState(Class<T> extraStateType) {
		if ( next == null ) {
			return null;
		}
		if ( extraStateType.isAssignableFrom( next.getClass() ) ) {
			return (T) next;
		}
		else {
			return next.getExtraState( extraStateType );
		}
	}
	/**
	 * Saves the value for the given enum property.
	 *
	 * @param state
	 *            identifies the value to store
	 * @param value
	 *            the value to store; The caller must make sure that it matches
	 *            the given identifier
	 */
	private <E extends Enum<E>> void setCompressedValue(EnumState<E> state, E value) {
		// reset the bits for the given property to 0
		compressedState &= state.getUnsetMask();
		// store the numeric representation of the enum value at the right offset
		compressedState |= ( state.getValue( value ) << state.getOffset() );
	}

	/**
	 * Gets the current value of the given enum property.
	 *
	 * @param state
	 *            identifies the value to store
	 * @return the current value of the specified property
	 */
	private <E extends Enum<E>> E getCompressedValue(EnumState<E> state) {
		// restore the numeric value from the bits at the right offset and return the corresponding enum constant
		int index = ( ( compressedState & state.getMask() ) >> state.getOffset() ) - 1;
		return index == - 1 ? null : state.getEnumConstants()[index];
	}

	/**
	 * Saves the value for the given boolean flag.
	 *
	 * @param state
	 *            identifies the value to store
	 * @param value
	 *            the value to store
	 */
	private void setCompressedValue(BooleanState state, boolean value) {
		compressedState &= state.getUnsetMask();
		compressedState |= ( state.getValue( value ) << state.getOffset() );
	}

	/**
	 * Gets the current value of the given boolean flag.
	 *
	 * @param state
	 *            identifies the value to store
	 * @return the current value of the specified flag
	 */
	private boolean getCompressedValue(BooleanState state) {
		return ( ( compressedState & state.getMask() ) >> state.getOffset() ) == 1;
	}

	/**
	 * Represents an enum value stored within a number value, using four bits starting at a specified offset.
	 *
	 * @author Gunnar Morling
	 */
	private static class EnumState<E extends Enum<E>> {

		private static final EnumState<LockMode> LOCK_MODE = new EnumState<LockMode>( 0, LockMode.class );
		private static final EnumState<Status> STATUS = new EnumState<Status>( 4, Status.class );
		private static final EnumState<Status> PREVIOUS_STATUS = new EnumState<Status>( 8, Status.class );

		private final int offset;
		private final E[] enumConstants;
		private final int mask;
		private final int unsetMask;

		private EnumState(int offset, Class<E> enumType) {
			E[] enumConstants = enumType.getEnumConstants();

			// In case any of the enums cannot be stored in 4 bits anymore, we'd have to re-structure the compressed
			// state int
			if ( enumConstants.length > 15 ) {
				throw new AssertionFailure( "Cannot store enum type " + enumType.getName() + " in compressed state as"
						+ " it has too many values." );
			}

			this.offset = offset;
			this.enumConstants = enumConstants;

			// a mask for reading the four bits, starting at the right offset
			this.mask = 0xF << offset;

			// a mask for setting the four bits at the right offset to 0
			this.unsetMask = 0xFFFF & ~mask;
		}

		/**
		 * Returns the numeric value to be stored for the given enum value.
		 */
		private int getValue(E value) {
			return value != null ? value.ordinal() + 1 : 0;
		}

		/**
		 * Returns the offset within the number value at which this enum value is stored.
		 */
		private int getOffset() {
			return offset;
		}

		/**
		 * Returns the bit mask for reading this enum value from the number value storing it.
		 */
		private int getMask() {
			return mask;
		}

		/**
		 * Returns the bit mask for resetting this enum value from the number value storing it.
		 */
		private int getUnsetMask() {
			return unsetMask;
		}

		/**
		 * Returns the constants of the represented enum which is cached for performance reasons.
		 */
		private E[] getEnumConstants() {
			return enumConstants;
		}
	}

	/**
	 * Represents a boolean flag stored within a number value, using one bit at a specified offset.
	 *
	 * @author Gunnar Morling
	 */
	private enum BooleanState {

		EXISTS_IN_DATABASE(13),
		IS_BEING_REPLICATED(14),
		LOADED_WITH_LAZY_PROPERTIES_UNFETCHED(15);

		private final int offset;
		private final int mask;
		private final int unsetMask;

		private BooleanState(int offset) {
			this.offset = offset;
			this.mask = 0x1 << offset;
			this.unsetMask = 0xFFFF & ~mask;
		}

		private int getValue(boolean value) {
			return value ? 1 : 0;
		}

		/**
		 * Returns the offset within the number value at which this boolean flag is stored.
		 */
		private int getOffset() {
			return offset;
		}

		/**
		 * Returns the bit mask for reading this flag from the number value storing it.
		 */
		private int getMask() {
			return mask;
		}

		/**
		 * Returns the bit mask for resetting this flag from the number value storing it.
		 */
		private int getUnsetMask() {
			return unsetMask;
		}
	}
}
