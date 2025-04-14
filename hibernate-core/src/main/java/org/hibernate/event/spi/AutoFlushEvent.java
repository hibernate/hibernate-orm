/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import java.util.Set;

/**
 * Event class for {@link org.hibernate.FlushMode#AUTO automatic}
 * stateful session flush.
 *
 * @author Steve Ebersole
 */
public class AutoFlushEvent extends FlushEvent {

	private Set<String> querySpaces;
	private boolean flushRequired;
	private final boolean skipPreFlush;

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

	public void setQuerySpaces(Set<String> querySpaces) {
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
