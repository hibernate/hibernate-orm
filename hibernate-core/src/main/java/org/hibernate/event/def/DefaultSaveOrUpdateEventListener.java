/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.event.def;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.PersistentObjectException;
import org.hibernate.TransientObjectException;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.Cascade;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Status;
import org.hibernate.event.EventSource;
import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.event.SaveOrUpdateEventListener;
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

	private static final Logger log = LoggerFactory.getLogger( DefaultSaveOrUpdateEventListener.class );

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
				( ( HibernateProxy ) object ).getHibernateLazyInitializer().setIdentifier( requestedId );
			}
		}

		if ( reassociateIfUninitializedProxy( object, source ) ) {
			log.trace( "reassociated uninitialized proxy" );
			// an uninitialized proxy, noop, don't even need to
			// return an id, since it is never a save()
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

	protected Serializable performSaveOrUpdate(SaveOrUpdateEvent event) {
		int entityState = getEntityState(
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

	protected Serializable entityIsPersistent(SaveOrUpdateEvent event) throws HibernateException {
		log.trace( "ignoring persistent instance" );

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
						.isEqual( requestedId, entityEntry.getId(), event.getSession().getEntityMode(), factory );

				if ( isEqual ) {
					throw new PersistentObjectException(
							"object passed to save() was already persistent: " +
									MessageHelper.infoString( entityEntry.getPersister(), requestedId, factory )
					);
				}

				savedId = requestedId;

			}

			if ( log.isTraceEnabled() ) {
				log.trace(
						"object already associated with session: " +
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

		log.trace( "saving transient instance" );

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

		log.trace( "updating detached instance" );


		if ( event.getSession().getPersistenceContext().isEntryFor( event.getEntity() ) ) {
			//TODO: assertion only, could be optimized away
			throw new AssertionFailure( "entity was persistent" );
		}

		Object entity = event.getEntity();

		EntityPersister persister = event.getSession().getEntityPersister( event.getEntityName(), entity );

		event.setRequestedId(
				getUpdateId(
						entity, persister, event.getRequestedId(), event.getSession()
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

			if ( !persister.isMutable() ) {
				log.trace( "immutable instance passed to performUpdate()" );
			}

			if ( log.isTraceEnabled() ) {
				log.trace(
						"updating " +
								MessageHelper.infoString(
										persister, event.getRequestedId(), event.getSession().getFactory()
								)
				);
			}

			final EventSource source = event.getSession();

			EntityKey key = new EntityKey( event.getRequestedId(), persister, source.getEntityMode() );

			source.getPersistenceContext().checkUniqueness( key, entity );

			if ( invokeUpdateLifecycle( entity, persister, source ) ) {
				reassociate( event, event.getObject(), event.getRequestedId(), persister );
				return;
			}

			// this is a transient object with existing persistent state not loaded by the session

			new OnUpdateVisitor( source, event.getRequestedId(), entity ).process( entity, persister );

			//TODO: put this stuff back in to read snapshot from
			//      the second-level cache (needs some extra work)
			/*Object[] cachedState = null;

			if ( persister.hasCache() ) {
				CacheEntry entry = (CacheEntry) persister.getCache()
						.get( event.getRequestedId(), source.getTimestamp() );
			    cachedState = entry==null ?
			    		null :
			    		entry.getState(); //TODO: half-assemble this stuff
			}*/

			source.getPersistenceContext().addEntity(
					entity,
					( persister.isMutable() ? Status.MANAGED : Status.READ_ONLY ),
					null, //cachedState,
					key,
					persister.getVersion( entity, source.getEntityMode() ),
					LockMode.NONE,
					true,
					persister,
					false,
					true //assume true, since we don't really know, and it doesn't matter
			);

			persister.afterReassociate( entity, source );

			if ( log.isTraceEnabled() ) {
				log.trace(
						"updating " +
								MessageHelper.infoString( persister, event.getRequestedId(), source.getFactory() )
				);
			}

			cascadeOnUpdate( event, persister, entity );
	}

	protected boolean invokeUpdateLifecycle(Object entity, EntityPersister persister, EventSource source) {
		if ( persister.implementsLifecycle( source.getEntityMode() ) ) {
			log.debug( "calling onUpdate()" );
			if ( ( ( Lifecycle ) entity ).onUpdate( source ) ) {
				log.debug( "update vetoed by onUpdate()" );
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
		EventSource source = event.getSession();
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( CascadingAction.SAVE_UPDATE, Cascade.AFTER_UPDATE, source )
					.cascade( persister, entity );
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

	protected CascadingAction getCascadeAction() {
		return CascadingAction.SAVE_UPDATE;
	}
}
