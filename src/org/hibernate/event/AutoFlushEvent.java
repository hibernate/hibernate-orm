///$Id$
package org.hibernate.event;

import java.util.Set;


/** Defines an event class for the auto-flushing of a session.
 *
 * @author Steve Ebersole
 */
public class AutoFlushEvent extends FlushEvent {

	private Set querySpaces;
	private boolean flushRequired;

	public AutoFlushEvent(Set querySpaces, EventSource source) {
		super(source);
		this.querySpaces = querySpaces;
	}

	public Set getQuerySpaces() {
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
}
