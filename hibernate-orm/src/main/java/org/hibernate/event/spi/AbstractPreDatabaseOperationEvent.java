/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.secure.spi.PermissionCheckEntityInformation;

/**
 * Represents an operation we are about to perform against the database.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPreDatabaseOperationEvent
		extends AbstractEvent
		implements PermissionCheckEntityInformation {

	private final Object entity;
	private final Serializable id;
	private final EntityPersister persister;

	/**
	 * Constructs an event containing the pertinent information.
	 *
	 * @param source The session from which the event originated.
	 * @param entity The entity to be invloved in the database operation.
	 * @param id The entity id to be invloved in the database operation.
	 * @param persister The entity's persister.
	 */
	public AbstractPreDatabaseOperationEvent(
			EventSource source,
			Object entity,
			Serializable id,
			EntityPersister persister) {
		super( source );
		this.entity = entity;
		this.id = id;
		this.persister = persister;
	}

	/**
	 * Retrieves the entity involved in the database operation.
	 *
	 * @return The entity.
	 */
	@Override
	public Object getEntity() {
		return entity;
	}

	/**
	 * The id to be used in the database operation.
	 *
	 * @return The id.
	 */
	public Serializable getId() {
		return id;
	}

	/**
	 * The persister for the {@link #getEntity entity}.
	 *
	 * @return The entity persister.
	 */
	public EntityPersister getPersister() {
		return persister;
	}

	/**
	 * Getter for property 'source'.  This is the session from which the event
	 * originated.
	 * <p/>
	 * Some of the pre-* events had previous exposed the event source using
	 * getSource() because they had not originally extended from
	 * {@link AbstractEvent}.
	 *
	 * @return Value for property 'source'.
	 *
	 * @deprecated Use {@link #getSession} instead
	 */
	@Deprecated
	public EventSource getSource() {
		return getSession();
	}

	@Override
	public String getEntityName() {
		return persister.getEntityName();
	}

	@Override
	public Serializable getIdentifier() {
		return id;
	}
}
