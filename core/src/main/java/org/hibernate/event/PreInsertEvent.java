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
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;

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
	 *
	 * @param entity The entity to be inserted.
	 * @param id The id to use in the insertion.
	 * @param state The state to be inserted.
	 * @param persister The entity's persister.
	 * @param source The session from which the event originated.
	 */
	public PreInsertEvent(
			Object entity,
			Serializable id,
			Object[] state,
			EntityPersister persister,
			EventSource source) {
		super( source, entity, id, persister );
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
