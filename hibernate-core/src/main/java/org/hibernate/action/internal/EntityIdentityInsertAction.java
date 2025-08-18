/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * The action for performing entity insertions when entity is using {@code IDENTITY} column identifier generation
 *
 * @see EntityInsertAction
 */
public class EntityIdentityInsertAction extends AbstractEntityInsertAction  {

	private final boolean isDelayed;
	private final EntityKey delayedEntityKey;
	private EntityKey entityKey;
	private Object generatedId;
	private Object rowId;

	/**
	 * Constructs an EntityIdentityInsertAction
	 *
	 * @param state The current (extracted) entity state
	 * @param instance The entity instance
	 * @param persister The entity persister
	 * @param isVersionIncrementDisabled Whether version incrementing is disabled
	 * @param session The session
	 * @param isDelayed Are we in a situation which allows the insertion to be delayed?
	 *
	 * @throws HibernateException Indicates an illegal state
	 */
	public EntityIdentityInsertAction(
			final Object[] state,
			final Object instance,
			final EntityPersister persister,
			final boolean isVersionIncrementDisabled,
			final EventSource session,
			final boolean isDelayed) {
		super(
				isDelayed ? generateDelayedPostInsertIdentifier() : null,
				state,
				instance,
				isVersionIncrementDisabled,
				persister,
				session
		);
		this.isDelayed = isDelayed;
		this.delayedEntityKey = isDelayed ? generateDelayedEntityKey() : null;
	}

	@Override
	public void execute() throws HibernateException {
		nullifyTransientReferencesIfNotAlready();

		final var persister = getPersister();
		final var session = getSession();
		final Object instance = getInstance();

		setVeto( preInsert() );

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !isVeto() ) {
			final var eventMonitor = session.getEventMonitor();
			final var event = eventMonitor.beginEntityInsertEvent();
			boolean success = false;
			final GeneratedValues generatedValues;
			try {
				generatedValues = persister.getInsertCoordinator().insert( instance, getState(), session );
				generatedId = castNonNull( generatedValues ).getGeneratedValue( persister.getIdentifierMapping() );
				success = true;
			}
			finally {
				eventMonitor.completeEntityInsertEvent( event, generatedId, persister.getEntityName(), success, session );
			}
			final var persistenceContext = session.getPersistenceContextInternal();
			if ( persister.getRowIdMapping() != null ) {
				rowId = generatedValues.getGeneratedValue( persister.getRowIdMapping() );
				if ( rowId != null && isDelayed ) {
					persistenceContext.replaceEntityEntryRowId( getInstance(), rowId );
				}
			}
			if ( persister.hasInsertGeneratedProperties() ) {
				persister.processInsertGeneratedProperties( generatedId, instance, getState(), generatedValues, session );
			}
			//need to do that here rather than in the save event listener to let
			//the post insert events to have an id-filled entity when IDENTITY is used (EJB3)
			persister.setIdentifier( instance, generatedId, session );
			persistenceContext.registerInsertedKey( getPersister(), generatedId );
			entityKey = session.generateEntityKey( generatedId, persister );
			persistenceContext.checkUniqueness( entityKey, getInstance() );
		}

		//TODO: this bit actually has to be called after all cascades!
		//      but since identity insert is called *synchronously*,
		//      instead of asynchronously as other actions, it isn't
		/*if ( persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			cacheEntry = new CacheEntry(object, persister, session);
			persister.getCache().insert(generatedId, cacheEntry);
		}*/

		postInsert();

		final var statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() && !isVeto() ) {
			statistics.insertEntity( getPersister().getEntityName() );
		}

		markExecuted();
	}

	@Override
	public boolean needsAfterTransactionCompletion() {
		//TODO: simply remove this override if we fix the above todos
		return hasPostCommitEventListeners();
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		final var group = getEventListenerGroups().eventListenerGroup_POST_COMMIT_INSERT;
		for ( var listener : group.listeners() ) {
			if ( listener.requiresPostCommitHandling( getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
		//TODO: reenable if we also fix the above todo
		/*EntityPersister persister = getEntityPersister();
		if ( success && persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			persister.getCache().afterInsert( getGeneratedId(), cacheEntry );
		}*/
		postCommitInsert( success );
	}

	protected void postInsert() {
		if ( isDelayed ) {
			getSession().getPersistenceContextInternal()
					.replaceDelayedEntityIdentityInsertKeys( delayedEntityKey, generatedId );
		}
		getEventListenerGroups().eventListenerGroup_POST_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent, PostInsertEventListener::onPostInsert );
	}

	PostInsertEvent newPostInsertEvent() {
		return new PostInsertEvent( getInstance(), generatedId, getState(), getPersister(), eventSource() );
	}

	protected void postCommitInsert(boolean success) {
		getEventListenerGroups().eventListenerGroup_POST_COMMIT_INSERT
			.fireLazyEventOnEachListener( this::newPostInsertEvent,
					success ? PostInsertEventListener::onPostInsert : this::postCommitInsertOnFailure );
	}

	private void postCommitInsertOnFailure(PostInsertEventListener listener, PostInsertEvent event) {
		if ( listener instanceof PostCommitInsertEventListener postCommitInsertEventListener ) {
			postCommitInsertEventListener.onPostInsertCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostInsert( event );
		}
	}

	protected boolean preInsert() {
		final var listenerGroup = getEventListenerGroups().eventListenerGroup_PRE_INSERT;
		if ( listenerGroup.isEmpty() ) {
			// NO_VETO
			return false;
		}
		else {
			final PreInsertEvent event =
					new PreInsertEvent( getInstance(), null, getState(), getPersister(), eventSource() );
			boolean veto = false;
			for ( var listener : listenerGroup.listeners() ) {
				veto |= listener.onPreInsert( event );
			}
			return veto;
		}
	}

	/**
	 * Access to the generated identifier
	 *
	 * @return The generated identifier
	 */
	public final Object getGeneratedId() {
		return generatedId;
	}

	protected void setGeneratedId(Object generatedId) {
		this.generatedId = generatedId;
	}

	@Override
	public boolean isEarlyInsert() {
		return !isDelayed;
	}

	@Override
	protected EntityKey getEntityKey() {
		return entityKey != null ? entityKey : delayedEntityKey;
	}

	@Override
	public Object getRowId() {
		return rowId;
	}

	protected void setEntityKey(EntityKey entityKey) {
		this.entityKey = entityKey;
	}

	private static DelayedPostInsertIdentifier generateDelayedPostInsertIdentifier() {
		return new DelayedPostInsertIdentifier();
	}

	protected EntityKey generateDelayedEntityKey() {
		if ( isDelayed ) {
			return getSession().generateEntityKey( getDelayedId(), getPersister() );
		}
		else {
			throw new AssertionFailure( "cannot request delayed entity-key for early-insert post-insert-id generation" );
		}
	}
}
