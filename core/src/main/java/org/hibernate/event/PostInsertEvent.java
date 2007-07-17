//$Id: PostInsertEvent.java 10680 2006-11-01 22:53:30Z epbernard $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs after inserting an item in the datastore
 * 
 * @author Gavin King
 */
public class PostInsertEvent extends AbstractEvent {
	private Object entity;
	private EntityPersister persister;
	private Object[] state;
	private Serializable id;
	
	public PostInsertEvent(
			Object entity, 
			Serializable id,
			Object[] state,
			EntityPersister persister,
			EventSource source
	) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.state = state;
		this.persister = persister;
	}
	
	public Object getEntity() {
		return entity;
	}
	public Serializable getId() {
		return id;
	}
	public EntityPersister getPersister() {
		return persister;
	}
	public Object[] getState() {
		return state;
	}
}
