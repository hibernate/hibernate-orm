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
package org.hibernate.event.def;

import java.io.Serializable;

import org.hibernate.Hibernate;
import org.hibernate.PersistentObjectException;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Status;
import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.engine.SessionImplementor;

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
