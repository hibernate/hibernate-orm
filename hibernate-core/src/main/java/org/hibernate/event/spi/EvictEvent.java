/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 *  Defines an event class for the evicting of an entity.
 *
 * @author Steve Ebersole
 */
public class EvictEvent extends AbstractEvent {

	private Object object;

	public EvictEvent(Object object, EventSource source) {
		super(source);
		this.object = object;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
}
