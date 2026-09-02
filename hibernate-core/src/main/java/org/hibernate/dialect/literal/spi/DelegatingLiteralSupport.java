/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.literal.spi;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import jakarta.persistence.TemporalType;

import org.hibernate.SPI;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Forwards literal rendering to another supported strategy.
///
/// Override only the operations which require provider-specific decoration.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI({ USE, IMPLEMENT })
public class DelegatingLiteralSupport implements LiteralSupport {
	private final LiteralSupport delegate;

	@SPI( IMPLEMENT )
	public DelegatingLiteralSupport(LiteralSupport delegate) {
		this.delegate = requireNonNull( delegate, "delegate" );
	}

	protected final LiteralSupport delegate() {
		return delegate;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendLiteral(SqlAppender appender, String literal) {
		delegate.appendLiteral( appender, literal );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		delegate.appendBinaryLiteral( appender, bytes );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean value) {
		delegate.appendBooleanValueString( appender, value );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String toBooleanValueString(boolean value) {
		return delegate.toBooleanValueString( value );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendIntervalLiteral(SqlAppender appender, Duration literal) {
		delegate.appendIntervalLiteral( appender, literal );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendIntervalLiteral(SqlAppender appender, TemporalAmount literal) {
		delegate.appendIntervalLiteral( appender, literal );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendUUIDLiteral(SqlAppender appender, UUID literal) {
		delegate.appendUUIDLiteral( appender, literal );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendArrayLiteral(
			SqlAppender appender,
			Object[] literal,
			JdbcLiteralFormatter<Object> elementFormatter,
			WrapperOptions wrapperOptions) {
		delegate.appendArrayLiteral( appender, literal, elementFormatter, wrapperOptions );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		delegate.appendDateTimeLiteral( appender, temporalAccessor, precision, jdbcTimeZone );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Date date,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		delegate.appendDateTimeLiteral( appender, date, precision, jdbcTimeZone );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		delegate.appendDateTimeLiteral( appender, calendar, precision, jdbcTimeZone );
	}
}
