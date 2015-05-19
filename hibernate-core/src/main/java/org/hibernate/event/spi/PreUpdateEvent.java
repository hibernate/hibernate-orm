/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Represents a <tt>pre-update</tt> event, which occurs just prior to
 * performing the update of an entity in the database.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PreUpdateEvent extends AbstractPreDatabaseOperationEvent {
	private Object[] state;
	private Object[] oldState;

	/**
	 * Constructs an event containing the pertinent information.
	 *
	 * @param entity The entity to be updated.
	 * @param id The id of the entity to use for updating.
	 * @param state The state to be updated.
	 * @param oldState The state of the entity at the time it was loaded from
	 * the database.
	 * @param persister The entity's persister.
	 * @param source The session from which the event originated.
	 */
	public PreUpdateEvent(
			Object entity,
			Serializable id,
			Object[] state,
			Object[] oldState,
			EntityPersister persister,
			EventSource source) {
		super( source, entity, id, persister );
		this.state = state;
		this.oldState = oldState;
	}

	/**
	 * Retrieves the state to be used in the update.
	 *
	 * @return The current state.
	 */
	public Object[] getState() {
		return state;
	}

	/**
	 * The old state of the entity at the time it was last loaded from the
	 * database; can be null in the case of detached entities.
	 *
	 * @return The loaded state, or null.
	 */
	public Object[] getOldState() {
		return oldState;
	}
}
