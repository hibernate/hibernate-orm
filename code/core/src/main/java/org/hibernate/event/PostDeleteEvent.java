//$Id: PostDeleteEvent.java 10680 2006-11-01 22:53:30Z epbernard $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs after deleting an item from the datastore
 * 
 * @author Gavin King
 */
public class PostDeleteEvent extends AbstractEvent {
	private Object entity;
	private EntityPersister persister;
	private Serializable id;
	private Object[] deletedState;
	
	public PostDeleteEvent(
			Object entity, 
			Serializable id,
			Object[] deletedState,
			EntityPersister persister,
			EventSource source
	) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.persister = persister;
		this.deletedState = deletedState;
	}
	
	public Serializable getId() {
		return id;
	}
	public EntityPersister getPersister() {
		return persister;
	}
	public Object getEntity() {
		return entity;
	}
	public Object[] getDeletedState() {
		return deletedState;
	}
}
