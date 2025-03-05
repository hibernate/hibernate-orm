/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * An event for {@link org.hibernate.Session#clear()} listening
 *
 * @author Steve Ebersole
 */
public class ClearEvent extends AbstractEvent {
	/**
	 * Constructs an event from the given event session.
	 *
	 * @param source The session event source.
	 */
	public ClearEvent(EventSource source) {
		super( source );
	}
}
