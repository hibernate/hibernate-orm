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
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.secure.spi.PermissionCheckEntityInformation;


/**
 * Represents a <tt>pre-delete</tt> event, which occurs just prior to
 * performing the deletion of an entity from the database.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PreDeleteEvent
		extends AbstractPreDatabaseOperationEvent
		implements PermissionCheckEntityInformation {

	private Object[] deletedState;

	/**
	 * Constructs an event containing the pertinent information.
	 *
	 * @param entity The entity to be deleted.
	 * @param id The id to use in the deletion.
	 * @param deletedState The entity's state at deletion time.
	 * @param persister The entity's persister.
	 * @param source The session from which the event originated.
	 */
	public PreDeleteEvent(
			Object entity,
			Serializable id,
			Object[] deletedState,
			EntityPersister persister,
			EventSource source) {
		super( source, entity, id, persister );
		this.deletedState = deletedState;
	}

	/**
	 * Getter for property 'deletedState'.  This is the entity state at the
	 * time of deletion (useful for optomistic locking and such).
	 *
	 * @return Value for property 'deletedState'.
	 */
	public Object[] getDeletedState() {
		return deletedState;
	}

}
