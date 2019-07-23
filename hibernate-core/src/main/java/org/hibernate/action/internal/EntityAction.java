/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;

import org.jboss.logging.Logger;

/**
 * Base class for actions relating to insert/update/delete of an entity
 * instance.
 *
 * @author Gavin King
 */
public abstract class EntityAction
		implements Executable, Serializable, Comparable, AfterTransactionCompletionProcess {
	private static final Logger LOG = Logger.getLogger(EntityAction.class);

	private final String entityName;
	private final Serializable id;

	private transient Object instance;
	private transient SharedSessionContractImplementor session;
	private transient EntityPersister persister;

	private transient boolean veto;

	/**
	 * Instantiate an action.
	 *
	 * @param session The session from which this action is coming.
	 * @param id The id of the entity
	 * @param instance The entity instance
	 * @param persister The entity persister
	 */
	protected EntityAction(SharedSessionContractImplementor session, Serializable id, Object instance, EntityPersister persister) {
		this.entityName = persister.getEntityName();
		this.id = id;
		this.instance = instance;
		this.session = session;
		this.persister = persister;
	}

	public boolean isVeto() {
		return veto;
	}

	public void setVeto(boolean veto) {
		this.veto = veto;
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

	protected boolean needsAfterTransactionCompletion() {
		return persister.canWriteToCache() || hasPostCommitEventListeners();
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
			final EntityEntry entry = session.getPersistenceContextInternal().getEntry( instance );
			final Serializable eeId = entry == null ? null : entry.getId();
			return eeId instanceof DelayedPostInsertIdentifier ? null : eeId;
		}
		return id;
	}

	public final DelayedPostInsertIdentifier getDelayedId() {
		return DelayedPostInsertIdentifier.class.isInstance( id )
				? DelayedPostInsertIdentifier.class.cast( id )
				: null;
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
	public final SharedSessionContractImplementor getSession() {
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
		final EntityAction action = (EntityAction) other;
		//sort first by entity name
		final int roleComparison = entityName.compareTo( action.entityName );
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
	@Override
	public void afterDeserialize(SharedSessionContractImplementor session) {
		if ( this.session != null || this.persister != null ) {
			throw new IllegalStateException( "already attached to a session." );
		}
		// IMPL NOTE: non-flushed changes code calls this method with session == null...
		// guard against NullPointerException
		if ( session != null ) {
			this.session = session;
			this.persister = session.getFactory().getMetamodel().entityPersister( entityName );
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
