/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * The action for performing entity insertions when entity is using IDENTITY column identifier generation
 *
 * @see EntityInsertAction
 */
public class EntityIdentityInsertAction extends AbstractEntityInsertAction  {

	private final boolean isDelayed;
	private final EntityKey delayedEntityKey;
	private EntityKey entityKey;
	private Serializable generatedId;

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
			Object[] state,
			Object instance,
			EntityPersister persister,
			boolean isVersionIncrementDisabled,
			SharedSessionContractImplementor session,
			boolean isDelayed) {
		super(
				( isDelayed ? generateDelayedPostInsertIdentifier() : null ),
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

		final EntityPersister persister = getPersister();
		final SharedSessionContractImplementor session = getSession();
		final Object instance = getInstance();

		setVeto( preInsert() );

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !isVeto() ) {
			generatedId = persister.insert( getState(), instance, session );
			if ( persister.hasInsertGeneratedProperties() ) {
				persister.processInsertGeneratedProperties( generatedId, instance, getState(), session );
			}
			//need to do that here rather than in the save event listener to let
			//the post insert events to have a id-filled entity when IDENTITY is used (EJB3)
			persister.setIdentifier( instance, generatedId, session );
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
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

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
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
		final EventListenerGroup<PostInsertEventListener> group = listenerGroup( EventType.POST_COMMIT_INSERT );
		for ( PostInsertEventListener listener : group.listeners() ) {
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
		final EventSource eventSource = eventSource();
		if ( isDelayed ) {
			eventSource.getPersistenceContextInternal().replaceDelayedEntityIdentityInsertKeys( delayedEntityKey, generatedId );
		}

		final EventListenerGroup<PostInsertEventListener> listenerGroup = listenerGroup( EventType.POST_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostInsertEvent event = new PostInsertEvent(
				getInstance(),
				generatedId,
				getState(),
				getPersister(),
				eventSource
		);
		for ( PostInsertEventListener listener : listenerGroup.listeners() ) {
			listener.onPostInsert( event );
		}
	}

	protected void postCommitInsert(boolean success) {
		final EventListenerGroup<PostInsertEventListener> listenerGroup = listenerGroup( EventType.POST_COMMIT_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostInsertEvent event = new PostInsertEvent(
				getInstance(),
				generatedId,
				getState(),
				getPersister(),
				eventSource()
		);
		for ( PostInsertEventListener listener : listenerGroup.listeners() ) {
			if ( PostCommitInsertEventListener.class.isInstance( listener ) ) {
				if ( success ) {
					listener.onPostInsert( event );
				}
				else {
					((PostCommitInsertEventListener) listener).onPostInsertCommitFailed( event );
				}
			}
			else {
				//default to the legacy implementation that always fires the event
				listener.onPostInsert( event );
			}
		}
	}

	protected boolean preInsert() {
		final EventListenerGroup<PreInsertEventListener> listenerGroup = listenerGroup( EventType.PRE_INSERT );
		if ( listenerGroup.isEmpty() ) {
			// NO_VETO
			return false;
		}
		boolean veto = false;
		final PreInsertEvent event = new PreInsertEvent( getInstance(), null, getState(), getPersister(), eventSource() );
		for ( PreInsertEventListener listener : listenerGroup.listeners() ) {
			veto |= listener.onPreInsert( event );
		}
		return veto;
	}

	/**
	 * Access to the generated identifier
	 *
	 * @return The generated identifier
	 */
	public final Serializable getGeneratedId() {
		return generatedId;
	}

	protected void setGeneratedId(Serializable generatedId) {
		this.generatedId = generatedId;
	}

	/**
	 * Access to the delayed entity key
	 *
	 * @return The delayed entity key
	 *
	 * @deprecated No Hibernate code currently uses this method
	 */
	@Deprecated
	@SuppressWarnings("UnusedDeclaration")
	public EntityKey getDelayedEntityKey() {
		return delayedEntityKey;
	}

	@Override
	public boolean isEarlyInsert() {
		return !isDelayed;
	}

	@Override
	protected EntityKey getEntityKey() {
		return entityKey != null ? entityKey : delayedEntityKey;
	}

	protected void setEntityKey(EntityKey entityKey) {
		this.entityKey = entityKey;
	}

	private static DelayedPostInsertIdentifier generateDelayedPostInsertIdentifier() {
		return new DelayedPostInsertIdentifier();
	}

	protected EntityKey generateDelayedEntityKey() {
		if ( !isDelayed ) {
			throw new AssertionFailure( "cannot request delayed entity-key for early-insert post-insert-id generation" );
		}
		return getSession().generateEntityKey( getDelayedId(), getPersister() );
	}
}
