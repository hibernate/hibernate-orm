/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

/**
 * An event class for merge() and saveOrUpdateCopy()
 *
 * @author Gavin King
 * @author Yanming Zhou
 */
public class MergeEvent extends AbstractEvent {

	private Object original;
	private Object requestedId;
	private String entityName;
	private Object entity;
	private Object result;
	private boolean cascading;

	public MergeEvent(String entityName, Object original, EventSource source, boolean cascading) {
		this(original, source);
		this.entityName = entityName;
		this.cascading = cascading;
	}

	public MergeEvent(String entityName, Object original, EventSource source) {
		this(original, source);
		this.entityName = entityName;
	}

	public MergeEvent(String entityName, Object original, Object id, EventSource source) {
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

	public boolean isCascading() {
		return cascading;
	}
}
