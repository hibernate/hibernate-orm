/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.util.Set;

/** Defines an event class for the auto-flushing of a session.
 *
 * @author Steve Ebersole
 */
public class AutoFlushEvent extends FlushEvent {

	private Set<String> querySpaces;
	private boolean flushRequired;
	private boolean skipPreFlush;

	public AutoFlushEvent(Set<String> querySpaces, EventSource source) {
		this( querySpaces, false, source );
	}

	public AutoFlushEvent(Set<String> querySpaces, boolean skipPreFlush, EventSource source) {
		super( source );
		this.querySpaces = querySpaces;
		this.skipPreFlush = skipPreFlush;
	}

	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	public void setQuerySpaces(Set querySpaces) {
		this.querySpaces = querySpaces;
	}

	public boolean isFlushRequired() {
		return flushRequired;
	}

	public void setFlushRequired(boolean dirty) {
		this.flushRequired = dirty;
	}

	public boolean isSkipPreFlush() {
		return skipPreFlush;
	}
}
