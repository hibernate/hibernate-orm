/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitymode.dom4j;

import org.hibernate.testing.logger.LogListener;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class LogListenerImpl implements LogListener {
	/**
	 * Singleton access
	 */
	public static final LogListenerImpl INSTANCE = new LogListenerImpl();

	@Override
	public void loggedEvent(Logger.Level level, String renderedMessage, Throwable thrown) {
		if ( renderedMessage != null && renderedMessage.startsWith( "HHH90000003: " ) ) {
			throw new AssertionError( "Deprecation message was triggered" );
		}
	}
}
