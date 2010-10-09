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
package org.hibernate.action;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PreDeleteEvent;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.EventSource;
import org.hibernate.persister.entity.EntityPersister;

public final class EntityDeleteAction extends EntityAction {
	private final Object version;
	private final boolean isCascadeDeleteEnabled;
	private final Object[] state;

	private SoftLock lock;

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
	}

	public void execute() throws HibernateException {
		Serializable id = getId();
		EntityPersister persister = getPersister();
		SessionImplementor session = getSession();
		Object instance = getInstance();

		boolean veto = preDelete();

		Object version = this.version;
		if ( persister.isVersionPropertyGenerated() ) {
			// we need to grab the version value from the entity, otherwise
			// we have issues with generated-version entities that may have
			// multiple actions queued during the same flush
			version = persister.getVersion( instance, session.getEntityMode() );
		}

		final CacheKey ck;
		if ( persister.hasCache() ) {
			ck = new CacheKey( 
					id, 
					persister.getIdentifierType(), 
					persister.getRootEntityName(), 
					session.getEntityMode(), 
					session.getFactory() 
			);
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
		EntityEntry entry = persistenceContext.removeEntry( instance );
		if ( entry == null ) {
			throw new AssertionFailure( "possible nonthreadsafe access to session" );
		}
		entry.postDelete();

		persistenceContext.removeEntity( entry.getEntityKey() );
		persistenceContext.removeProxy( entry.getEntityKey() );
		
		if ( persister.hasCache() ) {
			persister.getCacheAccessStrategy().remove( ck );
		}

		postDelete();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() && !veto ) {
			getSession().getFactory().getStatisticsImplementor().deleteEntity( getPersister().getEntityName() );
		}
	}

	private boolean preDelete() {
		PreDeleteEventListener[] preListeners = getSession().getListeners().getPreDeleteEventListeners();
		boolean veto = false;
		if (preListeners.length>0) {
			PreDeleteEvent preEvent = new PreDeleteEvent( getInstance(), getId(), state, getPersister() ,(EventSource) getSession() );
			for ( int i = 0; i < preListeners.length; i++ ) {
				veto = preListeners[i].onPreDelete(preEvent) || veto;
			}
		}
		return veto;
	}

	private void postDelete() {
		PostDeleteEventListener[] postListeners = getSession().getListeners()
				.getPostDeleteEventListeners();
		if (postListeners.length>0) {
			PostDeleteEvent postEvent = new PostDeleteEvent(
					getInstance(),
					getId(),
					state,
					getPersister(),
					(EventSource) getSession() 
			);
			for ( int i = 0; i < postListeners.length; i++ ) {
				postListeners[i].onPostDelete(postEvent);
			}
		}
	}

	private void postCommitDelete() {
		PostDeleteEventListener[] postListeners = getSession().getListeners()
				.getPostCommitDeleteEventListeners();
		if (postListeners.length>0) {
			PostDeleteEvent postEvent = new PostDeleteEvent(
					getInstance(),
					getId(),
					state,
					getPersister(),
					(EventSource) getSession()
			);
			for ( int i = 0; i < postListeners.length; i++ ) {
				postListeners[i].onPostDelete(postEvent);
			}
		}
	}

	public void doAfterTransactionCompletion(boolean success, SessionImplementor session) throws HibernateException {
		if ( getPersister().hasCache() ) {
			final CacheKey ck = new CacheKey(
					getId(),
					getPersister().getIdentifierType(),
					getPersister().getRootEntityName(),
					getSession().getEntityMode(),
					getSession().getFactory()
			);
			getPersister().getCacheAccessStrategy().unlockItem( ck, lock );
		}
		postCommitDelete();
	}

	protected boolean hasPostCommitEventListeners() {
		return getSession().getListeners().getPostCommitDeleteEventListeners().length > 0;
	}
}
