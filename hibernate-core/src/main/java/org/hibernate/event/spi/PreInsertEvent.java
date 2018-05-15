/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * Represents a <tt>pre-insert</tt> event, which occurs just prior to
 * performing the insert of an entity into the database.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PreInsertEvent extends AbstractPreDatabaseOperationEvent {
	private Object[] state;

	/**
	 * Constructs an event containing the pertinent information.
	 *  @param entity The entity to be inserted.
	 * @param id The id to use in the insertion.
	 * @param state The state to be inserted.
	 * @param descriptor The entity's descriptor.
	 * @param source The session from which the event originated.
	 */
	public PreInsertEvent(
			Object entity,
			Object id,
			Object[] state,
			EntityTypeDescriptor descriptor,
			EventSource source) {
		super( source, entity, id, descriptor );
		this.state = state;
	}

	/**
	 * Getter for property 'state'.  These are the values to be inserted.
	 *
	 * @return Value for property 'state'.
	 */
	public Object[] getState() {
		return state;
	}
}
