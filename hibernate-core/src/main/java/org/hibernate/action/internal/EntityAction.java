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
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Base class for actions relating to insert/update/delete of an entity
 * instance.
 *
 * @author Gavin King
 */
public abstract class EntityAction
		implements Executable, Serializable, Comparable, AfterTransactionCompletionProcess {

	private final String entityName;
	private final Serializable id;

	private transient Object instance;
	private transient SessionImplementor session;
	private transient EntityPersister persister;

	/**
	 * Instantiate an action.
	 *
	 * @param session The session from which this action is coming.
	 * @param id The id of the entity
	 * @param instance The entity instance
	 * @param persister The entity persister
	 */
	protected EntityAction(SessionImplementor session, Serializable id, Object instance, EntityPersister persister) {
		this.entityName = persister.getEntityName();
		this.id = id;
		this.instance = instance;
		this.session = session;
		this.persister = persister;
	}

	@Override
	public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess() {
		return null;
	}

	@Override
	public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess() {
		return needsAfterTransactionCompletion()
				? this
				: null;
	}

	protected abstract boolean hasPostCommitEventListeners();

	public boolean needsAfterTransactionCompletion() {
		return persister.hasCache() || hasPostCommitEventListeners();
	}

	/**
	 * entity name accessor
	 *
	 * @return The entity name
	 */
	public String getEntityName() {
		return entityName;
	}

	/**
	 * entity id accessor
	 *
	 * @return The entity id
	 */
	public final Serializable getId() {
		if ( id instanceof DelayedPostInsertIdentifier ) {
			Serializable eeId = session.getPersistenceContext().getEntry( instance ).getId();
			return eeId instanceof DelayedPostInsertIdentifier ? null : eeId;
		}
		return id;
	}

	public final DelayedPostInsertIdentifier getDelayedId() {
		return DelayedPostInsertIdentifier.class.isInstance( id ) ?
				DelayedPostInsertIdentifier.class.cast( id ) :
				null;
	}

	/**
	 * entity instance accessor
	 *
	 * @return The entity instance
	 */
	public final Object getInstance() {
		return instance;
	}

	/**
	 * originating session accessor
	 *
	 * @return The session from which this action originated.
	 */
	public final SessionImplementor getSession() {
		return session;
	}

	/**
	 * entity persister accessor
	 *
	 * @return The entity persister
	 */
	public final EntityPersister getPersister() {
		return persister;
	}

	@Override
	public final Serializable[] getPropertySpaces() {
		return persister.getPropertySpaces();
	}

	@Override
	public void beforeExecutions() {
		throw new AssertionFailure( "beforeExecutions() called for non-collection action" );
	}

	@Override
	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + MessageHelper.infoString( entityName, id );
	}

	@Override
	public int compareTo(Object other) {
		EntityAction action = ( EntityAction ) other;
		//sort first by entity name
		int roleComparison = entityName.compareTo( action.entityName );
		if ( roleComparison != 0 ) {
			return roleComparison;
		}
		else {
			//then by id
			return persister.getIdentifierType().compare( id, action.id );
		}
	}

	/**
	 * Reconnect to session after deserialization...
	 *
	 * @param session The session being deserialized
	 */
	public void afterDeserialize(SessionImplementor session) {
		if ( this.session != null || this.persister != null ) {
			throw new IllegalStateException( "already attached to a session." );
		}
		// IMPL NOTE: non-flushed changes code calls this method with session == null...
		// guard against NullPointerException
		if ( session != null ) {
			this.session = session;
			this.persister = session.getFactory().getEntityPersister( entityName );
			this.instance = session.getPersistenceContext().getEntity( session.generateEntityKey( id, persister ) );
		}
	}

	protected <T> EventListenerGroup<T> listenerGroup(EventType<T> eventType) {
		return getSession()
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( eventType );
	}

	protected EventSource eventSource() {
		return (EventSource) getSession();
	}
}

