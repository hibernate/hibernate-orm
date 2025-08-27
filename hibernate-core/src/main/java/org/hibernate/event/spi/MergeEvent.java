/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Event class for {@link org.hibernate.Session#merge}.
 *
 * @author Gavin King
 *
 * @see org.hibernate.Session#merge
 */
public class MergeEvent extends AbstractEvent {

	private Object original;
	private Object requestedId;
	private String entityName;
	private Object entity;
	private Object result;

	public MergeEvent(String entityName, Object original, EventSource source) {
		this(original, source);
		this.entityName = entityName;
	}

	public MergeEvent(String entityName, Object original, Object id, EventSource source) {
		this(entityName, original, source);
		this.requestedId = id;
		if ( requestedId == null ) {
			throw new IllegalArgumentException( "Identifier may not be null" );
		}
	}

	public MergeEvent(Object object, EventSource source) {
		super(source);
		if ( object == null ) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		this.original = object;
	}

	public Object getOriginal() {
		return original;
	}

	public void setOriginal(Object object) {
		this.original = object;
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

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
}
