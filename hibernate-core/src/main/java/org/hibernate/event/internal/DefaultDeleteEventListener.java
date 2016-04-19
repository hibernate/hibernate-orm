/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.TransientObjectException;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.OrphanRemovalAction;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

/**
 * Defines the default delete event listener used by hibernate for deleting entities
 * from the datastore in response to generated delete events.
 *
 * @author Steve Ebersole
 */
public class DefaultDeleteEventListener implements DeleteEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultDeleteEventListener.class );

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
			LOG.trace( "Entity was not persistent in delete processing" );

			persister = source.getEntityPersister( event.getEntityName(), entity );

			if ( ForeignKeys.isTransient( persister.getEntityName(), entity, null, source ) ) {
				deleteTransientEntity( source, entity, event.isCascadeDeleteEnabled(), persister, transientEntities );
				// EARLY EXIT!!!
				return;
			}
			performDetachedEntityDeletionCheck( event );

			id = persister.getIdentifier( entity, source );

			if ( id == null ) {
				throw new TransientObjectException(
						"the detached instance passed to delete() had a null identifier"
				);
			}

			final EntityKey key = source.generateEntityKey( id, persister );

			persistenceContext.checkUniqueness( key, entity );

			new OnUpdateVisitor( source, id, entity ).process( entity, persister );

			version = persister.getVersion( entity );

			entityEntry = persistenceContext.addEntity(
					entity,
					(persister.isMutable() ? Status.MANAGED : Status.READ_ONLY),
					persister.getPropertyValues( entity ),
					key,
					version,
					LockMode.NONE,
					true,
					persister,
					false
			);
		}
		else {
			LOG.trace( "Deleting a persistent instance" );

			if ( entityEntry.getStatus() == Status.DELETED || entityEntry.getStatus() == Status.GONE ) {
				LOG.trace( "Object was already deleted" );
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

		deleteEntity(
				source,
				entity,
				entityEntry,
				event.isCascadeDeleteEnabled(),
				event.isOrphanRemovalBeforeUpdates(),
				persister,
				transientEntities
		);

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
		LOG.handlingTransientEntity();
		if ( transientEntities.contains( entity ) ) {
			LOG.trace( "Already handled transient entity; skipping" );
			return;
		}
		transientEntities.add( entity );
		cascadeBeforeDelete( session, persister, entity, null, transientEntities );
		cascadeAfterDelete( session, persister, entity, transientEntities );
	}

	/**
	 * Perform the entity deletion.  Well, as with most operations, does not
	 * really perform it; just schedules an action/execution with the
	 * {@link org.hibernate.engine.spi.ActionQueue} for execution during flush.
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
			final boolean isOrphanRemovalBeforeUpdates,
			final EntityPersister persister,
			final Set transientEntities) {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Deleting {0}",
					MessageHelper.infoString( persister, entityEntry.getId(), session.getFactory() )
			);
		}

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final Type[] propTypes = persister.getPropertyTypes();
		final Object version = entityEntry.getVersion();

		final Object[] currentState;
		if ( entityEntry.getLoadedState() == null ) {
			//ie. the entity came in from update()
			currentState = persister.getPropertyValues( entity );
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

		// beforeQuery any callbacks, etc, so subdeletions see that this deletion happened first
		persistenceContext.setEntryStatus( entityEntry, Status.DELETED );
		final EntityKey key = session.generateEntityKey( entityEntry.getId(), persister );

		cascadeBeforeDelete( session, persister, entity, entityEntry, transientEntities );

		new ForeignKeys.Nullifier( entity, true, false, session )
				.nullifyTransientReferences( entityEntry.getDeletedState(), propTypes );
		new Nullability( session ).checkNullability( entityEntry.getDeletedState(), persister, true );
		persistenceContext.getNullifiableEntityKeys().add( key );

		if ( isOrphanRemovalBeforeUpdates ) {
			// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.  This should be removed once action/task
			// ordering is improved.
			session.getActionQueue().addAction(
					new OrphanRemovalAction(
							entityEntry.getId(),
							deletedState,
							version,
							entity,
							persister,
							isCascadeDeleteEnabled,
							session
					)
			);
		}
		else {
			// Ensures that containing deletions happen beforeQuery sub-deletions
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
		}

		cascadeAfterDelete( session, persister, entity, transientEntities );

		// the entry will be removed afterQuery the flush, and will no longer
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
		TypeHelper.deepCopy( currentState, propTypes, copyability, deletedState, session );
		return deletedState;
	}

	protected boolean invokeDeleteLifecycle(EventSource session, Object entity, EntityPersister persister) {
		if ( persister.implementsLifecycle() ) {
			LOG.debug( "Calling onDelete()" );
			if ( ( (Lifecycle) entity ).onDelete( session ) ) {
				LOG.debug( "Deletion vetoed by onDelete()" );
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
			Cascade.cascade(
					CascadingActions.DELETE,
					CascadePoint.AFTER_INSERT_BEFORE_DELETE,
					session,
					persister,
					entity,
					transientEntities
			);
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
			Cascade.cascade(
					CascadingActions.DELETE,
					CascadePoint.BEFORE_INSERT_AFTER_DELETE,
					session,
					persister,
					entity,
					transientEntities
			);
		}
		finally {
			session.getPersistenceContext().decrementCascadeLevel();
			session.setCacheMode( cacheMode );
		}
	}
}
