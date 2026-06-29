/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Event class for {@link org.hibernate.Session#evict}
 * and {@link org.hibernate.Session#detach}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#evict
 * @see org.hibernate.Session#detach
 */
public class EvictEvent extends AbstractSessionEvent {

	private Object object;

	public EvictEvent(@Nonnull Object object, @Nonnull EventSource source) {
		super(source);
		if (object == null) {
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
}
