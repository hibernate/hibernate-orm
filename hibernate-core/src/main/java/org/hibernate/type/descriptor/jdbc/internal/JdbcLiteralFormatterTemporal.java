/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
		if ( value instanceof java.util.Date ) {
			dialect.appendDateTimeLiteral(
					appender,
					(java.util.Date) value,
					precision,
					jdbcTimeZone
			);
		}
		else if ( value instanceof java.util.Calendar ) {
			dialect.appendDateTimeLiteral(
					appender,
					(java.util.Calendar) value,
					precision,
					jdbcTimeZone
			);
		}
		else if ( value instanceof TemporalAccessor ) {
			dialect.appendDateTimeLiteral(
					appender,
					(TemporalAccessor) value,
					precision,
					jdbcTimeZone
			);
		}
		else {
			switch ( precision ) {
				case DATE:
					dialect.appendDateTimeLiteral(
							appender,
							unwrap( value, java.sql.Date.class, options ),
							precision,
							jdbcTimeZone
					);
					break;
				case TIME:
					dialect.appendDateTimeLiteral(
							appender,
							unwrap( value, java.sql.Time.class, options ),
							precision,
							jdbcTimeZone
					);
					break;
				default:
					dialect.appendDateTimeLiteral(
							appender,
							unwrap( value, java.util.Date.class, options ),
							precision,
							jdbcTimeZone
					);
					break;
			}
		}
	}

	private static TimeZone getJdbcTimeZone(WrapperOptions options) {
		return options == null || options.getJdbcTimeZone() == null
				? TimeZone.getDefault()
				: options.getJdbcTimeZone();
	}
}
