/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

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
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
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
		final Serializable requestedId = event.getRequestedId();

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
			final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
			final Object entity = persistenceContext.unproxyAndReassociate( object );
			event.setEntity( entity );
			event.setEntry( persistenceContext.getEntry( entity ) );
			//return the id in the event object
			event.setResultId( performSaveOrUpdate( event ) );
		}

	}

	protected boolean reassociateIfUninitializedProxy(Object object, SessionImplementor source) {
		return source.getPersistenceContextInternal().reassociateIfUninitializedProxy( object );
	}

	protected Serializable performSaveOrUpdate(SaveOrUpdateEvent event) {
		EntityState entityState = EntityState.getEntityState(
				event.getEntity(),
				event.getEntityName(),
				event.getEntry(),
				event.getSession(),
				null
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

	protected Serializable entityIsPersistent(SaveOrUpdateEvent event) throws HibernateException {
		if ( LOG.isTraceEnabled() ) {
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

			Serializable requestedId = event.getRequestedId();

			Serializable savedId;
			if ( requestedId == null ) {
				savedId = entityEntry.getId();
			}
			else {

				final boolean isEqual = !entityEntry.getPersister().getIdentifierType()
						.isEqual( requestedId, entityEntry.getId(), factory );

				if ( isEqual ) {
					throw new PersistentObjectException(
							"object passed to save() was already persistent: " +
									MessageHelper.infoString( entityEntry.getPersister(), requestedId, factory )
					);
				}

				savedId = requestedId;

			}

			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Object already associated with session: {0}",
						MessageHelper.infoString( entityEntry.getPersister(), savedId, factory )
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
	protected Serializable entityIsTransient(SaveOrUpdateEvent event) {

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

		Serializable id = saveWithGeneratedOrRequestedId( event );

		source.getPersistenceContextInternal().reassociateProxy( event.getObject(), id );

		return id;
	}

	/**
	 * Save the transient instance, assigning the right identifier
	 *
	 * @param event The initiating event.
	 *
	 * @return The entity's identifier value after saving.
	 */
	protected Serializable saveWithGeneratedOrRequestedId(SaveOrUpdateEvent event) {
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

		final EventSource session = event.getSession();
		if ( session.getPersistenceContextInternal().isEntryFor( event.getEntity() ) ) {
			//TODO: assertion only, could be optimized away
			throw new AssertionFailure( "entity was persistent" );
		}

		Object entity = event.getEntity();

		EntityPersister persister = session.getEntityPersister( event.getEntityName(), entity );

		event.setRequestedId(
				getUpdateId(
						entity, persister, event.getRequestedId(), session
				)
		);

		performUpdate( event, entity, persister );

	}

	/**
	 * Determine the id to use for updating.
	 *
	 * @param entity The entity.
	 * @param persister The entity persister
	 * @param requestedId The requested identifier
	 * @param session The session
	 *
	 * @return The id.
	 *
	 * @throws TransientObjectException If the entity is considered transient.
	 */
	protected Serializable getUpdateId(
			Object entity,
			EntityPersister persister,
			Serializable requestedId,
			SessionImplementor session) {
		// use the id assigned to the instance
		Serializable id = persister.getIdentifier( entity, session );
		if ( id == null ) {
			// assume this is a newly instantiated transient object
			// which should be saved rather than updated
			throw new TransientObjectException(
					"The given object has a null identifier: " +
							persister.getEntityName()
			);
		}
		else {
			return id;
		}

	}

	protected void performUpdate(
			SaveOrUpdateEvent event,
			Object entity,
			EntityPersister persister) throws HibernateException {

		if ( LOG.isTraceEnabled() && !persister.isMutable() ) {
			LOG.trace( "Immutable instance passed to performUpdate()" );
		}

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Updating {0}",
					MessageHelper.infoString( persister, event.getRequestedId(), event.getSession().getFactory() )
			);
		}

		final EventSource source = event.getSession();
		final EntityKey key = source.generateEntityKey( event.getRequestedId(), persister );

		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		persistenceContext.checkUniqueness( key, entity );

		if ( invokeUpdateLifecycle( entity, persister, source ) ) {
			reassociate( event, event.getObject(), event.getRequestedId(), persister );
			return;
		}

		// this is a transient object with existing persistent state not loaded by the session

		new OnUpdateVisitor( source, event.getRequestedId(), entity ).process( entity, persister );

		// TODO: put this stuff back in to read snapshot from
		// the second-level cache (needs some extra work)
		/*Object[] cachedState = null;

        if ( persister.hasCache() ) {
        	CacheEntry entry = (CacheEntry) persister.getCache()
        			.get( event.getRequestedId(), source.getTimestamp() );
            cachedState = entry==null ?
            		null :
            		entry.getState(); //TODO: half-assemble this stuff
        }*/

		persistenceContext.addEntity(
				entity,
				( persister.isMutable() ? Status.MANAGED : Status.READ_ONLY ),
				null, // cachedState,
				key,
				persister.getVersion( entity ),
				LockMode.NONE,
				true,
				persister,
				false
		);

		persister.afterReassociate( entity, source );

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Updating {0}", MessageHelper.infoString(
					persister,
					event.getRequestedId(),
					source.getFactory()
			)
			);
		}

		cascadeOnUpdate( event, persister, entity );
	}

	protected boolean invokeUpdateLifecycle(Object entity, EntityPersister persister, EventSource source) {
		if ( persister.implementsLifecycle() ) {
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
	 * @param persister The defined persister for the entity being updated.
	 * @param entity The entity being updated.
	 */
	private void cascadeOnUpdate(SaveOrUpdateEvent event, EntityPersister persister, Object entity) {
		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		persistenceContext.incrementCascadeLevel();
		try {
			Cascade.cascade( CascadingActions.SAVE_UPDATE, CascadePoint.AFTER_UPDATE, source, persister, entity );
		}
		finally {
			persistenceContext.decrementCascadeLevel();
		}
	}

	@Override
	protected CascadingAction getCascadeAction() {
		return CascadingActions.SAVE_UPDATE;
	}
}
