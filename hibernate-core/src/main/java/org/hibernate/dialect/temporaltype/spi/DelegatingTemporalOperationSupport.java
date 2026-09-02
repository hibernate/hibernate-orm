/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.spi;

import jakarta.annotation.Nullable;
import jakarta.persistence.TemporalType;

import org.hibernate.SPI;
import org.hibernate.query.common.TemporalUnit;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base forwarding temporal operations to a stable delegate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingTemporalOperationSupport implements TemporalOperationSupport {
	private final TemporalOperationSupport delegate;

	@SPI(IMPLEMENT)
	protected DelegatingTemporalOperationSupport(TemporalOperationSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override public String extractPattern(TemporalUnit unit) { return delegate.extractPattern( unit ); }
	@Override public String translateExtractField(TemporalUnit unit) { return delegate.translateExtractField( unit ); }
	@Override public String translateDurationField(TemporalUnit unit) { return delegate.translateDurationField( unit ); }
	@Override public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, @Nullable IntervalType intervalType) { return delegate.timestampaddPattern( unit, temporalType, intervalType ); }
	@Override public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) { return delegate.timestampdiffPattern( unit, fromTemporalType, toTemporalType ); }
	@Override public long fractionalSecondPrecisionInNanos() { return delegate.fractionalSecondPrecisionInNanos(); }
}
