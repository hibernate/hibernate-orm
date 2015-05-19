/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;

/**
 * An event handler for update() events
 * @author Gavin King
 */
public class DefaultUpdateEventListener extends DefaultSaveOrUpdateEventListener {

	protected Serializable performSaveOrUpdate(SaveOrUpdateEvent event) {
		// this implementation is supposed to tolerate incorrect unsaved-value
		// mappings, for the purpose of backward-compatibility
		EntityEntry entry = event.getSession().getPersistenceContext().getEntry( event.getEntity() );
		if ( entry!=null ) {
			if ( entry.getStatus()== Status.DELETED ) {
				throw new ObjectDeletedException( "deleted instance passed to update()", null, event.getEntityName() );
			}
			else {
				return entityIsPersistent(event);
			}
		}
		else {
			entityIsDetached(event);
			return null;
		}
	}
	
	/**
	 * If the user specified an id, assign it to the instance and use that, 
	 * otherwise use the id already assigned to the instance
	 */
	protected Serializable getUpdateId(
			Object entity,
			EntityPersister persister,
			Serializable requestedId,
			SessionImplementor session) throws HibernateException {
		if ( requestedId == null ) {
			return super.getUpdateId( entity, persister, requestedId, session );
		}
		else {
			persister.setIdentifier( entity, requestedId, session );
			return requestedId;
		}
	}

}
