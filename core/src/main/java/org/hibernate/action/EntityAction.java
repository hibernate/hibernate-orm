//$Id: EntityAction.java 11402 2007-04-11 14:24:35Z steve.ebersole@jboss.com $
package org.hibernate.action;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.util.StringHelper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Base class for actions relating to insert/update/delete of an entity
 * instance.
 *
 * @author Gavin King
 */
public abstract class EntityAction implements Executable, Serializable, Comparable {

	private final String entityName;
	private final Serializable id;
	private final Object instance;
	private final SessionImplementor session;

	private transient EntityPersister persister;

	/**
	 * Instantiate an action.
	 *
	 * @param session The session from which this action is coming.
	 * @param id The id of the entity
	 * @param instance The entiyt instance
	 * @param persister The entity persister
	 */
	protected EntityAction(SessionImplementor session, Serializable id, Object instance, EntityPersister persister) {
		this.entityName = persister.getEntityName();
		this.id = id;
		this.instance = instance;
		this.session = session;
		this.persister = persister;
	}

	protected abstract boolean hasPostCommitEventListeners();

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
			return session.getPersistenceContext().getEntry( instance ).getId();
		}
		return id;
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

	public final Serializable[] getPropertySpaces() {
		return persister.getPropertySpaces();
	}

	public void beforeExecutions() {
		throw new AssertionFailure( "beforeExecutions() called for non-collection action" );
	}

	public boolean hasAfterTransactionCompletion() {
		return persister.hasCache() || hasPostCommitEventListeners();
	}

	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + MessageHelper.infoString( entityName, id );
	}

	public int compareTo(Object other) {
		EntityAction action = ( EntityAction ) other;
		//sort first by entity name
		int roleComparison = entityName.compareTo( action.entityName );
		if ( roleComparison != 0 ) {
			return roleComparison;
		}
		else {
			//then by id
			return persister.getIdentifierType().compare( id, action.id, session.getEntityMode() );
		}
	}

	/**
	 * Serialization...
	 *
	 * @param ois Thed object stream
	 * @throws IOException Problem performing the default stream reading
	 * @throws ClassNotFoundException Problem performing the default stream reading
	 */
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		persister = session.getFactory().getEntityPersister( entityName );
	}
}

