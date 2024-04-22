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

import org.hibernate.AssertionFailure;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.TypeHelper;

import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;
import static org.hibernate.engine.internal.AbstractEntityEntry.BooleanState.EXISTS_IN_DATABASE;
import static org.hibernate.engine.internal.AbstractEntityEntry.BooleanState.IS_BEING_REPLICATED;
import static org.hibernate.engine.internal.AbstractEntityEntry.EnumState.LOCK_MODE;
import static org.hibernate.engine.internal.AbstractEntityEntry.EnumState.PREVIOUS_STATUS;
import static org.hibernate.engine.internal.AbstractEntityEntry.EnumState.STATUS;
import static org.hibernate.engine.internal.ManagedTypeHelper.asManagedEntity;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.asSelfDirtinessTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.isHibernateProxy;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isSelfDirtinessTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfManagedEntity;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfSelfDirtinessTracker;
import static org.hibernate.engine.spi.CachedNaturalIdValueSource.LOAD;
import static org.hibernate.engine.spi.Status.DELETED;
import static org.hibernate.engine.spi.Status.GONE;
import static org.hibernate.engine.spi.Status.MANAGED;
import static org.hibernate.engine.spi.Status.READ_ONLY;
import static org.hibernate.engine.spi.Status.SAVING;
import static org.hibernate.pretty.MessageHelper.infoString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * A base implementation of {@link EntityEntry}.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 * @author Sanne Grinovero
 */
public abstract class AbstractEntityEntry implements Serializable, EntityEntry {

	protected final Object id;
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
	 * <p>
	 * Use {@link #setCompressedValue(EnumState, Enum)},
	 * {@link #getCompressedValue(EnumState)} etc
	 * to access the enums and booleans stored in this value.
	 * <p>
	 * Representing enum values by their ordinal value is acceptable for our case as this value itself is never
	 * serialized or deserialized and thus is not affected should ordinal values change.
	 */
	private transient int compressedState;

	public AbstractEntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Object id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement,
			final PersistenceContext persistenceContext) {
		setCompressedValue( STATUS, status );
		// not useful strictly speaking but more explicit
		setCompressedValue( PREVIOUS_STATUS, null );
		// only retain loaded state if the status is not Status.READ_ONLY
		if ( status != READ_ONLY ) {
			this.loadedState = loadedState;
		}
		this.id = id;
		this.rowId = rowId;
		setCompressedValue( EXISTS_IN_DATABASE, existsInDatabase );
		this.version = version;
		setCompressedValue( LOCK_MODE, lockMode );
		setCompressedValue( IS_BEING_REPLICATED, disableVersionIncrement );
		this.persister = persister;
		this.persistenceContext = persistenceContext;
	}

	/**
	 * This for is used during custom deserialization handling
	 */
	protected AbstractEntityEntry(
			final SessionFactoryImplementor factory,
			final String entityName,
			final Object id,
			final Status status,
			final Status previousStatus,
			final Object[] loadedState,
			final Object[] deletedState,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final boolean isBeingReplicated,
			final PersistenceContext persistenceContext) {
		this.persister = factory == null
				? null
				: factory.getRuntimeMetamodels().getMappingMetamodel()
						.getEntityDescriptor( entityName );
		this.id = id;
		setCompressedValue( STATUS, status );
		setCompressedValue( PREVIOUS_STATUS, previousStatus );
		this.loadedState = loadedState;
		setDeletedState( deletedState );
		this.version = version;
		setCompressedValue( LOCK_MODE, lockMode );
		setCompressedValue( EXISTS_IN_DATABASE, existsInDatabase );
		setCompressedValue( IS_BEING_REPLICATED, isBeingReplicated );
		this.rowId = null; // this is equivalent to the old behavior...
		this.persistenceContext = persistenceContext;
	}

	@Override
	public LockMode getLockMode() {
		return getCompressedValue( LOCK_MODE );
	}

	@Override
	public void setLockMode(LockMode lockMode) {
		setCompressedValue( LOCK_MODE, lockMode );
	}


	@Override
	public Status getStatus() {
		return getCompressedValue( STATUS );
	}

	private Status getPreviousStatus() {
		return getCompressedValue( PREVIOUS_STATUS );
	}

	@Override
	public void setStatus(Status status) {
		if ( status == READ_ONLY ) {
			//memory optimization
			loadedState = null;
		}

		final Status currentStatus = this.getStatus();
		if ( currentStatus != status ) {
			setCompressedValue( PREVIOUS_STATUS, currentStatus );
			setCompressedValue( STATUS, status );
		}
	}

	@Override
	public final Object getId() {
		return id;
	}

	@Override
	public final Object[] getLoadedState() {
		return loadedState;
	}

	@Override
	public Object[] getDeletedState() {
		final EntityEntryExtraStateHolder extra = getExtraState( EntityEntryExtraStateHolder.class );
		return extra == null ? null : extra.getDeletedState();
	}

	@Override
	public void setDeletedState(Object[] deletedState) {
		final EntityEntryExtraStateHolder existingExtra = getExtraState( EntityEntryExtraStateHolder.class );
		if ( existingExtra != null ) {
			existingExtra.setDeletedState( deletedState );
		}
		else if ( deletedState != null ) {
			final EntityEntryExtraStateHolder newExtra = new EntityEntryExtraStateHolder();
			newExtra.setDeletedState( deletedState );
			addExtraState( newExtra );
		}
		//else this is the default value, we do not store the extra state
	}

	@Override
	public boolean isExistsInDatabase() {
		return getCompressedValue( EXISTS_IN_DATABASE );
	}

	@Override
	public final Object getVersion() {
		return version;
	}

	@Override
	public void postInsert(Object version) {
		this.version = version;
	}

	@Override
	public final EntityPersister getPersister() {
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
		return getCompressedValue( IS_BEING_REPLICATED );
	}

	@Override
	public Object getRowId() {
		return rowId;
	}

	@Override
	public void postUpdate(Object entity, Object[] updatedState, Object nextVersion) {
		loadedState = updatedState;
		setLockMode( LockMode.WRITE );

		if ( persister.isVersioned() ) {
			version = nextVersion;
			persister.setValue( entity, persister.getVersionProperty(), nextVersion );
		}

		processIfSelfDirtinessTracker( entity, AbstractEntityEntry::clearDirtyAttributes );
		processIfManagedEntity( entity, AbstractEntityEntry::useTracker );

		final SharedSessionContractImplementor session = getPersistenceContext().getSession();
		session.getFactory().getCustomEntityDirtinessStrategy()
				.resetDirty( entity, persister, session.asSessionImplementor() );
	}

	private static void clearDirtyAttributes(final SelfDirtinessTracker entity) {
		entity.$$_hibernate_clearDirtyAttributes();
	}

	private static void useTracker(final ManagedEntity entity) {
		entity.$$_hibernate_setUseTracker( true );
	}

	@Override
	public void postDelete() {
		setCompressedValue( PREVIOUS_STATUS, getStatus() );
		setCompressedValue( STATUS, GONE );
		setCompressedValue( EXISTS_IN_DATABASE, false );
	}

	@Override
	public void postInsert(Object[] insertedState) {
		setCompressedValue( EXISTS_IN_DATABASE, true );
	}

	@Override
	public boolean isNullifiable(boolean earlyInsert, SharedSessionContractImplementor session) {
		if ( getStatus() == SAVING ) {
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
		final int index = propertyIndex( propertyName );
		return index < 0 ? null : loadedState[index];
	}

	private int propertyIndex(String propertyName) {
		final AttributeMapping attributeMapping = persister.findAttributeMapping( propertyName );
		return attributeMapping != null ? attributeMapping.getStateArrayPosition() : -1;
	}

	@Override
	public void overwriteLoadedStateCollectionValue(String propertyName, PersistentCollection<?> collection) {
		// nothing to do if status is READ_ONLY
		if ( getStatus() != READ_ONLY ) {
			assert propertyName != null;
			assert loadedState != null;

			loadedState[ propertyIndex( propertyName ) ] = collection;
		}
	}

	@Override
	public boolean requiresDirtyCheck(Object entity) {
		return isModifiableEntity()
			&& !isUnequivocallyNonDirty( entity );
	}

	private boolean isUnequivocallyNonDirty(Object entity) {
		if ( isSelfDirtinessTracker( entity ) ) {
			final boolean uninitializedProxy;
			if ( isPersistentAttributeInterceptable( entity ) ) {
				final PersistentAttributeInterceptor interceptor =
						asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
				if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
					EnhancementAsProxyLazinessInterceptor enhancementAsProxyLazinessInterceptor =
							(EnhancementAsProxyLazinessInterceptor) interceptor;
					return !enhancementAsProxyLazinessInterceptor.hasWrittenFieldNames(); //EARLY EXIT!
				}
				else {
					uninitializedProxy = false;
				}
			}
			else if ( isHibernateProxy( entity ) ) {
				uninitializedProxy = extractLazyInitializer( entity ).isUninitialized();
			}
			else {
				uninitializedProxy = false;
			}
			// we never have to check an uninitialized proxy
			return uninitializedProxy
				|| !persister.hasCollections()
					&& !persister.hasMutableProperties()
					&& !asSelfDirtinessTracker( entity ).$$_hibernate_hasDirtyAttributes()
					&& asManagedEntity( entity ).$$_hibernate_useTracker();
		}
		else {
			if ( isPersistentAttributeInterceptable( entity ) ) {
				final PersistentAttributeInterceptor interceptor =
						asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
				if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
					// we never have to check an uninitialized proxy
					return true; //EARLY EXIT!
				}
			}

			final SessionImplementor session = getPersistenceContext().getSession().asSessionImplementor();
			final CustomEntityDirtinessStrategy customEntityDirtinessStrategy =
					session.getFactory().getCustomEntityDirtinessStrategy();
			return customEntityDirtinessStrategy.canDirtyCheck( entity, getPersister(), session  )
				&& !customEntityDirtinessStrategy.isDirty( entity, getPersister(), session );
		}
	}

	@Override
	public boolean isModifiableEntity() {
		final Status status = getStatus();
		final Status previousStatus = getPreviousStatus();
		return persister.isMutable()
			&& status != READ_ONLY
			&& ! ( status == DELETED && previousStatus == READ_ONLY );
	}

	@Override
	public void forceLocked(Object entity, Object nextVersion) {
		version = nextVersion;
		loadedState[ persister.getVersionProperty() ] = version;
		setLockMode( PESSIMISTIC_FORCE_INCREMENT );
		persister.setValue( entity, getPersister().getVersionProperty(), nextVersion );
	}

	@Override
	public boolean isReadOnly() {
		final Status status = getStatus();
		if ( status != MANAGED && status != READ_ONLY ) {
			throw new HibernateException("instance was not in a valid state");
		}
		return status == READ_ONLY;
	}

	@Override
	public void setReadOnly(boolean readOnly, Object entity) {
		if ( readOnly != isReadOnly() ) {
			if ( readOnly ) {
				setStatus( READ_ONLY );
				loadedState = null;
			}
			else {
				if ( ! persister.isMutable() ) {
					throw new IllegalStateException( "Cannot make an entity of immutable type '"
							+ persister.getEntityName() + "' modifiable" );
				}
				setStatus( MANAGED );
				loadedState = persister.getValues( entity );
				TypeHelper.deepCopy(
						loadedState,
						persister.getPropertyTypes(),
						persister.getPropertyCheckability(),
						loadedState,
						getPersistenceContext().getSession()
				);
				if ( persister.hasNaturalIdentifier() ) {
					getPersistenceContext().getNaturalIdResolutions().manageLocalResolution(
							id,
							persister.getNaturalIdMapping().extractNaturalIdFromEntityState( loadedState ),
							persister,
							LOAD
					);
				}
			}
		}
	}

	@Override
	public String toString() {
		return "EntityEntry"
			+ infoString( getPersister().getEntityName(), id )
			+ '(' + getStatus() + ')';
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

	@Override @SuppressWarnings("unchecked")
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
	 *			identifies the value to store
	 * @param value
	 *			the value to store; The caller must make sure that it matches
	 *			the given identifier
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
	 *			identifies the value to store
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
	 *			identifies the value to store
	 * @param value
	 *			the value to store
	 */
	protected void setCompressedValue(BooleanState state, boolean value) {
		compressedState &= state.getUnsetMask();
		compressedState |= ( state.getValue( value ) << state.getOffset() );
	}

	/**
	 * Gets the current value of the given boolean flag.
	 *
	 * @param state
	 *			identifies the value to store
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

		protected static final EnumState<LockMode> LOCK_MODE = new EnumState<>( 0, LockMode.class );
		protected static final EnumState<Status> STATUS = new EnumState<>( 4, Status.class );
		protected static final EnumState<Status> PREVIOUS_STATUS = new EnumState<>( 8, Status.class );

		protected final int offset;
		protected final E[] enumConstants;
		protected final int mask;
		protected final int unsetMask;

		private EnumState(int offset, Class<E> enumType) {
			final E[] enumConstants = enumType.getEnumConstants();

			// In case any of the enums cannot be stored in 4 bits anymore,
			// we'd have to re-structure the compressed state int
			if ( enumConstants.length > 15 ) {
				throw new AssertionFailure( "Cannot store enum type " + enumType.getName()
						+ " in compressed state as it has too many values." );
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

		BooleanState(int offset) {
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
