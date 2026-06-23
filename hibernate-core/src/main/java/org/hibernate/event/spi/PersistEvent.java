/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


/**
 * Event class for {@link org.hibernate.Session#persist}.
 *
 * @author Gavin King
 *
 * @see org.hibernate.Session#persist
 */
public class PersistEvent extends AbstractSessionEvent {

	private Object object;
	private String entityName;

	public PersistEvent(@Nullable String entityName, @Nonnull Object original, @Nonnull EventSource source) {
		this(original, source);
		this.entityName = entityName;
	}

	public PersistEvent(@Nonnull Object object, @Nonnull EventSource source) {
		super(source);
		if ( object == null ) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		this.object = object;
	}

	@Nonnull
	public Object getObject() {
		return object;
	}

	public void setObject(@Nonnull Object object) {
		this.object = object;
	}

	@Nullable
	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(@Nullable String entityName) {
		this.entityName = entityName;
	}

}
