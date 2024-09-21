/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem registering an event listener.
 *
 * @author Steve Ebersole
 */
public class EventListenerRegistrationException extends HibernateException {
	public EventListenerRegistrationException(String s) {
		super( s );
	}

	public EventListenerRegistrationException(String string, Throwable root) {
		super( string, root );
	}
}
