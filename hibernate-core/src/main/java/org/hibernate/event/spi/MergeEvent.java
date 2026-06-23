/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


/**
 * Event class for {@link org.hibernate.Session#merge}.
 *
 * @author Gavin King
 *
 * @see org.hibernate.Session#merge
 */
public class MergeEvent extends AbstractSessionEvent {

	private Object original;
	private Object requestedId;
	private String entityName;
	private Object entity;
	private Object result;

	public MergeEvent(@Nullable String entityName, @Nonnull Object original, @Nonnull EventSource source) {
		this(original, source);
		this.entityName = entityName;
	}

	public MergeEvent(@Nullable String entityName, @Nonnull Object original, @Nonnull Object id, @Nonnull EventSource source) {
		this(entityName, original, source);
		if ( id == null ) {
			throw new IllegalArgumentException( "Identifier may not be null" );
		}
		this.requestedId = id;
	}

	public MergeEvent(@Nonnull Object object, @Nonnull EventSource source) {
		super(source);
		if ( object == null ) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		this.original = object;
	}

	@Nonnull
	public Object getOriginal() {
		return original;
	}

	public void setOriginal(@Nonnull Object object) {
		this.original = object;
	}

	@Nullable
	public Object getRequestedId() {
		return requestedId;
	}

	public void setRequestedId(@Nullable Object requestedId) {
		this.requestedId = requestedId;
	}

	@Nullable
	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(@Nullable String entityName) {
		this.entityName = entityName;
	}

	@Nonnull
	public Object getEntity() {
		return entity;
	}

	public void setEntity(@Nonnull Object entity) {
		this.entity = entity;
	}

	@Nullable
	public Object getResult() {
		return result;
	}

	public void setResult(@Nullable Object result) {
		this.result = result;
	}
}
