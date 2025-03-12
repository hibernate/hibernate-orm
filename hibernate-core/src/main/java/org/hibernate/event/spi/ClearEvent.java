/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Event class for {@link org.hibernate.Session#clear}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#clear
 */
public class ClearEvent extends AbstractEvent {
	public ClearEvent(EventSource source) {
		super( source );
	}
}
