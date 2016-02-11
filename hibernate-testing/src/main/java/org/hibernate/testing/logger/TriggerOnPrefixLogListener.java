/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger.Level;

final class TriggerOnPrefixLogListener implements LogListener, Triggerable {

	private Set<String> expectedPrefixes = new HashSet<>();
	private final AtomicBoolean triggered = new AtomicBoolean( false );
	private final AtomicReference<String> triggerMessage = new AtomicReference<String>( null );

	public TriggerOnPrefixLogListener(String expectedPrefix) {
		expectedPrefixes.add( expectedPrefix );
	}

	public TriggerOnPrefixLogListener(Set<String> expectedPrefixes) {
		this.expectedPrefixes = expectedPrefixes;
	}

	@Override
	public void loggedEvent(Level level, String renderedMessage, Throwable thrown) {
		if ( renderedMessage != null ) {
			for ( String expectedPrefix : expectedPrefixes ) {
				if ( renderedMessage.startsWith( expectedPrefix ) ) {
					triggered.set( true );
					triggerMessage.set( renderedMessage );
				}
			}
		}
	}

	@Override
	public String triggerMessage() {
		return triggerMessage.get();
	}

	@Override
	public boolean wasTriggered() {
		return triggered.get();
	}

	@Override
	public void reset() {
		triggered.set( false );
		triggerMessage.set( null );
	}
}
