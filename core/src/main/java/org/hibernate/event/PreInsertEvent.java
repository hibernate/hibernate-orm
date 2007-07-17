//$Id: PreInsertEvent.java 7850 2005-08-11 19:37:08Z epbernard $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.engine.SessionImplementor;

/**
 * Occurs before inserting an item in the datastore
 * 
 * @author Gavin King
 */
public class PreInsertEvent {
	private Object entity;
	private EntityPersister persister;
	private Object[] state;
	private Serializable id;
	private SessionImplementor source;

	public PreInsertEvent(
			Object entity,
			Serializable id,
			Object[] state,
			EntityPersister persister,
			SessionImplementor source
	) {
		this.source = source;
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
	public SessionImplementor getSource() {
		return source;
	}
}
