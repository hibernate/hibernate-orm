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
package org.hibernate.event;

import java.io.Serializable;

/** 
 * An event class for merge() and saveOrUpdateCopy()
 *
 * @author Gavin King
 */
public class MergeEvent extends AbstractEvent {

	private Object original;
	private Serializable requestedId;
	private String entityName;
	private Object entity;
	private Object result;

	public MergeEvent(String entityName, Object original, EventSource source) {
		this(original, source);
		this.entityName = entityName;
	}

	public MergeEvent(String entityName, Object original, Serializable id, EventSource source) {
		this(entityName, original, source);
		this.requestedId = id;
		if ( requestedId == null ) {
			throw new IllegalArgumentException(
					"attempt to create merge event with null identifier"
				);
		}
	}

	public MergeEvent(Object object, EventSource source) {
		super(source);
		if ( object == null ) {
			throw new IllegalArgumentException(
					"attempt to create merge event with null entity"
				);
		}
		this.original = object;
	}

	public Object getOriginal() {
		return original;
	}

	public void setOriginal(Object object) {
		this.original = object;
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

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
}
