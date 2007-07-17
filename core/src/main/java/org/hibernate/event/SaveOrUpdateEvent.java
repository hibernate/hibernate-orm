//$Id: SaveOrUpdateEvent.java 7785 2005-08-08 23:24:44Z oneovthafew $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.engine.EntityEntry;

/** 
 * An event class for saveOrUpdate()
 *
 * @author Steve Ebersole
 */
public class SaveOrUpdateEvent extends AbstractEvent {

	private Object object;
	private Serializable requestedId;
	private String entityName;
	private Object entity;
	private EntityEntry entry;
	private Serializable resultId;

	public SaveOrUpdateEvent(String entityName, Object original, EventSource source) {
		this(original, source);
		this.entityName = entityName;
	}

	public SaveOrUpdateEvent(String entityName, Object original, Serializable id, EventSource source) {
		this(entityName, original, source);
		this.requestedId = id;
		if ( requestedId == null ) {
			throw new IllegalArgumentException(
					"attempt to create saveOrUpdate event with null identifier"
				);
		}
	}

	public SaveOrUpdateEvent(Object object, EventSource source) {
		super(source);
		if ( object == null ) {
			throw new IllegalArgumentException(
					"attempt to create saveOrUpdate event with null entity"
				);
		}
		this.object = object;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public Serializable getRequestedId() {
		return requestedId;
	}

	public void setRequestedId(Serializable requestedId) {
		this.requestedId = requestedId;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public Object getEntity() {
		return entity;
	}
	
	public void setEntity(Object entity) {
		this.entity = entity;
	}
	
	public EntityEntry getEntry() {
		return entry;
	}
	
	public void setEntry(EntityEntry entry) {
		this.entry = entry;
	}

	public Serializable getResultId() {
		return resultId;
	}

	public void setResultId(Serializable resultId) {
		this.resultId = resultId;
	}
}
