/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Arrays;

import org.hibernate.AssertionFailure;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.action.internal.DelayedPostInsertIdentifier;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.asSelfDirtinessTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isSelfDirtinessTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfSelfDirtinessTracker;
import static org.hibernate.engine.internal.Versioning.getVersion;
import static org.hibernate.engine.internal.Versioning.incrementVersion;
import static org.hibernate.engine.internal.Versioning.setVersion;

/**
 * An event that occurs for each entity instance at flush time
 *
 * @author Gavin King
 */
public class DefaultFlushEntityEventListener implements FlushEntityEventListener, CallbackRegistryConsumer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultFlushEntityEventListener.class );

	private CallbackRegistry callbackRegistry;

	@Override
	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	/**
	 * make sure user didn't mangle the id
	 */
	public void checkId(Object object, EntityPersister persister, Object id, SessionImplementor session)
			throws HibernateException {

		if ( id instanceof DelayedPostInsertIdentifier ) {
			// this is a situation where the entity id is assigned by a post-insert generator
			// and was saved outside the transaction forcing it to be delayed
			return;
		}

		final Object oid = persister.getIdentifier( object, session );

		if ( id == null ) {
			throw new AssertionFailure( "null id in " + persister.getEntityName()
					+ " entry (don't flush the Session after an exception occurs)" );
		}

		//Small optimisation: always try to avoid getIdentifierType().isEqual(..) when possible.
		//(However it's not safe to invoke the equals() method as it might trigger side-effects)
		if ( id == oid ) {
			//No further checks necessary:
			return;
		}

		if ( !persister.getIdentifierType().isEqual( id, oid, session.getFactory() ) ) {
			throw new HibernateException( "identifier of an instance of " + persister.getEntityName()
					+ " was altered from " + oid + " to " + id );
		}
	}

	private void checkNaturalId(
			EntityPersister persister,
			Object entity,
			EntityEntry entry,
			Object[] current,
			Object[] loaded,
			SessionImplementor session) {
		if ( !isUninitializedEnhanced( entity ) ) {
			final NaturalIdMapping naturalIdMapping = persister.getNaturalIdMapping();
			if ( naturalIdMapping != null && entry.getStatus() != Status.READ_ONLY ) {
				naturalIdMapping.verifyFlushState( entry.getId(), current, loaded, session );
			}
		}
	}

	private static boolean isUninitializedEnhanced(Object entity) {
		if ( isPersistentAttributeInterceptable( entity ) ) {
			final PersistentAttributeInterceptor interceptor = asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
			// the entity is an un-initialized enhancement-as-proxy reference
			return interceptor instanceof EnhancementAsProxyLazinessInterceptor;
		}
		else {
			return false;
		}
	}

	/**
	 * Flushes a single entity's state to the database, by scheduling
	 * an update action, if necessary
	 */
	@Override
	public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
		final Object entity = event.getEntity();
		final EntityEntry entry = event.getEntityEntry();
		final EventSource session = event.getSession();

		final boolean mightBeDirty = entry.requiresDirtyCheck( entity );

		final Object[] values = getValues( entity, entry, mightBeDirty, session );

		event.setPropertyValues( values );

		//TODO: avoid this for non-new instances where mightBeDirty==false

		boolean substitute = wrapCollections( event, values );

		if ( isUpdateNecessary( event, mightBeDirty ) ) {
			substitute = scheduleUpdate( event ) || substitute;
		}

		if ( entry.getStatus() != Status.DELETED ) {
			final EntityPersister persister = entry.getPersister();
			// now update the object
			// has to be outside the main if block above (because of collections)
			if ( substitute ) {
				persister.setPropertyValues( entity, values );
			}
			// Search for collections by reachability, updating their role.
			// We don't want to touch collections reachable from a deleted object
			if ( persister.hasCollections() ) {
				new FlushVisitor( session, entity )
						.processEntityPropertyValues( values, persister.getPropertyTypes() );
			}
		}

	}

	private Object[] getValues(Object entity, EntityEntry entry, boolean mightBeDirty, SessionImplementor session) {
		final Object[] loadedState = entry.getLoadedState();

		if ( entry.getStatus() == Status.DELETED ) {
			//grab its state saved at deletion
			return entry.getDeletedState();
		}
		else if ( !mightBeDirty && loadedState != null ) {
			return loadedState;
		}
		else {
			final EntityPersister persister = entry.getPersister();
			checkId( entity, persister, entry.getId(), session );
			// grab its current state
			Object[] values = persister.getValues( entity );
			checkNaturalId( persister, entity, entry, values, loadedState, session );
			return values;
		}
	}

	/**
	 * Wrap up any new collections directly referenced by the object
	 * or its components.
	 */
	private boolean wrapCollections(
			FlushEntityEvent event,
			Object[] values) {
		final EntityEntry entry = event.getEntityEntry();
		final EntityPersister persister = entry.getPersister();
		if ( persister.hasCollections() ) {
			// NOTE: we need to do the wrap here even if it's not "dirty",
			// because collections need wrapping but changes to _them_
			// don't dirty the container. Also, for versioned data, we
			// need to wrap before calling searchForDirtyCollections
			final WrapVisitor visitor = new WrapVisitor( event.getEntity(), entry.getId(), event.getSession() );
			// substitutes into values by side effect
			visitor.processEntityPropertyValues( values, persister.getPropertyTypes() );
			return visitor.isSubstitutionRequired();
		}
		else {
			return false;
		}
	}

	private boolean isUpdateNecessary(final FlushEntityEvent event, final boolean mightBeDirty) {
		final EntityEntry entry = event.getEntityEntry();
		if ( mightBeDirty || entry.getStatus() == Status.DELETED ) {
			// compare to cached state (ignoring collections unless versioned)
			dirtyCheck( event );
			if ( isUpdateNecessary( event ) ) {
				return true;
			}
			else {
				final Object entity = event.getEntity();
				processIfSelfDirtinessTracker( entity, SelfDirtinessTracker::$$_hibernate_clearDirtyAttributes );
				final EventSource source = event.getSession();
				source.getFactory()
						.getCustomEntityDirtinessStrategy()
						.resetDirty( entity, entry.getPersister(), source );
				return false;
			}
		}
		else {
			return hasDirtyCollections( event );
		}
	}

	private boolean scheduleUpdate(final FlushEntityEvent event) {
		final EntityEntry entry = event.getEntityEntry();
		final EventSource session = event.getSession();
		final Object entity = event.getEntity();
		final Status status = entry.getStatus();
		final EntityPersister persister = entry.getPersister();
		final Object[] values = event.getPropertyValues();

		logScheduleUpdate( entry, session, status, persister );

		final boolean intercepted = !entry.isBeingReplicated() && handleInterception( event );

		// increment the version number (if necessary)
		final Object nextVersion = getNextVersion( event );

		int[] dirtyProperties = getDirtyProperties( event, intercepted );

		// check nullability but do not doAfterTransactionCompletion command execute
		// we'll use scheduled updates for that.
		new Nullability( session ).checkNullability( values, persister, true );

		// schedule the update
		// note that we intentionally do _not_ pass in currentPersistentState!
		session.getActionQueue().addAction(
				new EntityUpdateAction(
						entry.getId(),
						values,
						dirtyProperties,
						event.hasDirtyCollection(),
						status == Status.DELETED && !entry.isModifiableEntity()
								? persister.getValues( entity )
								: entry.getLoadedState(),
						entry.getVersion(),
						nextVersion,
						entity,
						entry.getRowId(),
						persister,
						session
				)
		);

		return intercepted;
	}

	private static int[] getDirtyProperties(FlushEntityEvent event, boolean intercepted) {
		int[] dirtyProperties = event.getDirtyProperties();
		if ( event.isDirtyCheckPossible() && dirtyProperties == null ) {
			if ( !intercepted && !event.hasDirtyCollection() ) {
				throw new AssertionFailure( "dirty, but no dirty properties" );
			}
			else {
				// it was dirtied by a collection only
				return ArrayHelper.EMPTY_INT_ARRAY;
			}
		}
		else {
			return dirtyProperties;
		}
	}

	private static void logScheduleUpdate(EntityEntry entry, EventSource session, Status status, EntityPersister persister) {
		if ( LOG.isTraceEnabled() ) {
			if ( status == Status.DELETED ) {
				if ( !persister.isMutable() ) {
					LOG.tracev(
							"Updating immutable, deleted entity: {0}",
							MessageHelper.infoString(persister, entry.getId(), session.getFactory() )
					);
				}
				else if ( !entry.isModifiableEntity() ) {
					LOG.tracev(
							"Updating non-modifiable, deleted entity: {0}",
							MessageHelper.infoString(persister, entry.getId(), session.getFactory() )
					);
				}
				else {
					LOG.tracev(
							"Updating deleted entity: {0}",
							MessageHelper.infoString(persister, entry.getId(), session.getFactory() )
					);
				}
			}
			else {
				LOG.tracev(
						"Updating entity: {0}",
						MessageHelper.infoString(persister, entry.getId(), session.getFactory() )
				);
			}
		}
	}

	protected boolean handleInterception(FlushEntityEvent event) {
		//give the Interceptor a chance to modify property values
		final boolean intercepted = invokeInterceptor( event );
		//now we might need to recalculate the dirtyProperties array
		if ( intercepted && event.isDirtyCheckPossible() ) {
			dirtyCheck( event );
		}
		return intercepted;
	}

	protected boolean invokeInterceptor(FlushEntityEvent event) {
		final EntityEntry entry = event.getEntityEntry();
		final Object entity = event.getEntity();
		final Object id = entry.getId();
		final Object[] values = event.getPropertyValues();
		final EntityPersister persister = entry.getPersister();
		final EventSource session = event.getSession();

		boolean isDirty = false;

		if ( entry.getStatus() != Status.DELETED ) {
			if ( callbackRegistry.preUpdate( entity ) ) {
				isDirty = copyState( entity, persister.getPropertyTypes(), values, session.getFactory() );
			}
		}

		final boolean stateModified = session.getInterceptor().onFlushDirty(
				entity,
				id,
				values,
				entry.getLoadedState(),
				persister.getPropertyNames(),
				persister.getPropertyTypes()
		);

		return stateModified || isDirty;
	}

	private boolean copyState(Object entity, Type[] types, Object[] state, SessionFactoryImplementor factory) {
		// copy the entity state into the state array and return true if the state has changed
		final Object[] newState = currentState( entity, factory );
		boolean isDirty = false;
		for ( int index = 0, size = newState.length; index < size; index++ ) {
			if ( isDirty( types[index], state[index], newState[index] ) ) {
				isDirty = true;
				state[index] = newState[index];
			}
		}
		return isDirty;
	}

	private static Object[] currentState(Object entity, SessionFactoryImplementor factory) {
		return factory.getRuntimeMetamodels()
				.getEntityMappingType( entity.getClass() )
				.getEntityPersister()
				.getValues( entity );
	}

	private static boolean isDirty(Type types, Object state, Object newState) {
		return state == UNFETCHED_PROPERTY && newState != UNFETCHED_PROPERTY
			|| state != newState && !types.isEqual( state, newState );
	}

	/**
	 * Convenience method to retrieve an entities next version value
	 */
	private Object getNextVersion(FlushEntityEvent event) throws HibernateException {
		final EntityEntry entry = event.getEntityEntry();
		final EntityPersister persister = entry.getPersister();
		if ( persister.isVersioned() ) {
			final Object[] values = event.getPropertyValues();
			if ( entry.isBeingReplicated() ) {
				return getVersion( values, persister );
			}
			else {
				final Object nextVersion = isVersionIncrementRequired( event, entry )
						? incrementVersion( event.getEntity(), entry.getVersion(), persister, event.getSession() )
						: entry.getVersion(); //use the current version
				setVersion( values, nextVersion, persister );
				return nextVersion;
			}
		}
		else {
			return null;
		}
	}

	private static boolean isVersionIncrementRequired(FlushEntityEvent event, EntityEntry entry) {
		if ( entry.getStatus() == Status.DELETED ) {
			return false;
		}
		else {
			int[] dirtyProperties = event.getDirtyProperties();
			return dirtyProperties == null
				|| Versioning.isVersionIncrementRequired(
					dirtyProperties,
					event.hasDirtyCollection(),
					event.getEntityEntry().getPersister().getPropertyVersionability()
				);
		}
	}

	/**
	 * Performs all necessary checking to determine if an entity needs an SQL update
	 * to synchronize its state to the database. Modifies the event by side effect!
	 * Note: this method is quite slow, avoid calling if possible!
	 */
	protected final boolean isUpdateNecessary(FlushEntityEvent event) throws HibernateException {
		return !event.isDirtyCheckPossible()
			|| event.hasDirtyProperties()
			|| hasDirtyCollections(event);
	}

	private boolean hasDirtyCollections(FlushEntityEvent event) {
		final EntityEntry entityEntry = event.getEntityEntry();
		final EntityPersister persister = entityEntry.getPersister();
		if ( isCollectionDirtyCheckNecessary( persister, entityEntry.getStatus() ) ) {
			final DirtyCollectionSearchVisitor visitor = new DirtyCollectionSearchVisitor(
					event.getEntity(),
					event.getSession(),
					persister.getPropertyVersionability()
			);
			visitor.processEntityPropertyValues( event.getPropertyValues(), persister.getPropertyTypes() );
			boolean hasDirtyCollections = visitor.wasDirtyCollectionFound();
			event.setHasDirtyCollection( hasDirtyCollections );
			return hasDirtyCollections;
		}
		else {
			return false;
		}
	}

	private boolean isCollectionDirtyCheckNecessary(EntityPersister persister, Status status) {
		return ( status == Status.MANAGED || status == Status.READ_ONLY )
			&& persister.isVersioned()
			&& persister.hasCollections();
	}

	/**
	 * Perform a dirty check, and attach the results to the event
	 */
	protected void dirtyCheck(final FlushEntityEvent event) throws HibernateException {
		int[] dirtyProperties = getDirtyProperties( event );
		event.setDatabaseSnapshot( null );
		if ( dirtyProperties == null ) {
			// do the dirty check the hard way
			dirtyProperties = performDirtyCheck( event );
		}
		else {
			// the Interceptor, SelfDirtinessTracker, or CustomEntityDirtinessStrategy
			// already handled the dirty check for us
			event.setDirtyProperties( dirtyProperties );
			event.setDirtyCheckHandledByInterceptor( true );
			event.setDirtyCheckPossible( true );
		}
		logDirtyProperties( event.getEntityEntry(), dirtyProperties );
	}

	private static int[] performDirtyCheck(FlushEntityEvent event) {
		final SessionImplementor session = event.getSession();
		boolean dirtyCheckPossible;
		int[] dirtyProperties = null;
		final EventManager eventManager = session.getEventManager();
		final HibernateEvent dirtyCalculationEvent = eventManager.beginDirtyCalculationEvent();
		final EntityEntry entry = event.getEntityEntry();
		final EntityPersister persister = entry.getPersister();
		try {
			session.getEventListenerManager().dirtyCalculationStart();
			// object loaded by update()
			final Object[] values = event.getPropertyValues();
			final Object[] loadedState = entry.getLoadedState();
			final Object entity = event.getEntity();
			if ( loadedState != null ) {
				// dirty check against the usual snapshot of the entity
				dirtyProperties = persister.findDirty( values, loadedState, entity, session );
				dirtyCheckPossible = true;
			}
			else if ( entry.getStatus() == Status.DELETED && !entry.isModifiableEntity() ) {
				// A non-modifiable (e.g., read-only or immutable) entity needs to be have
				// references to transient entities set to null before being deleted. No other
				// fields should be updated.
				if ( values != entry.getDeletedState() ) {
					throw new IllegalStateException(
							"Entity has status Status.DELETED but values != entry.getDeletedState"
					);
				}
				// Even if loadedState == null, we can dirty-check by comparing currentState and
				// entry.getDeletedState() because the only fields to be updated are those that
				// refer to transient entities that are being set to null.
				// - currentState contains the entity's current property values.
				// - entry.getDeletedState() contains the entity's current property values with
				//   references to transient entities set to null.
				// - dirtyProperties will only contain properties that refer to transient entities
				final Object[] currentState = persister.getValues( event.getEntity() );
				dirtyProperties = persister.findDirty( entry.getDeletedState(), currentState, entity, session );
				dirtyCheckPossible = true;
			}
			else {
				// dirty check against the database snapshot, if possible/necessary
				final Object[] databaseSnapshot = getDatabaseSnapshot( persister, entry.getId(), session );
				if ( databaseSnapshot != null ) {
					dirtyProperties = persister.findModified( databaseSnapshot, values, entity, session );
					dirtyCheckPossible = true;
					event.setDatabaseSnapshot( databaseSnapshot );
				}
				else {
					dirtyCheckPossible = false;
				}
			}
			event.setDirtyProperties( dirtyProperties );
			event.setDirtyCheckHandledByInterceptor( false );
			event.setDirtyCheckPossible( dirtyCheckPossible );
		}
		finally {
			eventManager.completeDirtyCalculationEvent( dirtyCalculationEvent, session, persister, entry, dirtyProperties );
			session.getEventListenerManager().dirtyCalculationEnd( dirtyProperties != null );
		}
		return dirtyProperties;
	}

	/**
	 * Attempt to get the dirty properties from either the Interceptor,
	 * the bytecode enhancement, or a custom dirtiness strategy.
	 */
	private static int[] getDirtyProperties(FlushEntityEvent event) {
		int[] dirtyProperties = getDirtyPropertiesFromInterceptor( event );
		if ( dirtyProperties != null ) {
			return dirtyProperties;
		}
		else {
			final Object entity = event.getEntity();
			return isSelfDirtinessTracker( entity )
					? getDirtyPropertiesFromSelfDirtinessTracker( asSelfDirtinessTracker( entity ), event )
					: getDirtyPropertiesFromCustomEntityDirtinessStrategy( event );
		}
	}

	private static int[] getDirtyPropertiesFromInterceptor(FlushEntityEvent event) {
		final EntityEntry entry = event.getEntityEntry();
		final EntityPersister persister = entry.getPersister();
		return event.getSession().getInterceptor().findDirty(
				event.getEntity(),
				entry.getId(),
				event.getPropertyValues(),
				entry.getLoadedState(),
				persister.getPropertyNames(),
				persister.getPropertyTypes()
		);
	}

	private static int[] getDirtyPropertiesFromCustomEntityDirtinessStrategy(FlushEntityEvent event) {
		// see if the custom dirtiness strategy can tell us...
		class DirtyCheckContextImpl implements CustomEntityDirtinessStrategy.DirtyCheckContext {
			private int[] found;
			@Override
			public void doDirtyChecking(CustomEntityDirtinessStrategy.AttributeChecker attributeChecker) {
				found = new DirtyCheckAttributeInfoImpl( event ).visitAttributes( attributeChecker );
				if ( found.length == 0 ) {
					found = null;
				}
			}
		}
		final EventSource session = event.getSession();
		final DirtyCheckContextImpl context = new DirtyCheckContextImpl();
		session.getFactory().getCustomEntityDirtinessStrategy()
				.findDirty( event.getEntity(), event.getEntityEntry().getPersister(), session, context );
		return context.found;
	}

	private static int[] getDirtyPropertiesFromSelfDirtinessTracker(SelfDirtinessTracker tracker, FlushEntityEvent event) {
		final EntityEntry entry = event.getEntityEntry();
		final EntityPersister persister = entry.getPersister();
		if ( tracker.$$_hibernate_hasDirtyAttributes() || persister.hasMutableProperties() ) {
			return persister.resolveDirtyAttributeIndexes(
					event.getPropertyValues(),
					entry.getLoadedState(),
					tracker.$$_hibernate_getDirtyAttributes(),
					event.getSession()
			);
		}
		else {
			return ArrayHelper.EMPTY_INT_ARRAY;
		}
	}

	private static class DirtyCheckAttributeInfoImpl implements CustomEntityDirtinessStrategy.AttributeInformation {
		private final FlushEntityEvent event;
		private final EntityPersister persister;
		private final int numberOfAttributes;
		private int index;

		private DirtyCheckAttributeInfoImpl(FlushEntityEvent event) {
			this.event = event;
			this.persister = event.getEntityEntry().getPersister();
			this.numberOfAttributes = persister.getPropertyNames().length;
		}

		@Override
		public EntityPersister getContainingPersister() {
			return persister;
		}

		@Override
		public int getAttributeIndex() {
			return index;
		}

		@Override
		public String getName() {
			return persister.getPropertyNames()[index];
		}

		@Override
		public Type getType() {
			return persister.getPropertyTypes()[index];
		}

		@Override
		public Object getCurrentValue() {
			return event.getPropertyValues()[index];
		}

		Object[] databaseSnapshot;

		@Override
		public Object getLoadedValue() {
			if ( databaseSnapshot == null ) {
				databaseSnapshot = getDatabaseSnapshot( persister, event.getEntityEntry().getId(), event.getSession() );
			}
			return databaseSnapshot[index];
		}

		public int[] visitAttributes(CustomEntityDirtinessStrategy.AttributeChecker attributeChecker) {
			databaseSnapshot = null;
			final int[] indexes = new int[numberOfAttributes];
			int count = 0;
			for ( index = 0; index < numberOfAttributes; index++ ) {
				if ( attributeChecker.isDirty( this ) ) {
					indexes[count++] = index;
				}
			}
			return Arrays.copyOf( indexes, count );
		}
	}

	private void logDirtyProperties(EntityEntry entry, int[] dirtyProperties) {
		if ( dirtyProperties != null && dirtyProperties.length > 0 && LOG.isTraceEnabled() ) {
			final EntityPersister persister = entry.getPersister();
			final String[] allPropertyNames = persister.getPropertyNames();
			final String[] dirtyPropertyNames = new String[dirtyProperties.length];
			for ( int i = 0; i < dirtyProperties.length; i++ ) {
				dirtyPropertyNames[i] = allPropertyNames[dirtyProperties[i]];
			}
			LOG.tracev(
					"Found dirty properties [{0}] : {1}",
					MessageHelper.infoString( persister.getEntityName(), entry.getId() ),
					Arrays.toString( dirtyPropertyNames )
			);
		}
	}

	private static Object[] getDatabaseSnapshot(EntityPersister persister, Object id, SessionImplementor session) {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		if ( persister.isSelectBeforeUpdateRequired() ) {
			final Object[] snapshot = persistenceContext.getDatabaseSnapshot( id, persister );
			if ( snapshot == null ) {
				//do we even really need this? the update will fail anyway....
				final StatisticsImplementor statistics = session.getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( persister.getEntityName() );
				}
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
			return snapshot;
		}
		// TODO: optimize away this lookup for entities w/o unsaved-value="undefined"
		return persistenceContext.getCachedDatabaseSnapshot( session.generateEntityKey( id, persister ) );
	}
}
