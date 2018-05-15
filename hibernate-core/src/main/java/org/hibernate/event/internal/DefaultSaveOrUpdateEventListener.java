/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.PersistentObjectException;
import org.hibernate.TransientObjectException;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;

/**
 * Defines the default listener used by Hibernate for handling save-update
 * events.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class DefaultSaveOrUpdateEventListener extends AbstractSaveEventListener implements SaveOrUpdateEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultSaveOrUpdateEventListener.class );

	/**
	 * Handle the given update event.
	 *
	 * @param event The update event to be handled.
	 */
	public void onSaveOrUpdate(SaveOrUpdateEvent event) {
		final SessionImplementor source = event.getSession();
		final Object object = event.getObject();
		final Object requestedId = event.getRequestedId();

		if ( requestedId != null ) {
			//assign the requested id to the proxy, *before*
			//reassociating the proxy
			if ( object instanceof HibernateProxy ) {
				( (HibernateProxy) object ).getHibernateLazyInitializer().setIdentifier( requestedId );
			}
		}

		// For an uninitialized proxy, noop, don't even need to return an id, since it is never a save()
		if ( reassociateIfUninitializedProxy( object, source ) ) {
			LOG.trace( "Reassociated uninitialized proxy" );
		}
		else {
			//initialize properties of the event:
			final Object entity = source.getPersistenceContext().unproxyAndReassociate( object );
			event.setEntity( entity );
			event.setEntry( source.getPersistenceContext().getEntry( entity ) );
			//return the id in the event object
			event.setResultId( performSaveOrUpdate( event ) );
		}

	}

	protected boolean reassociateIfUninitializedProxy(Object object, SessionImplementor source) {
		return source.getPersistenceContext().reassociateIfUninitializedProxy( object );
	}

	protected Object performSaveOrUpdate(SaveOrUpdateEvent event) {
		EntityState entityState = getEntityState(
				event.getEntity(),
				event.getEntityName(),
				event.getEntry(),
				event.getSession()
		);

		switch ( entityState ) {
			case DETACHED:
				entityIsDetached( event );
				return null;
			case PERSISTENT:
				return entityIsPersistent( event );
			default: //TRANSIENT or DELETED
				return entityIsTransient( event );
		}
	}

	protected Object entityIsPersistent(SaveOrUpdateEvent event) throws HibernateException {
		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( traceEnabled ) {
			LOG.trace( "Ignoring persistent instance" );
		}
		EntityEntry entityEntry = event.getEntry();
		if ( entityEntry == null ) {
			throw new AssertionFailure( "entity was transient or detached" );
		}
		else {

			if ( entityEntry.getStatus() == Status.DELETED ) {
				throw new AssertionFailure( "entity was deleted" );
			}

			final SessionFactoryImplementor factory = event.getSession().getFactory();

			Object requestedId = event.getRequestedId();

			Object savedId;
			if ( requestedId == null ) {
				savedId = entityEntry.getId();
			}
			else {

				final boolean isEqual = !entityEntry.getDescriptor().getIdentifierType()
						.getJavaTypeDescriptor().areEqual( requestedId, entityEntry.getId() );

				if ( isEqual ) {
					throw new PersistentObjectException(
							"object passed to save() was already persistent: " +
									MessageHelper.infoString( entityEntry.getDescriptor(), requestedId, factory )
					);
				}

				savedId = requestedId;

			}

			if ( traceEnabled ) {
				LOG.tracev(
						"Object already associated with session: {0}",
						MessageHelper.infoString( entityEntry.getDescriptor(), savedId, factory )
				);
			}

			return savedId;

		}
	}

	/**
	 * The given save-update event named a transient entity.
	 * <p/>
	 * Here, we will perform the save processing.
	 *
	 * @param event The save event to be handled.
	 *
	 * @return The entity's identifier after saving.
	 */
	protected Object entityIsTransient(SaveOrUpdateEvent event) {

		LOG.trace( "Saving transient instance" );

		final EventSource source = event.getSession();

		EntityEntry entityEntry = event.getEntry();
		if ( entityEntry != null ) {
			if ( entityEntry.getStatus() == Status.DELETED ) {
				source.forceFlush( entityEntry );
			}
			else {
				throw new AssertionFailure( "entity was persistent" );
			}
		}

		Object id = saveWithGeneratedOrRequestedId( event );

		source.getPersistenceContext().reassociateProxy( event.getObject(), id );

		return id;
	}

	/**
	 * Save the transient instance, assigning the right identifier
	 *
	 * @param event The initiating event.
	 *
	 * @return The entity's identifier value after saving.
	 */
	protected Object saveWithGeneratedOrRequestedId(SaveOrUpdateEvent event) {
		return saveWithGeneratedId(
				event.getEntity(),
				event.getEntityName(),
				null,
				event.getSession(),
				true
		);
	}

	/**
	 * The given save-update event named a detached entity.
	 * <p/>
	 * Here, we will perform the update processing.
	 *
	 * @param event The update event to be handled.
	 */
	protected void entityIsDetached(SaveOrUpdateEvent event) {

		LOG.trace( "Updating detached instance" );

		if ( event.getSession().getPersistenceContext().isEntryFor( event.getEntity() ) ) {
			//TODO: assertion only, could be optimized away
			throw new AssertionFailure( "entity was persistent" );
		}

		Object entity = event.getEntity();

		EntityTypeDescriptor entityDescriptor = event.getSession().getEntityDescriptor( event.getEntityName(), entity );

		event.setRequestedId(
				getUpdateId( entity, entityDescriptor, event.getRequestedId(), event.getSession() )
		);

		performUpdate( event, entity, entityDescriptor );

	}

	/**
	 * Determine the id to use for updating.
	 *
	 * @param entity The entity.
	 * @param descriptor The entity descriptor
	 * @param requestedId The requested identifier
	 * @param session The session
	 *
	 * @return The id.
	 *
	 * @throws TransientObjectException If the entity is considered transient.
	 */
	protected Object getUpdateId(
			Object entity,
			EntityTypeDescriptor descriptor,
			Object requestedId,
			SessionImplementor session) {
		// use the id assigned to the instance
		Object id = descriptor.getIdentifier( entity, session );
		if ( id == null ) {
			// assume this is a newly instantiated transient object
			// which should be saved rather than updated
			throw new TransientObjectException(
					"The given object has a null identifier: " +
							descriptor.getEntityName()
			);
		}
		else {
			return id;
		}

	}

	protected void performUpdate(
			SaveOrUpdateEvent event,
			Object entity,
			EntityTypeDescriptor entityDescriptor) throws HibernateException {

		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( traceEnabled && !entityDescriptor.getJavaTypeDescriptor().getMutabilityPlan().isMutable() ) {
			LOG.trace( "Immutable instance passed to performUpdate()" );
		}

		if ( traceEnabled ) {
			LOG.tracev(
					"Updating {0}",
					MessageHelper.infoString( entityDescriptor, event.getRequestedId(), event.getSession().getFactory() )
			);
		}

		final EventSource source = event.getSession();
		final EntityKey key = source.generateEntityKey( event.getRequestedId(), entityDescriptor );

		source.getPersistenceContext().checkUniqueness( key, entity );

		if ( invokeUpdateLifecycle( entity, entityDescriptor, source ) ) {
			reassociate( event, event.getObject(), event.getRequestedId(), entityDescriptor );
			return;
		}

		// this is a transient object with existing persistent state not loaded by the session

		new OnUpdateVisitor( source, event.getRequestedId(), entity ).process( entity, entityDescriptor );

		// TODO: put this stuff back in to read snapshot from
		// the second-level cache (needs some extra work)
		/*Object[] cachedState = null;

        if ( entityDescriptor.hasCache() ) {
        	CacheEntry entry = (CacheEntry) entityDescriptor.getCache()
        			.get( event.getRequestedId(), source.getTimestamp() );
            cachedState = entry==null ?
            		null :
            		entry.getState(); //TODO: half-assemble this stuff
        }*/

		source.getPersistenceContext().addEntity(
				entity,
				( entityDescriptor.getJavaTypeDescriptor().getMutabilityPlan().isMutable() ? Status.MANAGED : Status.READ_ONLY ),
				null, // cachedState,
				key,
				entityDescriptor.getVersion( entity ),
				LockMode.NONE,
				true,
				entityDescriptor,
				false
		);

		entityDescriptor.afterReassociate( entity, source );

		if ( traceEnabled ) {
			LOG.tracev(
					"Updating {0}", MessageHelper.infoString(
					entityDescriptor,
					event.getRequestedId(),
					source.getFactory()
			)
			);
		}

		cascadeOnUpdate( event, entityDescriptor, entity );
	}

	protected boolean invokeUpdateLifecycle(Object entity, EntityTypeDescriptor entityDescriptor, EventSource source) {
		if ( entityDescriptor.implementsLifecycle() ) {
			LOG.debug( "Calling onUpdate()" );
			if ( ( (Lifecycle) entity ).onUpdate( source ) ) {
				LOG.debug( "Update vetoed by onUpdate()" );
				return true;
			}
		}
		return false;
	}

	/**
	 * Handles the calls needed to perform cascades as part of an update request
	 * for the given entity.
	 *
	 * @param event The event currently being processed.
	 * @param entityDescriptor The defined entityDescriptor for the entity being updated.
	 * @param entity The entity being updated.
	 */
	private void cascadeOnUpdate(SaveOrUpdateEvent event, EntityTypeDescriptor entityDescriptor, Object entity) {
		final EventSource source = event.getSession();
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			Cascade.cascade( CascadingActions.SAVE_UPDATE, CascadePoint.AFTER_UPDATE, source, entityDescriptor, entity );
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

	@Override
	protected CascadingAction getCascadeAction() {
		return CascadingActions.SAVE_UPDATE;
	}
}
