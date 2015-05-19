/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.logger;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger.Level;

final class TriggerOnPrefixLogListener implements LogListener, Triggerable {

	private final String expectedPrefix;
	private final AtomicBoolean triggered = new AtomicBoolean( false );

	public TriggerOnPrefixLogListener(String expectedPrefix) {
		this.expectedPrefix = expectedPrefix;
	}

	@Override
	public void loggedEvent(Level level, String renderedMessage, Throwable thrown) {
		if ( renderedMessage != null && renderedMessage.startsWith( expectedPrefix ) ) {
			triggered.set( true );
		}
	}

	@Override
	public boolean wasTriggered() {
		return triggered.get();
	}

	@Override
	public void reset() {
		triggered.set( false );
	}

}
