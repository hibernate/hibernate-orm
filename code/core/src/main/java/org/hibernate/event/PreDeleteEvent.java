//$Id: PreDeleteEvent.java 7581 2005-07-20 22:48:22Z oneovthafew $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs before deleting an item from the datastore
 * 
 * @author Gavin King
 */
public class PreDeleteEvent {
	private Object entity;
	private EntityPersister persister;
	private Serializable id;
	private Object[] deletedState;
	
	public Object getEntity() {
		return entity;
	}
	public Serializable getId() {
		return id;
	}
	public EntityPersister getPersister() {
		return persister;
	}
	public Object[] getDeletedState() {
		return deletedState;
	}
	
	public PreDeleteEvent(
			Object entity, 
			Serializable id,
			Object[] deletedState,
			EntityPersister persister
	) {
		this.entity = entity;
		this.persister = persister;
		this.id = id;
		this.deletedState = deletedState;
	}

}
