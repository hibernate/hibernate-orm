//$Id: DefaultSaveEventListener.java 8486 2005-10-29 10:32:45Z oneovthafew $
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
