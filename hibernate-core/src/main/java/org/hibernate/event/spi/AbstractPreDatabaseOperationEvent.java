/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
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
	private final Object id;
	private final EntityTypeDescriptor descriptor;

	/**
	 * Constructs an event containing the pertinent information.
	 *  @param source The session from which the event originated.
	 * @param entity The entity to be invloved in the database operation.
	 * @param id The entity id to be invloved in the database operation.
	 * @param descriptor The entity's descriptor.
	 */
	public AbstractPreDatabaseOperationEvent(
			EventSource source,
			Object entity,
			Object id,
			EntityTypeDescriptor descriptor) {
		super( source );
		this.entity = entity;
		this.id = id;
		this.descriptor = descriptor;
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
	public Object getId() {
		return id;
	}

	/**
	 * The descriptor for the {@link #getEntity entity}.
	 *
	 * @return The entity descriptor.
	 */
	public EntityTypeDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Getter for property 'source'.  This is the session from which the event
	 * originated.
	 * <p/>
	 * Some of the pre-* events had previous exposed the event source using
	 * getContainer() because they had not originally extended from
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
		return descriptor.getEntityName();
	}

	@Override
	public Object getIdentifier() {
		return id;
	}
}
