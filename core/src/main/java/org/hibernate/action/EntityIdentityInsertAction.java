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

import org.hibernate.HibernateException;
import org.hibernate.AssertionFailure;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.EntityKey;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PreInsertEvent;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.EventSource;
import org.hibernate.persister.entity.EntityPersister;

public final class EntityIdentityInsertAction extends EntityAction  {

	private final Object[] state;
	private final boolean isDelayed;
	private final EntityKey delayedEntityKey;
	//private CacheEntry cacheEntry;
	private Serializable generatedId;

	public EntityIdentityInsertAction(
			Object[] state,
	        Object instance,
	        EntityPersister persister,
	        SessionImplementor session,
	        boolean isDelayed) throws HibernateException {
		super( session, null, instance, persister );
		this.state = state;
		this.isDelayed = isDelayed;
		this.delayedEntityKey = isDelayed ? generateDelayedEntityKey() : null;
	}

	public void execute() throws HibernateException {
		final EntityPersister persister = getPersister();
		final SessionImplementor session = getSession();
		final Object instance = getInstance();
		
		boolean veto = preInsert();

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !veto ) {
			generatedId = persister.insert( state, instance, session );
			if ( persister.hasInsertGeneratedProperties() ) {
				persister.processInsertGeneratedProperties( generatedId, instance, state, session );
			}
			//need to do that here rather than in the save event listener to let
			//the post insert events to have a id-filled entity when IDENTITY is used (EJB3)
			persister.setIdentifier( instance, generatedId, session );
		}


		//TODO: this bit actually has to be called after all cascades!
		//      but since identity insert is called *synchronously*,
		//      instead of asynchronously as other actions, it isn't
		/*if ( persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			cacheEntry = new CacheEntry(object, persister, session);
			persister.getCache().insert(generatedId, cacheEntry);
		}*/
		
		postInsert();

		if ( session.getFactory().getStatistics().isStatisticsEnabled() && !veto ) {
			session.getFactory().getStatisticsImplementor().insertEntity( getPersister().getEntityName() );
		}

	}

	public boolean needsAfterTransactionCompletion() {
		//TODO: simply remove this override if we fix the above todos
		return hasPostCommitEventListeners();
	}

	protected boolean hasPostCommitEventListeners() {
		return getSession().getListeners().getPostCommitInsertEventListeners().length>0;
	}

	public void doAfterTransactionCompletion(boolean success, SessionImplementor session) {
		//TODO: reenable if we also fix the above todo
		/*EntityPersister persister = getEntityPersister();
		if ( success && persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			persister.getCache().afterInsert( getGeneratedId(), cacheEntry );
		}*/
		postCommitInsert();
	}

	private void postInsert() {
		if ( isDelayed ) {
			getSession().getPersistenceContext().replaceDelayedEntityIdentityInsertKeys( delayedEntityKey, generatedId );
		}
		PostInsertEventListener[] postListeners = getSession().getListeners()
				.getPostInsertEventListeners();
		if (postListeners.length>0) {
			PostInsertEvent postEvent = new PostInsertEvent(
					getInstance(),
					generatedId,
					state,
					getPersister(),
					(EventSource) getSession()
			);
			for ( int i = 0; i < postListeners.length; i++ ) {
				postListeners[i].onPostInsert(postEvent);
			}
		}
	}

	private void postCommitInsert() {
		PostInsertEventListener[] postListeners = getSession().getListeners()
				.getPostCommitInsertEventListeners();
		if (postListeners.length>0) {
			PostInsertEvent postEvent = new PostInsertEvent(
					getInstance(),
					generatedId,
					state,
					getPersister(),
					(EventSource) getSession() 
			);
			for ( int i = 0; i < postListeners.length; i++ ) {
				postListeners[i].onPostInsert(postEvent);
			}
		}
	}

	private boolean preInsert() {
		PreInsertEventListener[] preListeners = getSession().getListeners()
				.getPreInsertEventListeners();
		boolean veto = false;
		if (preListeners.length>0) {
			PreInsertEvent preEvent = new PreInsertEvent( getInstance(), null, state, getPersister(), (EventSource)getSession() );
			for ( int i = 0; i < preListeners.length; i++ ) {
				veto = preListeners[i].onPreInsert(preEvent) || veto;
			}
		}
		return veto;
	}

	public final Serializable getGeneratedId() {
		return generatedId;
	}

	public EntityKey getDelayedEntityKey() {
		return delayedEntityKey;
	}

	private synchronized EntityKey generateDelayedEntityKey() {
		if ( !isDelayed ) {
			throw new AssertionFailure( "cannot request delayed entity-key for non-delayed post-insert-id generation" );
		}
		return new EntityKey( new DelayedPostInsertIdentifier(), getPersister(), getSession().getEntityMode() );
	}
}
