/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import java.util.Set;
import jakarta.annotation.Nonnull;

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

	public AutoFlushEvent(@Nonnull Set<String> querySpaces, @Nonnull EventSource source) {
		this( querySpaces, false, source );
	}

	public AutoFlushEvent(@Nonnull Set<String> querySpaces, boolean skipPreFlush, @Nonnull EventSource source) {
		super( source );
		this.querySpaces = querySpaces;
		this.skipPreFlush = skipPreFlush;
	}

	@Nonnull
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	public void setQuerySpaces(@Nonnull Set<String> querySpaces) {
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
