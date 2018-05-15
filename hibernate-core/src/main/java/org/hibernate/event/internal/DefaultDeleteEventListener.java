/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.List;
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
import org.hibernate.event.service.spi.JpaBootstrapSensitive;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.internal.TypeHelper;

/**
 * Defines the default delete event listener used by hibernate for deleting entities
 * from the datastore in response to generated delete events.
 *
 * @author Steve Ebersole
 */
public class DefaultDeleteEventListener implements DeleteEventListener, CallbackRegistryConsumer, JpaBootstrapSensitive {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultDeleteEventListener.class );

	private CallbackRegistry callbackRegistry;
	private boolean jpaBootstrap;

	@Override
	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	public void wasJpaBootstrap(boolean wasJpaBootstrap) {
		this.jpaBootstrap = wasJpaBootstrap;
	}

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
		final EntityTypeDescriptor descriptor;
		final Object id;
		final Object version;

		if ( entityEntry == null ) {
			LOG.trace( "Entity was not persistent in delete processing" );

			descriptor = source.getEntityDescriptor( event.getEntityName(), entity );

			if ( ForeignKeys.isTransient( descriptor.getEntityName(), entity, null, source ) ) {
				deleteTransientEntity( source, entity, event.isCascadeDeleteEnabled(), descriptor, transientEntities );
				// EARLY EXIT!!!
				return;
			}
			performDetachedEntityDeletionCheck( event );

			id = descriptor.getIdentifier( entity, source );

			if ( id == null ) {
				throw new TransientObjectException(
						"the detached instance passed to delete() had a null identifier"
				);
			}

			final EntityKey key = source.generateEntityKey( id, descriptor );

			persistenceContext.checkUniqueness( key, entity );

			new OnUpdateVisitor( source, id, entity ).process( entity, descriptor );

			version = descriptor.getVersion( entity );

			entityEntry = persistenceContext.addEntity(
					entity,
					( descriptor.getHierarchy().getMutabilityPlan().isMutable() ? Status.MANAGED : Status.READ_ONLY ),
					descriptor.getPropertyValues( entity ),
					key,
					version,
					LockMode.NONE,
					true,
					descriptor,
					false
			);
		}
		else {
			LOG.trace( "Deleting a persistent instance" );

			if ( entityEntry.getStatus() == Status.DELETED || entityEntry.getStatus() == Status.GONE ) {
				LOG.trace( "Object was already deleted" );
				return;
			}
			descriptor = entityEntry.getDescriptor();
			id = entityEntry.getId();
			version = entityEntry.getVersion();
		}

		/*if ( !descriptor.isMutable() ) {
			throw new HibernateException(
					"attempted to delete an object of immutable class: " +
					MessageHelper.infoString(descriptor)
				);
		}*/

		if ( invokeDeleteLifecycle( source, entity, descriptor ) ) {
			return;
		}

		deleteEntity(
				source,
				entity,
				entityEntry,
				event.isCascadeDeleteEnabled(),
				event.isOrphanRemovalBeforeUpdates(),
				descriptor,
				transientEntities
		);

		if ( source.getFactory().getSettings().isIdentifierRollbackEnabled() ) {
			descriptor.resetIdentifier( entity, id, version, source );
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
		if ( jpaBootstrap ) {
			disallowDeletionOfDetached( event );
		}
		// ok in normal Hibernate usage to delete a detached entity; JPA however
		// forbids it, thus this is a hook for HEM to affect this behavior
	}

	private void disallowDeletionOfDetached(DeleteEvent event) {
		EventSource source = event.getSession();
		String entityName = event.getEntityName();
		EntityTypeDescriptor descriptor = source.getEntityDescriptor( entityName, event.getObject() );
		Object id =  descriptor.getIdentifier( event.getObject(), source );
		entityName = entityName == null ? source.guessEntityName( event.getObject() ) : entityName;
		throw new IllegalArgumentException( "Removing a detached instance " + entityName + "#" + id );
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
	 * @param descriptor The entity descriptor
	 * @param transientEntities A cache of already visited transient entities
	 * (to avoid infinite recursion).
	 */
	protected void deleteTransientEntity(
			EventSource session,
			Object entity,
			boolean cascadeDeleteEnabled,
			EntityTypeDescriptor descriptor,
			Set transientEntities) {
		LOG.handlingTransientEntity();
		if ( transientEntities.contains( entity ) ) {
			LOG.trace( "Already handled transient entity; skipping" );
			return;
		}
		transientEntities.add( entity );
		cascadeBeforeDelete( session, descriptor, entity, null, transientEntities );
		cascadeAfterDelete( session, descriptor, entity, transientEntities );
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
	 * @param entityDescriptor The entity Descriptor.
	 * @param transientEntities A cache of already deleted entities.
	 */
	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected final void deleteEntity(
			final EventSource session,
			final Object entity,
			final EntityEntry entityEntry,
			final boolean isCascadeDeleteEnabled,
			final boolean isOrphanRemovalBeforeUpdates,
			final EntityTypeDescriptor entityDescriptor,
			final Set transientEntities) {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Deleting {0}",
					MessageHelper.infoString( entityDescriptor, entityEntry.getId(), session.getFactory() )
			);
		}

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final Object version = entityEntry.getVersion();

		final Object[] currentState;
		if ( entityEntry.getLoadedState() == null ) {
			//ie. the entity came in from update()
			currentState = entityDescriptor.getPropertyValues( entity );
		}
		else {
			currentState = entityEntry.getLoadedState();
		}

		final Object[] deletedState = createDeletedState( entityDescriptor, currentState, session );
		entityEntry.setDeletedState( deletedState );

		session.getInterceptor().onDelete(
				entity,
				entityEntry.getId(),
				deletedState,
				entityDescriptor.getPropertyNames(),
				entityDescriptor.getPropertyJavaTypeDescriptors()
		);

		// before any callbacks, etc, so subdeletions see that this deletion happened first
		persistenceContext.setEntryStatus( entityEntry, Status.DELETED );
		final EntityKey key = session.generateEntityKey( entityEntry.getId(), entityDescriptor );

		cascadeBeforeDelete( session, entityDescriptor, entity, entityEntry, transientEntities );

		final List<NonIdPersistentAttribute<?, ?>> attributes = entityDescriptor.getPersistentAttributes();
		new ForeignKeys.Nullifier( entity, true, false, session )
				.nullifyTransientReferences( entityEntry.getDeletedState(), attributes );
		new Nullability( session ).checkNullability( entityEntry.getDeletedState(), entityDescriptor, Nullability.NullabilityCheckType.DELETE );
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
							entityDescriptor,
							isCascadeDeleteEnabled,
							session
					)
			);
		}
		else {
			// Ensures that containing deletions happen before sub-deletions
			session.getActionQueue().addAction(
					new EntityDeleteAction(
							entityEntry.getId(),
							deletedState,
							version,
							entity,
							entityDescriptor,
							isCascadeDeleteEnabled,
							session
					)
			);
		}

		cascadeAfterDelete( session, entityDescriptor, entity, transientEntities );

		// the entry will be removed after the flush, and will no longer
		// override the stale snapshot
		// This is now handled by removeEntity() in EntityDeleteAction
		//persistenceContext.removeDatabaseSnapshot(key);
	}

	@SuppressWarnings("unchecked")
	private Object[] createDeletedState(EntityTypeDescriptor entityDescriptor, Object[] currentState, EventSource session) {
		final Object[] deletedState = new Object[ currentState.length ];

		TypeHelper.deepCopy(
				entityDescriptor,
				currentState,
				deletedState,
				(navigable) -> true
		);

		return deletedState;
	}

	protected boolean invokeDeleteLifecycle(EventSource session, Object entity, EntityTypeDescriptor descriptor) {
		callbackRegistry.preRemove( entity );
		if ( descriptor.implementsLifecycle() ) {
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
			EntityTypeDescriptor descriptor,
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
					descriptor,
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
			EntityTypeDescriptor descriptor,
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
					descriptor,
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
