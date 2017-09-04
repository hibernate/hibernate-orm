/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.action.internal.DelayedPostInsertIdentifier;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

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
	public void checkId(Object object, EntityDescriptor persister, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( id != null && id instanceof DelayedPostInsertIdentifier ) {
			// this is a situation where the entity id is assigned by a post-insert generator
			// and was saved outside the transaction forcing it to be delayed
			return;
		}

		// todo (6.0) : iirc we removed the ability to define an entity whose class does not contain the id
		//		so `canExtractIdOutOfEntity` should always be true
		//if ( persister.canExtractIdOutOfEntity() ) {
			Serializable oid = persister.getIdentifier( object, session );
			if ( id == null ) {
				throw new AssertionFailure( "null id in " + persister.getEntityName() + " entry (don't flush the Session afterQuery an exception occurs)" );
			}
			if ( !persister.getIdentifierType().getJavaTypeDescriptor().areEqual( id, oid ) ) {
				throw new HibernateException(
						"identifier of an instance of " + persister.getEntityName() + " was altered from "
								+ id + " to " + oid
				);
			}
		//}
	}

	@SuppressWarnings("unchecked")
	private void checkNaturalId(
			EntityDescriptor entityDescriptor,
			EntityEntry entry,
			Object[] current,
			Object[] loaded,
			SessionImplementor session) {
		// mainly we are checking that the value of a natural-id defined as
		// immutable has not been changed
		final NaturalIdDescriptor naturalIdentifierDescriptor = entityDescriptor.getHierarchy().getNaturalIdDescriptor();

		if ( naturalIdentifierDescriptor == null || entry.getStatus() == Status.READ_ONLY ) {
			// no natural-id or the entity was loaded as read-only: nothing else to check
			return;
		}

		if ( naturalIdentifierDescriptor.isMutable() ) {
			// the natural id is mutable: nothing else to check
			return;
		}

		final Object[] snapshot = loaded == null
				? session.getPersistenceContext().getNaturalIdSnapshot( entry.getId(), entityDescriptor )
				: session.getPersistenceContext().getNaturalIdHelper().extractNaturalIdValues( loaded, entityDescriptor );

		// todo (6.0) : as-is this block assumes that the orders are the same - which is not necessarily accurate
		int i = -1;

		for ( PersistentAttribute persistentAttribute : naturalIdentifierDescriptor.getPersistentAttributes() ) {
			i++;

			// todo (6.0) : see the 6.0-todo doc "open question" regarding `@NaturalId#mutable`
			//		here we assume that naturalIdentifierDescriptor.isMutable() check above
			//		already verifies that (since the natural-id is considered immutable if we
			//		get here) the attributes are also immutable
			//if ( persistentAttribute.isMutable() ) {
			//	continue;
			//}

			final Object previousAttributeValue = snapshot[i];
			final Object currentAttributeValue = current[i];

			final boolean changed = !persistentAttribute.getJavaTypeDescriptor().areEqual(
					previousAttributeValue,
					currentAttributeValue
			);
			if ( changed ) {
				throw new HibernateException(
						String.format(
								"An immutable natural identifier of entity %s was altered from %s to %s",
								entityDescriptor.getEntityName(),
								persistentAttribute.getJavaTypeDescriptor().extractLoggableRepresentation( previousAttributeValue ),
								persistentAttribute.getJavaTypeDescriptor().extractLoggableRepresentation( currentAttributeValue )
						)
				);
			}
		}
	}

	/**
	 * Flushes a single entity's state to the database, by scheduling
	 * an update action, if necessary
	 */
	public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
		final Object entity = event.getEntity();
		final EntityEntry entry = event.getEntityEntry();
		final EventSource session = event.getSession();
		final EntityDescriptor entityDescriptor = entry.getPersister();
		final Status status = entry.getStatus();
		final List persistentAttributes = entityDescriptor.getPersistentAttributes();

		final boolean mightBeDirty = entry.requiresDirtyCheck( entity );

		final Object[] values = getValues( entity, entry, mightBeDirty, session );

		event.setPropertyValues( values );

		//TODO: avoid this for non-new instances where mightBeDirty==false
		boolean substitute = wrapCollections( session, entityDescriptor, persistentAttributes, values );

		if ( isUpdateNecessary( event, mightBeDirty ) ) {
			substitute = scheduleUpdate( event ) || substitute;
		}

		if ( status != Status.DELETED ) {
			// now update the object .. has to be outside the main if block above (because of collections)
			if ( substitute ) {
				entityDescriptor.setPropertyValues( entity, values );
			}

			// Search for collections by reachability, updating their role.
			// We don't want to touch collections reachable from a deleted object
			if ( entityDescriptor.hasCollections() ) {
				new FlushVisitor( session, entity ).processEntityPropertyValues( values, persistentAttributes );
			}
		}

	}

	private Object[] getValues(Object entity, EntityEntry entry, boolean mightBeDirty, SessionImplementor session) {
		final Object[] loadedState = entry.getLoadedState();
		final Status status = entry.getStatus();
		final EntityDescriptor persister = entry.getPersister();

		final Object[] values;
		if ( status == Status.DELETED ) {
			//grab its state saved at deletion
			values = entry.getDeletedState();
		}
		else if ( !mightBeDirty && loadedState != null ) {
			values = loadedState;
		}
		else {
			checkId( entity, persister, entry.getId(), session );

			// grab its current state
			values = persister.getPropertyValues( entity );

			checkNaturalId( persister, entry, values, loadedState, session );
		}
		return values;
	}

	private boolean wrapCollections(
			EventSource session,
			EntityDescriptor persister,
			List<PersistentAttribute> persistentAttributes,
			Object[] values) {
		if ( !persister.hasCollections() ) {
			return false;
		}

		// wrap up any new collections directly referenced by the object
		// or its components

		// NOTE: we need to do the wrap here even if its not "dirty",
		// because collections need wrapping but changes to _them_
		// don't dirty the container. Also, for versioned data, we
		// need to wrap beforeQuery calling searchForDirtyCollections

		WrapVisitor visitor = new WrapVisitor( session );
		// substitutes into values by side-effect
		visitor.processEntityPropertyValues( values, persistentAttributes );
		return visitor.isSubstitutionRequired();
	}

	private boolean isUpdateNecessary(final FlushEntityEvent event, final boolean mightBeDirty) {
		final Status status = event.getEntityEntry().getStatus();
		if ( mightBeDirty || status == Status.DELETED ) {
			// compare to cached state (ignoring collections unless versioned)
			dirtyCheck( event );
			if ( isUpdateNecessary( event ) ) {
				return true;
			}
			else {
				if ( SelfDirtinessTracker.class.isInstance( event.getEntity() ) ) {
					( (SelfDirtinessTracker) event.getEntity() ).$$_hibernate_clearDirtyAttributes();
				}
				event.getSession()
						.getFactory()
						.getCustomEntityDirtinessStrategy()
						.resetDirty( event.getEntity(), event.getEntityEntry().getPersister(), event.getSession() );
				return false;
			}
		}
		else {
			return hasDirtyCollections( event, event.getEntityEntry().getPersister(), status );
		}
	}

	private boolean scheduleUpdate(final FlushEntityEvent event) {
		final EntityEntry entry = event.getEntityEntry();
		final EventSource session = event.getSession();
		final Object entity = event.getEntity();
		final Status status = entry.getStatus();
		final EntityDescriptor entityDescriptor = entry.getPersister();
		final Object[] values = event.getPropertyValues();

		if ( LOG.isTraceEnabled() ) {
			if ( status == Status.DELETED ) {
				if ( !entityDescriptor.getJavaTypeDescriptor().getMutabilityPlan().isMutable() ) {
					LOG.tracev(
							"Updating immutable, deleted entity: {0}",
							MessageHelper.infoString( entityDescriptor, entry.getId(), session.getFactory() )
					);
				}
				else if ( !entry.isModifiableEntity() ) {
					LOG.tracev(
							"Updating non-modifiable, deleted entity: {0}",
							MessageHelper.infoString( entityDescriptor, entry.getId(), session.getFactory() )
					);
				}
				else {
					LOG.tracev(
							"Updating deleted entity: ",
							MessageHelper.infoString( entityDescriptor, entry.getId(), session.getFactory() )
					);
				}
			}
			else {
				LOG.tracev(
						"Updating entity: {0}",
						MessageHelper.infoString( entityDescriptor, entry.getId(), session.getFactory() )
				);
			}
		}

		final boolean intercepted = !entry.isBeingReplicated() && handleInterception( event );

		// increment the version number (if necessary)
		final Object nextVersion = getNextVersion( event );

		// if it was dirtied by a collection only
		int[] dirtyProperties = event.getDirtyProperties();
		if ( event.isDirtyCheckPossible() && dirtyProperties == null ) {
			if ( !intercepted && !event.hasDirtyCollection() ) {
				throw new AssertionFailure( "dirty, but no dirty properties" );
			}
			dirtyProperties = ArrayHelper.EMPTY_INT_ARRAY;
		}

		// check nullability but do not doAfterTransactionCompletion command execute
		// we'll use scheduled updates for that.
		new Nullability( session ).checkNullability( values, entityDescriptor, true );

		// schedule the update
		// note that we intentionally do _not_ pass in currentPersistentState!
		session.getActionQueue().addAction(
				new EntityUpdateAction(
						entry.getId(),
						values,
						dirtyProperties,
						event.hasDirtyCollection(),
						( status == Status.DELETED && !entry.isModifiableEntity() ?
								entityDescriptor.getPropertyValues( entity ) :
								entry.getLoadedState() ),
						entry.getVersion(),
						nextVersion,
						entity,
						entry.getRowId(),
						entityDescriptor,
						session
				)
		);

		return intercepted;
	}

	protected boolean handleInterception(FlushEntityEvent event) {
		SessionImplementor session = event.getSession();
		EntityEntry entry = event.getEntityEntry();
		EntityDescriptor entityDescriptor = entry.getDescriptor();
		Object entity = event.getEntity();

		//give the Interceptor a chance to modify property values
		final Object[] values = event.getPropertyValues();
		final boolean intercepted = invokeInterceptor( session, entity, entry, values, entityDescriptor );

		//now we might need to recalculate the dirtyProperties array
		if ( intercepted && event.isDirtyCheckPossible() && !event.isDirtyCheckHandledByInterceptor() ) {
			int[] dirtyProperties;
			if ( event.hasDatabaseSnapshot() ) {
				dirtyProperties = entityDescriptor.findModified( event.getDatabaseSnapshot(), values, entity, session );
			}
			else {
				dirtyProperties = entityDescriptor.findDirty( values, entry.getLoadedState(), entity, session );
			}
			event.setDirtyProperties( dirtyProperties );
		}

		return intercepted;
	}

	protected boolean invokeInterceptor(
			SessionImplementor session,
			Object entity,
			EntityEntry entry,
			final Object[] values,
			EntityDescriptor persister) {
		boolean isDirty = false;
		if ( entry.getStatus() != Status.DELETED ) {
			if ( callbackRegistry.preUpdate( entity ) ) {
				isDirty = copyState( entity, persister.getPropertyJavaTypeDescriptors(), values, session.getFactory() );
			}
		}

		final boolean answerFromInterceptor = session.getInterceptor().onFlushDirty(
				entity,
				entry.getId(),
				values,
				entry.getLoadedState(),
				persister.getPropertyNames(),
				persister.getPropertyJavaTypeDescriptors()
		);

		return answerFromInterceptor || isDirty;
	}

	private boolean copyState(Object entity, JavaTypeDescriptor[] javaTypeDescriptors, Object[] state, SessionFactory sf) {
		// copy the entity state into the state array and return true if the state has changed
		ClassMetadata metadata = sf.getClassMetadata( entity.getClass() );
		Object[] newState = metadata.getPropertyValues( entity );
		int size = newState.length;
		boolean isDirty = false;
		for ( int index = 0; index < size ; index++ ) {
			if ( ( state[index] == LazyPropertyInitializer.UNFETCHED_PROPERTY &&
					newState[index] != LazyPropertyInitializer.UNFETCHED_PROPERTY ) ||
					( state[index] != newState[index] && !javaTypeDescriptors[index].areEqual( state[index], newState[index] ) ) ) {
				isDirty = true;
				state[index] = newState[index];
			}
		}
		return isDirty;
	}

	/**
	 * Convenience method to retrieve an entities next version value
	 */
	private Object getNextVersion(FlushEntityEvent event) throws HibernateException {

		final EntityEntry entry = event.getEntityEntry();
		final EntityDescriptor persister = entry.getPersister();
		final VersionDescriptor versionDescriptor = persister.getHierarchy().getVersionDescriptor();

		if ( versionDescriptor != null ) {
			final VersionSupport versionSupport = persister.getHierarchy().getVersionDescriptor().getVersionSupport();

			Object[] values = event.getPropertyValues();

			if ( entry.isBeingReplicated() ) {
				return Versioning.getVersion( values, persister );
			}
			else {
				int[] dirtyProperties = event.getDirtyProperties();

				final boolean isVersionIncrementRequired = isVersionIncrementRequired(
						event,
						entry,
						persister,
						dirtyProperties
				);

				final Object nextVersion = isVersionIncrementRequired ?
						Versioning.increment(
								entry.getVersion(),
								versionSupport,
								event.getSession()
						) :
						entry.getVersion(); //use the current version

				Versioning.setVersion( values, nextVersion, persister );

				return nextVersion;
			}
		}
		else {
			return null;
		}

	}

	private boolean isVersionIncrementRequired(
			FlushEntityEvent event,
			EntityEntry entry,
			EntityDescriptor persister,
			int[] dirtyProperties
	) {
		final boolean isVersionIncrementRequired = entry.getStatus() != Status.DELETED && (
				dirtyProperties == null ||
						Versioning.isVersionIncrementRequired(
								dirtyProperties,
								event.hasDirtyCollection(),
								persister.getPropertyVersionability()
						)
		);
		return isVersionIncrementRequired;
	}

	/**
	 * Performs all necessary checking to determine if an entity needs an SQL update
	 * to synchronize its state to the database. Modifies the event by side-effect!
	 * Note: this method is quite slow, avoid calling if possible!
	 */
	protected final boolean isUpdateNecessary(FlushEntityEvent event) throws HibernateException {

		EntityDescriptor persister = event.getEntityEntry().getPersister();
		Status status = event.getEntityEntry().getStatus();

		if ( !event.isDirtyCheckPossible() ) {
			return true;
		}
		else {

			int[] dirtyProperties = event.getDirtyProperties();
			if ( dirtyProperties != null && dirtyProperties.length != 0 ) {
				return true; //TODO: suck into event class
			}
			else {
				return hasDirtyCollections( event, persister, status );
			}

		}
	}

	private boolean hasDirtyCollections(FlushEntityEvent event, EntityDescriptor persister, Status status) {
		if ( isCollectionDirtyCheckNecessary( persister, status ) ) {
			DirtyCollectionSearchVisitor visitor = new DirtyCollectionSearchVisitor(
					event.getSession(),
					persister.getPropertyVersionability()
			);
			visitor.processEntityPropertyValues( event.getPropertyValues(), persister.getPersistentAttributes() );
			boolean hasDirtyCollections = visitor.wasDirtyCollectionFound();
			event.setHasDirtyCollection( hasDirtyCollections );
			return hasDirtyCollections;
		}
		else {
			return false;
		}
	}

	@SuppressWarnings("RedundantIfStatement")
	private boolean isCollectionDirtyCheckNecessary(EntityDescriptor persister, Status status) {
		if ( status != Status.MANAGED && status != Status.READ_ONLY ) {
			return false;
		}

		if ( persister.getHierarchy().getVersionDescriptor() == null ) {
			return false;
		}

		if ( !persister.hasCollections() ) {
			return false;
		}

		return true;
	}

	/**
	 * Perform a dirty check, and attach the results to the event
	 */
	protected void dirtyCheck(final FlushEntityEvent event) throws HibernateException {
		final Object entity = event.getEntity();
		final Object[] values = event.getPropertyValues();
		final SessionImplementor session = event.getSession();
		final EntityEntry entry = event.getEntityEntry();
		final EntityDescriptor persister = entry.getPersister();
		final Serializable id = entry.getId();
		final Object[] loadedState = entry.getLoadedState();

		int[] dirtyProperties = session.getInterceptor().findDirty(
				entity,
				id,
				values,
				loadedState,
				persister.getPropertyNames(),
				persister.getPropertyJavaTypeDescriptors()
		);

		if ( dirtyProperties == null ) {
			if ( entity instanceof SelfDirtinessTracker ) {
				if ( ( (SelfDirtinessTracker) entity ).$$_hibernate_hasDirtyAttributes() ) {
					dirtyProperties = persister.resolveAttributeIndexes( ( (SelfDirtinessTracker) entity ).$$_hibernate_getDirtyAttributes() );
				}
				else {
					dirtyProperties = new int[0];
				}
			}
			else {
				// see if the custom dirtiness strategy can tell us...
				class DirtyCheckContextImpl implements CustomEntityDirtinessStrategy.DirtyCheckContext {
					int[] found;

					@Override
					public void doDirtyChecking(CustomEntityDirtinessStrategy.AttributeChecker attributeChecker) {
						found = new DirtyCheckAttributeInfoImpl( event ).visitAttributes( attributeChecker );
						if ( found != null && found.length == 0 ) {
							found = null;
						}
					}
				}
				DirtyCheckContextImpl context = new DirtyCheckContextImpl();
				session.getFactory().getCustomEntityDirtinessStrategy().findDirty(
						entity,
						persister,
						session,
						context
				);
				dirtyProperties = context.found;
			}
		}

		event.setDatabaseSnapshot( null );

		final boolean interceptorHandledDirtyCheck;
		boolean cannotDirtyCheck;

		if ( dirtyProperties == null ) {
			// Interceptor returned null, so do the dirtycheck ourself, if possible
			try {
				session.getEventListenerManager().dirtyCalculationStart();

				interceptorHandledDirtyCheck = false;
				// object loaded by update()
				cannotDirtyCheck = loadedState == null;
				if ( !cannotDirtyCheck ) {
					// dirty check against the usual snapshot of the entity
					dirtyProperties = persister.findDirty( values, loadedState, entity, session );
				}
				else if ( entry.getStatus() == Status.DELETED && !event.getEntityEntry().isModifiableEntity() ) {
					// A non-modifiable (e.g., read-only or immutable) entity needs to be have
					// references to transient entities set to null beforeQuery being deleted. No other
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
					final Object[] currentState = persister.getPropertyValues( event.getEntity() );
					dirtyProperties = persister.findDirty( entry.getDeletedState(), currentState, entity, session );
					cannotDirtyCheck = false;
				}
				else {
					// dirty check against the database snapshot, if possible/necessary
					final Object[] databaseSnapshot = getDatabaseSnapshot( session, persister, id );
					if ( databaseSnapshot != null ) {
						dirtyProperties = persister.findModified( databaseSnapshot, values, entity, session );
						cannotDirtyCheck = false;
						event.setDatabaseSnapshot( databaseSnapshot );
					}
				}
			}
			finally {
				session.getEventListenerManager().dirtyCalculationEnd( dirtyProperties != null );
			}
		}
		else {
			// the Interceptor handled the dirty checking
			cannotDirtyCheck = false;
			interceptorHandledDirtyCheck = true;
		}

		logDirtyProperties( id, dirtyProperties, persister );

		event.setDirtyProperties( dirtyProperties );
		event.setDirtyCheckHandledByInterceptor( interceptorHandledDirtyCheck );
		event.setDirtyCheckPossible( !cannotDirtyCheck );

	}

	private class DirtyCheckAttributeInfoImpl implements CustomEntityDirtinessStrategy.AttributeInformation {
		private final FlushEntityEvent event;
		private final EntityDescriptor persister;
		private final int numberOfAttributes;
		private int index;

		private DirtyCheckAttributeInfoImpl(FlushEntityEvent event) {
			this.event = event;
			this.persister = event.getEntityEntry().getPersister();
			this.numberOfAttributes = persister.getPropertyNames().length;
		}

		@Override
		public EntityDescriptor getContainingPersister() {
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
				databaseSnapshot = getDatabaseSnapshot( event.getSession(), persister, event.getEntityEntry().getId() );
			}
			return databaseSnapshot[index];
		}

		public int[] visitAttributes(CustomEntityDirtinessStrategy.AttributeChecker attributeChecker) {
			databaseSnapshot = null;
			index = 0;

			final int[] indexes = new int[numberOfAttributes];
			int count = 0;
			for (; index < numberOfAttributes; index++ ) {
				if ( attributeChecker.isDirty( this ) ) {
					indexes[count++] = index;
				}
			}
			return Arrays.copyOf( indexes, count );
		}
	}

	private void logDirtyProperties(Serializable id, int[] dirtyProperties, EntityDescriptor persister) {
		if ( dirtyProperties != null && dirtyProperties.length > 0 && LOG.isTraceEnabled() ) {
			final String[] allPropertyNames = persister.getPropertyNames();
			final String[] dirtyPropertyNames = new String[dirtyProperties.length];
			for ( int i = 0; i < dirtyProperties.length; i++ ) {
				dirtyPropertyNames[i] = allPropertyNames[dirtyProperties[i]];
			}
			LOG.tracev(
					"Found dirty properties [{0}] : {1}",
					MessageHelper.infoString( persister.getEntityName(), id ),
					dirtyPropertyNames
			);
		}
	}

	private Object[] getDatabaseSnapshot(SessionImplementor session, EntityDescriptor persister, Serializable id) {
		if ( persister.isSelectBeforeUpdateRequired() ) {
			Object[] snapshot = session.getPersistenceContext()
					.getDatabaseSnapshot( id, persister );
			if ( snapshot == null ) {
				//do we even really need this? the update will fail anyway....
				if ( session.getFactory().getStatistics().isStatisticsEnabled() ) {
					session.getFactory().getStatisticsImplementor()
							.optimisticFailure( persister.getEntityName() );
				}
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
			return snapshot;
		}
		// TODO: optimize away this lookup for entities w/o unsaved-value="undefined"
		final EntityKey entityKey = session.generateEntityKey( id, persister );
		return session.getPersistenceContext().getCachedDatabaseSnapshot( entityKey );
	}
}
