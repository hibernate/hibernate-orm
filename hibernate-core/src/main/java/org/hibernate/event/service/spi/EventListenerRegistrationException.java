/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.spi;

import org.hibernate.HibernateException;
import jakarta.annotation.Nonnull;

/**
 * Indicates a problem registering an event listener.
 *
 * @author Steve Ebersole
 */
public class EventListenerRegistrationException extends HibernateException {
	public EventListenerRegistrationException(@Nonnull String s) {
		super( s );
	}

	public EventListenerRegistrationException(@Nonnull String string, @Nonnull Throwable root) {
		super( string, root );
	}
}
