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
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.FastSessionServices;
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
		implements Comparable<EntityAction>, Executable, Serializable, AfterTransactionCompletionProcess {

	private final String entityName;
	private final Object id;

	private transient Object instance;
	private transient EventSource session;
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
	protected EntityAction(
			EventSource session,
			Object id,
			Object instance,
			EntityPersister persister) {
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
		return needsAfterTransactionCompletion() ? this : null;
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
	public final Object getId() {
		if ( id instanceof DelayedPostInsertIdentifier ) {
			final EntityEntry entry = session.getPersistenceContextInternal().getEntry( instance );
			final Object eeId = entry == null ? null : entry.getId();
			return eeId instanceof DelayedPostInsertIdentifier ? null : eeId;
		}
		return id;
	}

	public final DelayedPostInsertIdentifier getDelayedId() {
		return id instanceof DelayedPostInsertIdentifier ? (DelayedPostInsertIdentifier) id : null;
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
	public final EventSource getSession() {
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
	public int compareTo(EntityAction action) {
		//sort first by entity name
		final int roleComparison = entityName.compareTo( action.getEntityName() );
		return roleComparison != 0
				? roleComparison
				//then by id
				: persister.getIdentifierType().compare( id, action.getId(), session.getSessionFactory() );
	}

	/**
	 * Reconnect to session after deserialization...
	 *
	 * @param session The session being deserialized
	 */
	@Override
	public void afterDeserialize(EventSource session) {
		if ( this.session != null || this.persister != null ) {
			throw new IllegalStateException( "already attached to a session." );
		}
		// IMPL NOTE: non-flushed changes code calls this method with session == null...
		// guard against NullPointerException
		if ( session != null ) {
			this.session = session;
			this.persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( entityName );
			this.instance = session.getPersistenceContext().getEntity( session.generateEntityKey( id, persister ) );
		}
	}

	protected EventSource eventSource() {
		return getSession();
	}

	/**
	 * Convenience method for all subclasses.
	 * @return the {@link FastSessionServices} instance from the SessionFactory.
	 */
	protected FastSessionServices getFastSessionServices() {
		return session.getFactory().getFastSessionServices();
	}

}
