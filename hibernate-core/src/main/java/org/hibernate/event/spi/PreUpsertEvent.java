/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Represents a pre-upsert event, which occurs just prior to
 * performing the upsert of an entity in the database.
 *
 * @author Gavin King
 */
public class PreUpsertEvent extends AbstractPreDatabaseOperationEvent {
	private final Object[] state;

	/**
	 * Constructs an event containing the pertinent information.
	 * @param entity The entity to be updated.
	 * @param id The id of the entity to use for updating.
	 * @param state The state to be updated.
	 * @param persister The entity's persister.
	 * @param source The session from which the event originated.
	 */
	public PreUpsertEvent(
			Object entity,
			Object id,
			Object[] state,
			EntityPersister persister,
			EventSource source) {
		super( source, entity, id, persister );
		this.state = state;
	}

	/**
	 * Retrieves the state to be used in the upsert.
	 *
	 * @return The current state.
	 */
	public Object[] getState() {
		return state;
	}
}
