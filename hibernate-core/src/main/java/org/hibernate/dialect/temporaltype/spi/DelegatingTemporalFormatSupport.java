/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.spi;

import org.hibernate.SPI;
import org.hibernate.sql.spi.SqlAppender;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base forwarding temporal-format translation to a stable delegate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingTemporalFormatSupport implements TemporalFormatSupport {
	private final TemporalFormatSupport delegate;

	@SPI(IMPLEMENT)
	protected DelegatingTemporalFormatSupport(TemporalFormatSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override
	public void appendFormat(SqlAppender appender, String format) {
		delegate.appendFormat( appender, format );
	}
}
