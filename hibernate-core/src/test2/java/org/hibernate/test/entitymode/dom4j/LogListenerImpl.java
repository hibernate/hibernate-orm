/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.entitymode.dom4j;

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
