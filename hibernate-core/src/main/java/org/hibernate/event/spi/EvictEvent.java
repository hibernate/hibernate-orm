/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Event class for {@link org.hibernate.Session#evict}
 * and {@link org.hibernate.Session#detach}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#evict
 * @see org.hibernate.Session#detach
 */
public class EvictEvent extends AbstractEvent {

	private Object object;

	public EvictEvent(Object object, EventSource source) {
		super(source);
		if (object == null) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		this.object = object;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
}
