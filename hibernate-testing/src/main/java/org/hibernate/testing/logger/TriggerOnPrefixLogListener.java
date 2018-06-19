/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger.Level;

final class TriggerOnPrefixLogListener implements LogListener, Triggerable {

	private Set<String> expectedPrefixes = new HashSet<>();

	private final List<String> triggerMessages = new CopyOnWriteArrayList<>();

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
					triggerMessages.add( renderedMessage );
				}
			}
		}
	}

	@Override
	public String triggerMessage() {
		return !triggerMessages.isEmpty() ? triggerMessages.get(0) : null;
	}

	@Override
	public List<String> triggerMessages() {
		return triggerMessages;
	}

	@Override
	public boolean wasTriggered() {
		return !triggerMessages.isEmpty();
	}

	@Override
	public void reset() {
		triggerMessages.clear();
	}
}
