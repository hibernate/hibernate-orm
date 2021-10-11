/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
 * @author Steve Ebersole
 */
public class JdbcLiteralFormatterTemporal extends BasicJdbcLiteralFormatter {
	private final TemporalType precision;

	public JdbcLiteralFormatterTemporal(JavaType<?> javaTypeDescriptor, TemporalType precision) {
		super( javaTypeDescriptor );
		this.precision = precision;
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, Object value, Dialect dialect, WrapperOptions wrapperOptions) {
		final TimeZone jdbcTimeZone;
		if ( wrapperOptions == null || wrapperOptions.getJdbcTimeZone() == null ) {
			jdbcTimeZone = TimeZone.getDefault();
		}
		else {
			jdbcTimeZone = wrapperOptions.getJdbcTimeZone();
		}
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
							unwrap( value, java.sql.Date.class, wrapperOptions ),
							precision,
							jdbcTimeZone
					);
					break;
				case TIME:
					dialect.appendDateTimeLiteral(
							appender,
							unwrap( value, java.sql.Time.class, wrapperOptions ),
							precision,
							jdbcTimeZone
					);
					break;
				default:
					dialect.appendDateTimeLiteral(
							appender,
							unwrap( value, java.util.Date.class, wrapperOptions ),
							precision,
							jdbcTimeZone
					);
					break;
			}
		}
	}
}
