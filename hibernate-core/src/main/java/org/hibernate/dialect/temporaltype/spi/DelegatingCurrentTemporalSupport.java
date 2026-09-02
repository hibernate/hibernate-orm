/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.spi;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base forwarding the current-temporal contract to one stable delegate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingCurrentTemporalSupport implements CurrentTemporalSupport {
	private final CurrentTemporalSupport delegate;

	@SPI(IMPLEMENT)
	protected DelegatingCurrentTemporalSupport(CurrentTemporalSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override public String currentDate() { return delegate.currentDate(); }
	@Override public String currentTime() { return delegate.currentTime(); }
	@Override public String currentTimestamp() { return delegate.currentTimestamp(); }
	@Override public String currentLocalTime() { return delegate.currentLocalTime(); }
	@Override public String currentLocalTimestamp() { return delegate.currentLocalTimestamp(); }
	@Override public String currentTimestampWithTimeZone() { return delegate.currentTimestampWithTimeZone(); }
	@Override public @Nullable CurrentTimestampSelection getCurrentTimestampSelection() { return delegate.getCurrentTimestampSelection(); }
	@Override public boolean isCurrentTimestampStable() { return delegate.isCurrentTimestampStable(); }
}
