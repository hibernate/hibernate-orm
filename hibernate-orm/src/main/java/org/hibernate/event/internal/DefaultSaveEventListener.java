/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.Hibernate;
import org.hibernate.PersistentObjectException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.SaveOrUpdateEvent;

/**
 * An event handler for save() events
 * @author Gavin King
 */
public class DefaultSaveEventListener extends DefaultSaveOrUpdateEventListener {

	protected Serializable performSaveOrUpdate(SaveOrUpdateEvent event) {
		// this implementation is supposed to tolerate incorrect unsaved-value
		// mappings, for the purpose of backward-compatibility
		EntityEntry entry = event.getSession().getPersistenceContext().getEntry( event.getEntity() );
		if ( entry!=null && entry.getStatus() != Status.DELETED ) {
			return entityIsPersistent(event);
		}
		else {
			return entityIsTransient(event);
		}
	}
	
	protected Serializable saveWithGeneratedOrRequestedId(SaveOrUpdateEvent event) {
		if ( event.getRequestedId() == null ) {
			return super.saveWithGeneratedOrRequestedId(event);
		}
		else {
			return saveWithRequestedId( 
					event.getEntity(), 
					event.getRequestedId(), 
					event.getEntityName(), 
					null, 
					event.getSession() 
				);
		}
		
	}

	protected boolean reassociateIfUninitializedProxy(Object object, SessionImplementor source) {
		if ( !Hibernate.isInitialized(object) ) {
			throw new PersistentObjectException("uninitialized proxy passed to save()");
		}
		else {
			return false;
		}
	}
	

}
