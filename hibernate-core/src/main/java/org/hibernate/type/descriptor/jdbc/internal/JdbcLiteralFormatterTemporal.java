/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.internal;

import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;
import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

/**
 * {@link org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter}
 * implementation for handling date/time literals
 *
 * @author Steve Ebersole
 */
public class JdbcLiteralFormatterTemporal<T> extends BasicJdbcLiteralFormatter<T> {
	private final TemporalType precision;

	public JdbcLiteralFormatterTemporal(JavaType<T> javaType, TemporalType precision) {
		super( javaType );
		this.precision = precision;
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, Object value, Dialect dialect, WrapperOptions options) {
		final TimeZone jdbcTimeZone = getJdbcTimeZone( options );
		// for performance reasons, avoid conversions if we can
		if ( value instanceof java.util.Date date ) {
			dialect.appendDateTimeLiteral( appender, date, precision, jdbcTimeZone );
		}
		else if ( value instanceof java.util.Calendar calendar ) {
			dialect.appendDateTimeLiteral( appender, calendar, precision, jdbcTimeZone );
		}
		else if ( value instanceof TemporalAccessor temporalAccessor ) {
			dialect.appendDateTimeLiteral( appender, temporalAccessor, precision, jdbcTimeZone );
		}
		else {
			dialect.appendDateTimeLiteral( appender, unwrap( value, options ), precision, jdbcTimeZone );
		}
	}

	private java.util.Date unwrap(Object value, WrapperOptions options) {
		return switch ( precision ) {
			case DATE -> unwrap( value, java.sql.Date.class, options );
			case TIME -> unwrap( value, java.sql.Time.class, options );
			case TIMESTAMP -> unwrap( value, java.util.Date.class, options );
		};
	}

	private static TimeZone getJdbcTimeZone(WrapperOptions options) {
		return options == null || options.getJdbcTimeZone() == null
				? TimeZone.getDefault()
				: options.getJdbcTimeZone();
	}
}
