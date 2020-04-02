/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;

/**
 * A base implementation of EntityEntry
 *
 * @author Gavin King
 * @author <a href="mailto:emmanuel@hibernate.org">Emmanuel Bernard</a>
 * @author Gunnar Morling
 * @author <a href="mailto:sanne@hibernate.org">Sanne Grinovero </a>
 */
public abstract class AbstractEntityEntry implements Serializable, EntityEntry {
	protected final Serializable id;
	protected Object[] loadedState;
	protected Object version;
	protected final EntityPersister persister; // permanent but we only need the entityName state in a non transient way
	protected transient EntityKey cachedEntityKey; // cached EntityKey (lazy-initialized)
	protected final transient Object rowId;
	protected final transient PersistenceContext persistenceContext;
	protected EntityEntryExtraState next;

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
	 * Use {@link #setCompressedValue(org.hibernate.engine.internal.AbstractEntityEntry.EnumState, Enum)},
	 * {@link #getCompressedValue(org.hibernate.engine.internal.AbstractEntityEntry.EnumState)} etc
	 * to access the enums and booleans stored in this value.
	 * <p>
	 * Representing enum values by their ordinal value is acceptable for our case as this value itself is never
	 * serialized or deserialized and thus is not affected should ordinal values change.
	 */
	private transient int compressedState;

	/**
	 * @deprecated the tenantId and entityMode parameters where removed: this constructor accepts but ignores them.
	 * Use the other constructor!
	 */
	@Deprecated
	public AbstractEntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final EntityMode entityMode,
			final String tenantId,
			final boolean disableVersionIncrement,
			final PersistenceContext persistenceContext) {
		this( status, loadedState, rowId, id, version, lockMode, existsInDatabase,
				persister,disableVersionIncrement, persistenceContext
		);
	}

	public AbstractEntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement,
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
		this.persister=persister;
		this.persistenceContext = persistenceContext;
	}

	/**
	 * This for is used during custom deserialization handling
	 */
	@SuppressWarnings( {"JavaDoc"})
	protected AbstractEntityEntry(
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
		this.rowId = null; // this is equivalent to the old behavior...
		this.persistenceContext = persistenceContext;
	}

	@Override
	public LockMode getLockMode() {
		return getCompressedValue( EnumState.LOCK_MODE );
	}

	@Override
	public void setLockMode(LockMode lockMode) {
		setCompressedValue( EnumState.LOCK_MODE, lockMode );
	}


	@Override
	public Status getStatus() {
		return getCompressedValue( EnumState.STATUS );
	}

	private Status getPreviousStatus() {
		return getCompressedValue( EnumState.PREVIOUS_STATUS );
	}

	@Override
	public void setStatus(Status status) {
		if ( status == Status.READ_ONLY ) {
			//memory optimization
			loadedState = null;
		}

		final Status currentStatus = this.getStatus();

		if ( currentStatus != status ) {
			setCompressedValue( EnumState.PREVIOUS_STATUS, currentStatus );
			setCompressedValue( EnumState.STATUS, status );
		}
	}

	@Override
	public Serializable getId() {
		return id;
	}

	@Override
	public Object[] getLoadedState() {
		return loadedState;
	}

	private static final Object[] DEFAULT_DELETED_STATE = null;

	@Override
	public Object[] getDeletedState() {
		final EntityEntryExtraStateHolder extra = getExtraState( EntityEntryExtraStateHolder.class );
		return extra != null ? extra.getDeletedState() : DEFAULT_DELETED_STATE;
	}

	@Override
	public void setDeletedState(Object[] deletedState) {
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

	@Override
	public boolean isExistsInDatabase() {
		return getCompressedValue( BooleanState.EXISTS_IN_DATABASE );
	}

	@Override
	public Object getVersion() {
		return version;
	}

	@Override
	public EntityPersister getPersister() {
		return persister;
	}

	@Override
	public EntityKey getEntityKey() {
		if ( cachedEntityKey == null ) {
			if ( getId() == null ) {
				throw new IllegalStateException( "cannot generate an EntityKey when id is null.");
			}
			cachedEntityKey = new EntityKey( getId(), getPersister() );
		}
		return cachedEntityKey;
	}

	@Override
	public String getEntityName() {
		return persister == null ? null : persister.getEntityName();

	}

	@Override
	public boolean isBeingReplicated() {
		return getCompressedValue( BooleanState.IS_BEING_REPLICATED );
	}

	@Override
	public Object getRowId() {
		return rowId;
	}

	@Override
	public void postUpdate(Object entity, Object[] updatedState, Object nextVersion) {
		this.loadedState = updatedState;
		setLockMode( LockMode.WRITE );

		if ( getPersister().isVersioned() ) {
			this.version = nextVersion;
			getPersister().setPropertyValue( entity, getPersister().getVersionProperty(), nextVersion );
		}

		if( entity instanceof SelfDirtinessTracker ) {
			( (SelfDirtinessTracker) entity ).$$_hibernate_clearDirtyAttributes();
		}

		getPersistenceContext().getSession()
				.getFactory()
				.getCustomEntityDirtinessStrategy()
				.resetDirty( entity, getPersister(), (Session) getPersistenceContext().getSession() );
	}

	@Override
	public void postDelete() {
		setCompressedValue( EnumState.PREVIOUS_STATUS, getStatus() );
		setCompressedValue( EnumState.STATUS, Status.GONE );
		setCompressedValue( BooleanState.EXISTS_IN_DATABASE, false );
	}

	@Override
	public void postInsert(Object[] insertedState) {
		setCompressedValue( BooleanState.EXISTS_IN_DATABASE, true );
	}

	@Override
	public boolean isNullifiable(boolean earlyInsert, SharedSessionContractImplementor session) {
		if ( getStatus() == Status.SAVING ) {
			return true;
		}
		else if ( earlyInsert ) {
			return !isExistsInDatabase();
		}
		else {
			return session.getPersistenceContextInternal().containsNullifiableEntityKey( this::getEntityKey );
		}
	}

	@Override
	public Object getLoadedValue(String propertyName) {
		if ( loadedState == null || propertyName == null ) {
			return null;
		}
		else {
			final int propertyIndex = ( (UniqueKeyLoadable) persister ).getPropertyIndex( propertyName );
			return loadedState[propertyIndex];
		}
	}

	@Override
	public void overwriteLoadedStateCollectionValue(String propertyName, PersistentCollection collection) {
		// nothing to do if status is READ_ONLY
		if ( getStatus() != Status.READ_ONLY ) {
			assert propertyName != null;
			assert loadedState != null;

			final int propertyIndex = ( (UniqueKeyLoadable) persister ).getPropertyIndex( propertyName );
			loadedState[propertyIndex] = collection;
		}
	}

	@Override
	public boolean requiresDirtyCheck(Object entity) {
		return isModifiableEntity()
				&& ( !isUnequivocallyNonDirty( entity ) );
	}

	@SuppressWarnings( {"SimplifiableIfStatement"})
	private boolean isUnequivocallyNonDirty(Object entity) {
		if ( entity instanceof SelfDirtinessTracker ) {
			return ! persister.hasCollections() && ! ( (SelfDirtinessTracker) entity ).$$_hibernate_hasDirtyAttributes();
		}

		if ( entity instanceof PersistentAttributeInterceptable ) {
			final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) entity;
			final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				// we never have to check an uninitialized proxy
				return true;
			}
		}

		final CustomEntityDirtinessStrategy customEntityDirtinessStrategy =
				getPersistenceContext().getSession().getFactory().getCustomEntityDirtinessStrategy();
		if ( customEntityDirtinessStrategy.canDirtyCheck( entity, getPersister(), (Session) getPersistenceContext().getSession() ) ) {
			return ! customEntityDirtinessStrategy.isDirty( entity, getPersister(), (Session) getPersistenceContext().getSession() );
		}

		if ( getPersister().hasMutableProperties() ) {
			return false;
		}

		return false;
	}

	@Override
	public boolean isModifiableEntity() {
		final Status status = getStatus();
		final Status previousStatus = getPreviousStatus();
		return getPersister().isMutable()
				&& status != Status.READ_ONLY
				&& ! ( status == Status.DELETED && previousStatus == Status.READ_ONLY );
	}

	@Override
	public void forceLocked(Object entity, Object nextVersion) {
		version = nextVersion;
		loadedState[ persister.getVersionProperty() ] = version;
		// TODO:  use LockMode.PESSIMISTIC_FORCE_INCREMENT
		//noinspection deprecation
		setLockMode( LockMode.FORCE );
		persister.setPropertyValue( entity, getPersister().getVersionProperty(), nextVersion );
	}

	@Override
	public boolean isReadOnly() {
		final Status status = getStatus();
		if (status != Status.MANAGED && status != Status.READ_ONLY) {
			throw new HibernateException("instance was not in a valid state");
		}
		return status == Status.READ_ONLY;
	}

	@Override
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
			loadedState = getPersister().getPropertyValues( entity );
			getPersistenceContext().getNaturalIdHelper().manageLocalNaturalIdCrossReference(
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
		return "EntityEntry" +
				MessageHelper.infoString( getPersister().getEntityName(), id ) +
				'(' + getStatus() + ')';
	}

	@Override
	public void serialize(ObjectOutputStream oos) throws IOException {
		final Status previousStatus = getPreviousStatus();
		oos.writeObject( getEntityName() );
		oos.writeObject( id );
		oos.writeObject( getStatus().name() );
		oos.writeObject( (previousStatus == null ? "" : previousStatus.name()) );
		// todo : potentially look at optimizing these two arrays
		oos.writeObject( loadedState );
		oos.writeObject( getDeletedState() );
		oos.writeObject( version );
		oos.writeObject( getLockMode().toString() );
		oos.writeBoolean( isExistsInDatabase() );
		oos.writeBoolean( isBeingReplicated() );
	}


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

	public PersistenceContext getPersistenceContext(){
		return persistenceContext;
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
	protected <E extends Enum<E>> void setCompressedValue(EnumState<E> state, E value) {
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
	protected <E extends Enum<E>> E getCompressedValue(EnumState<E> state) {
		// restore the numeric value from the bits at the right offset and return the corresponding enum constant
		final int index = ( ( compressedState & state.getMask() ) >> state.getOffset() ) - 1;
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
	protected void setCompressedValue(BooleanState state, boolean value) {
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
	protected boolean getCompressedValue(BooleanState state) {
		return ( ( compressedState & state.getMask() ) >> state.getOffset() ) == 1;
	}

	/**
	 * Represents an enum value stored within a number value, using four bits starting at a specified offset.
	 *
	 * @author Gunnar Morling
	 */
	protected static class EnumState<E extends Enum<E>> {

		protected static final EnumState<LockMode> LOCK_MODE = new EnumState<LockMode>( 0, LockMode.class );
		protected static final EnumState<Status> STATUS = new EnumState<Status>( 4, Status.class );
		protected static final EnumState<Status> PREVIOUS_STATUS = new EnumState<Status>( 8, Status.class );

		protected final int offset;
		protected final E[] enumConstants;
		protected final int mask;
		protected final int unsetMask;

		private EnumState(int offset, Class<E> enumType) {
			final E[] enumConstants = enumType.getEnumConstants();

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
	protected enum BooleanState {

		EXISTS_IN_DATABASE(13),
		IS_BEING_REPLICATED(14);

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
