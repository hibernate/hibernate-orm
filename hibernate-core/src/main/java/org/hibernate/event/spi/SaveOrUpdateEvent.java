/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.EntityEntry;

/** 
 * An event class for saveOrUpdate()
 *
 * @author Steve Ebersole
 */
public class SaveOrUpdateEvent extends AbstractEvent {
	private Object object;
	private Object requestedId;
	private String entityName;
	private Object entity;
	private EntityEntry entry;

	private Object resultId;

	public SaveOrUpdateEvent(String entityName, Object original, EventSource source) {
		this(original, source);
		this.entityName = entityName;
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

	public Object getRequestedId() {
		return requestedId;
	}

	public void setRequestedId(Object requestedId) {
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

	public Object getResultId() {
		return resultId;
	}

	public void setResultId(Object resultId) {
		this.resultId = resultId;
	}
}
