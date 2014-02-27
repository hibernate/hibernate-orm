/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * The action for performing an entity deletion.
 */
public class EntityDeleteAction extends EntityAction {
	private final Object version;
	private final boolean isCascadeDeleteEnabled;
	private final Object[] state;

	private SoftLock lock;
	private Object[] naturalIdValues;

	/**
	 * Constructs an EntityDeleteAction.
	 *
	 * @param id The entity identifier
	 * @param state The current (extracted) entity state
	 * @param version The current entity version
	 * @param instance The entity instance
	 * @param persister The entity persister
	 * @param isCascadeDeleteEnabled Whether cascade delete is enabled
	 * @param session The session
	 */
	public EntityDeleteAction(
			final Serializable id,
			final Object[] state,
			final Object version,
			final Object instance,
			final EntityPersister persister,
			final boolean isCascadeDeleteEnabled,
			final SessionImplementor session) {
		super( session, id, instance, persister );
		this.version = version;
		this.isCascadeDeleteEnabled = isCascadeDeleteEnabled;
		this.state = state;

		// before remove we need to remove the local (transactional) natural id cross-reference
		naturalIdValues = session.getPersistenceContext().getNaturalIdHelper().removeLocalNaturalIdCrossReference(
				getPersister(),
				getId(),
				state
		);
	}

	@Override
	public void execute() throws HibernateException {
		final Serializable id = getId();
		final EntityPersister persister = getPersister();
		final SessionImplementor session = getSession();
		final Object instance = getInstance();

		final boolean veto = preDelete();

		Object version = this.version;
		if ( persister.isVersionPropertyGenerated() ) {
			// we need to grab the version value from the entity, otherwise
			// we have issues with generated-version entities that may have
			// multiple actions queued during the same flush
			version = persister.getVersion( instance );
		}

		final CacheKey ck;
		if ( persister.hasCache() ) {
			ck = session.generateCacheKey( id, persister.getIdentifierType(), persister.getRootEntityName() );
			lock = persister.getCacheAccessStrategy().lockItem( ck, version );
		}
		else {
			ck = null;
		}

		if ( !isCascadeDeleteEnabled && !veto ) {
			persister.delete( id, version, instance, session );
		}
		
		//postDelete:
		// After actually deleting a row, record the fact that the instance no longer 
		// exists on the database (needed for identity-column key generation), and
		// remove it from the session cache
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityEntry entry = persistenceContext.removeEntry( instance );
		if ( entry == null ) {
			throw new AssertionFailure( "possible nonthreadsafe access to session" );
		}
		entry.postDelete();

		persistenceContext.removeEntity( entry.getEntityKey() );
		persistenceContext.removeProxy( entry.getEntityKey() );
		
		if ( persister.hasCache() ) {
			persister.getCacheAccessStrategy().remove( ck );
		}

		persistenceContext.getNaturalIdHelper().removeSharedNaturalIdCrossReference( persister, id, naturalIdValues );

		postDelete();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() && !veto ) {
			getSession().getFactory().getStatisticsImplementor().deleteEntity( getPersister().getEntityName() );
		}
	}

	private boolean preDelete() {
		boolean veto = false;
		final EventListenerGroup<PreDeleteEventListener> listenerGroup = listenerGroup( EventType.PRE_DELETE );
		if ( listenerGroup.isEmpty() ) {
			return veto;
		}
		final PreDeleteEvent event = new PreDeleteEvent( getInstance(), getId(), state, getPersister(), eventSource() );
		for ( PreDeleteEventListener listener : listenerGroup.listeners() ) {
			veto |= listener.onPreDelete( event );
		}
		return veto;
	}

	private void postDelete() {
		final EventListenerGroup<PostDeleteEventListener> listenerGroup = listenerGroup( EventType.POST_DELETE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostDeleteEvent event = new PostDeleteEvent(
				getInstance(),
				getId(),
				state,
				getPersister(),
				eventSource()
		);
		for ( PostDeleteEventListener listener : listenerGroup.listeners() ) {
			listener.onPostDelete( event );
		}
	}

	private void postCommitDelete(boolean success) {
		final EventListenerGroup<PostDeleteEventListener> listenerGroup = listenerGroup( EventType.POST_COMMIT_DELETE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostDeleteEvent event = new PostDeleteEvent(
				getInstance(),
				getId(),
				state,
				getPersister(),
				eventSource()
		);
		for ( PostDeleteEventListener listener : listenerGroup.listeners() ) {
			if ( PostCommitDeleteEventListener.class.isInstance( listener ) ) {
				if ( success ) {
					listener.onPostDelete( event );
				}
				else {
					((PostCommitDeleteEventListener) listener).onPostDeleteCommitFailed( event );
				}
			}
			else {
				//default to the legacy implementation that always fires the event
				listener.onPostDelete( event );
			}
		}
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SessionImplementor session) throws HibernateException {
		if ( getPersister().hasCache() ) {
			final CacheKey ck = getSession().generateCacheKey(
					getId(),
					getPersister().getIdentifierType(),
					getPersister().getRootEntityName()
			);
			getPersister().getCacheAccessStrategy().unlockItem( ck, lock );
		}
		postCommitDelete( success );
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		final EventListenerGroup<PostDeleteEventListener> group = listenerGroup( EventType.POST_COMMIT_DELETE );
		for ( PostDeleteEventListener listener : group.listeners() ) {
			if ( listener.requiresPostCommitHanding( getPersister() ) ) {
				return true;
			}
		}

		return false;
	}
}
