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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.TransientObjectException;
import org.hibernate.util.IdentitySet;
import org.hibernate.action.EntityDeleteAction;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.Cascade;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.Nullability;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.Status;
import org.hibernate.event.DeleteEvent;
import org.hibernate.event.DeleteEventListener;
import org.hibernate.event.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * Defines the default delete event listener used by hibernate for deleting entities
 * from the datastore in response to generated delete events.
 *
 * @author Steve Ebersole
 */
public class DefaultDeleteEventListener implements DeleteEventListener {

	private static final Logger log = LoggerFactory.getLogger( DefaultDeleteEventListener.class );

	/**
	 * Handle the given delete event.
	 *
	 * @param event The delete event to be handled.
	 *
	 * @throws HibernateException
	 */
	public void onDelete(DeleteEvent event) throws HibernateException {
		onDelete( event, new IdentitySet() );
	}

	/**
	 * Handle the given delete event.  This is the cascaded form.
	 *
	 * @param event The delete event.
	 * @param transientEntities The cache of entities already deleted
	 *
	 * @throws HibernateException
	 */
	public void onDelete(DeleteEvent event, Set transientEntities) throws HibernateException {

		final EventSource source = event.getSession();

		final PersistenceContext persistenceContext = source.getPersistenceContext();
		Object entity = persistenceContext.unproxyAndReassociate( event.getObject() );

		EntityEntry entityEntry = persistenceContext.getEntry( entity );
		final EntityPersister persister;
		final Serializable id;
		final Object version;

		if ( entityEntry == null ) {
			log.trace( "entity was not persistent in delete processing" );

			persister = source.getEntityPersister( event.getEntityName(), entity );

			if ( ForeignKeys.isTransient( persister.getEntityName(), entity, null, source ) ) {
				deleteTransientEntity( source, entity, event.isCascadeDeleteEnabled(), persister, transientEntities );
				// EARLY EXIT!!!
				return;
			}
			else {
				performDetachedEntityDeletionCheck( event );
			}

			id = persister.getIdentifier( entity, source );

			if ( id == null ) {
				throw new TransientObjectException(
						"the detached instance passed to delete() had a null identifier"
				);
			}

			EntityKey key = new EntityKey( id, persister, source.getEntityMode() );

			persistenceContext.checkUniqueness( key, entity );

			new OnUpdateVisitor( source, id, entity ).process( entity, persister );

			version = persister.getVersion( entity, source.getEntityMode() );

			entityEntry = persistenceContext.addEntity(
					entity,
					( persister.isMutable() ? Status.MANAGED : Status.READ_ONLY ),
					persister.getPropertyValues( entity, source.getEntityMode() ),
					key,
					version,
					LockMode.NONE,
					true,
					persister,
					false,
					false
			);
		}
		else {
			log.trace( "deleting a persistent instance" );

			if ( entityEntry.getStatus() == Status.DELETED || entityEntry.getStatus() == Status.GONE ) {
				log.trace( "object was already deleted" );
				return;
			}
			persister = entityEntry.getPersister();
			id = entityEntry.getId();
			version = entityEntry.getVersion();
		}

		/*if ( !persister.isMutable() ) {
			throw new HibernateException(
					"attempted to delete an object of immutable class: " +
					MessageHelper.infoString(persister)
				);
		}*/

		if ( invokeDeleteLifecycle( source, entity, persister ) ) {
			return;
		}

		deleteEntity( source, entity, entityEntry, event.isCascadeDeleteEnabled(), persister, transientEntities );

		if ( source.getFactory().getSettings().isIdentifierRollbackEnabled() ) {
			persister.resetIdentifier( entity, id, version, source );
		}
	}

	/**
	 * Called when we have recognized an attempt to delete a detached entity.
	 * <p/>
	 * This is perfectly valid in Hibernate usage; JPA, however, forbids this.
	 * Thus, this is a hook for HEM to affect this behavior.
	 *
	 * @param event The event.
	 */
	protected void performDetachedEntityDeletionCheck(DeleteEvent event) {
		// ok in normal Hibernate usage to delete a detached entity; JPA however
		// forbids it, thus this is a hook for HEM to affect this behavior
	}

	/**
	 * We encountered a delete request on a transient instance.
	 * <p/>
	 * This is a deviation from historical Hibernate (pre-3.2) behavior to
	 * align with the JPA spec, which states that transient entities can be
	 * passed to remove operation in which case cascades still need to be
	 * performed.
	 *
	 * @param session The session which is the source of the event
	 * @param entity The entity being delete processed
	 * @param cascadeDeleteEnabled Is cascading of deletes enabled
	 * @param persister The entity persister
	 * @param transientEntities A cache of already visited transient entities
	 * (to avoid infinite recursion).
	 */
	protected void deleteTransientEntity(
			EventSource session,
			Object entity,
			boolean cascadeDeleteEnabled,
			EntityPersister persister,
			Set transientEntities) {
		log.info( "handling transient entity in delete processing" );
		if ( transientEntities.contains( entity ) ) {
			log.trace( "already handled transient entity; skipping" );
			return;
		}
		transientEntities.add( entity );
		cascadeBeforeDelete( session, persister, entity, null, transientEntities );
		cascadeAfterDelete( session, persister, entity, transientEntities );
	}

	/**
	 * Perform the entity deletion.  Well, as with most operations, does not
	 * really perform it; just schedules an action/execution with the
	 * {@link org.hibernate.engine.ActionQueue} for execution during flush.
	 *
	 * @param session The originating session
	 * @param entity The entity to delete
	 * @param entityEntry The entity's entry in the {@link PersistenceContext}
	 * @param isCascadeDeleteEnabled Is delete cascading enabled?
	 * @param persister The entity persister.
	 * @param transientEntities A cache of already deleted entities.
	 */
	protected final void deleteEntity(
			final EventSource session,
			final Object entity,
			final EntityEntry entityEntry,
			final boolean isCascadeDeleteEnabled,
			final EntityPersister persister,
			final Set transientEntities) {

		if ( log.isTraceEnabled() ) {
			log.trace(
					"deleting " +
							MessageHelper.infoString( persister, entityEntry.getId(), session.getFactory() )
			);
		}

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final Type[] propTypes = persister.getPropertyTypes();
		final Object version = entityEntry.getVersion();

		final Object[] currentState;
		if ( entityEntry.getLoadedState() == null ) { //ie. the entity came in from update()
			currentState = persister.getPropertyValues( entity, session.getEntityMode() );
		}
		else {
			currentState = entityEntry.getLoadedState();
		}

		final Object[] deletedState = createDeletedState( persister, currentState, session );
		entityEntry.setDeletedState( deletedState );

		session.getInterceptor().onDelete(
				entity,
				entityEntry.getId(),
				deletedState,
				persister.getPropertyNames(),
				propTypes
		);

		// before any callbacks, etc, so subdeletions see that this deletion happened first
		persistenceContext.setEntryStatus( entityEntry, Status.DELETED );
		EntityKey key = new EntityKey( entityEntry.getId(), persister, session.getEntityMode() );

		cascadeBeforeDelete( session, persister, entity, entityEntry, transientEntities );

		new ForeignKeys.Nullifier( entity, true, false, session )
				.nullifyTransientReferences( entityEntry.getDeletedState(), propTypes );
		new Nullability( session ).checkNullability( entityEntry.getDeletedState(), persister, true );
		persistenceContext.getNullifiableEntityKeys().add( key );

		// Ensures that containing deletions happen before sub-deletions
		session.getActionQueue().addAction(
				new EntityDeleteAction(
						entityEntry.getId(),
						deletedState,
						version,
						entity,
						persister,
						isCascadeDeleteEnabled,
						session
				)
		);

		cascadeAfterDelete( session, persister, entity, transientEntities );

		// the entry will be removed after the flush, and will no longer
		// override the stale snapshot
		// This is now handled by removeEntity() in EntityDeleteAction
		//persistenceContext.removeDatabaseSnapshot(key);
	}

	private Object[] createDeletedState(EntityPersister persister, Object[] currentState, EventSource session) {
		Type[] propTypes = persister.getPropertyTypes();
		final Object[] deletedState = new Object[propTypes.length];
//		TypeFactory.deepCopy( currentState, propTypes, persister.getPropertyUpdateability(), deletedState, session );
		boolean[] copyability = new boolean[propTypes.length];
		java.util.Arrays.fill( copyability, true );
		TypeFactory.deepCopy( currentState, propTypes, copyability, deletedState, session );
		return deletedState;
	}

	protected boolean invokeDeleteLifecycle(EventSource session, Object entity, EntityPersister persister) {
		if ( persister.implementsLifecycle( session.getEntityMode() ) ) {
			log.debug( "calling onDelete()" );
			if ( ( ( Lifecycle ) entity ).onDelete( session ) ) {
				log.debug( "deletion vetoed by onDelete()" );
				return true;
			}
		}
		return false;
	}

	protected void cascadeBeforeDelete(
			EventSource session,
			EntityPersister persister,
			Object entity,
			EntityEntry entityEntry,
			Set transientEntities) throws HibernateException {

		CacheMode cacheMode = session.getCacheMode();
		session.setCacheMode( CacheMode.GET );
		session.getPersistenceContext().incrementCascadeLevel();
		try {
			// cascade-delete to collections BEFORE the collection owner is deleted
			new Cascade( CascadingAction.DELETE, Cascade.AFTER_INSERT_BEFORE_DELETE, session )
					.cascade( persister, entity, transientEntities );
		}
		finally {
			session.getPersistenceContext().decrementCascadeLevel();
			session.setCacheMode( cacheMode );
		}
	}

	protected void cascadeAfterDelete(
			EventSource session,
			EntityPersister persister,
			Object entity,
			Set transientEntities) throws HibernateException {

		CacheMode cacheMode = session.getCacheMode();
		session.setCacheMode( CacheMode.GET );
		session.getPersistenceContext().incrementCascadeLevel();
		try {
			// cascade-delete to many-to-one AFTER the parent was deleted
			new Cascade( CascadingAction.DELETE, Cascade.BEFORE_INSERT_AFTER_DELETE, session )
					.cascade( persister, entity, transientEntities );
		}
		finally {
			session.getPersistenceContext().decrementCascadeLevel();
			session.setCacheMode( cacheMode );
		}
	}

}
