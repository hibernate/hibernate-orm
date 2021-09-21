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
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

/**
 * @author Steve Ebersole
 */
public class JdbcLiteralFormatterTemporal extends BasicJdbcLiteralFormatter {
	private final TemporalType precision;

	public JdbcLiteralFormatterTemporal(JavaTypeDescriptor javaTypeDescriptor, TemporalType precision) {
		super( javaTypeDescriptor );
		this.precision = precision;
	}

	@Override
	public String toJdbcLiteral(Object value, Dialect dialect, WrapperOptions wrapperOptions) {
		final TimeZone jdbcTimeZone;
		if ( wrapperOptions == null || wrapperOptions.getJdbcTimeZone() == null ) {
			jdbcTimeZone = TimeZone.getDefault();
		}
		else {
			jdbcTimeZone = wrapperOptions.getJdbcTimeZone();
		}
		// for performance reasons, avoid conversions if we can
		if ( value instanceof java.util.Date ) {
			return dialect.formatDateTimeLiteral(
					(java.util.Date) value,
					precision,
					jdbcTimeZone
			);
		}
		else if ( value instanceof java.util.Calendar ) {
			return dialect.formatDateTimeLiteral(
					(java.util.Calendar) value,
					precision,
					jdbcTimeZone
			);
		}
		else if ( value instanceof TemporalAccessor ) {
			return dialect.formatDateTimeLiteral(
					(TemporalAccessor) value,
					precision,
					jdbcTimeZone
			);
		}

		switch ( precision) {
			case DATE: {
				return dialect.formatDateTimeLiteral(
						unwrap( value, java.sql.Date.class, wrapperOptions ),
						precision,
						jdbcTimeZone
				);
			}
			case TIME: {
				return dialect.formatDateTimeLiteral(
						unwrap( value, java.sql.Time.class, wrapperOptions ),
						precision,
						jdbcTimeZone
				);
			}
			default: {
				return dialect.formatDateTimeLiteral(
						unwrap( value, java.util.Date.class, wrapperOptions ),
						precision,
						jdbcTimeZone
				);
			}
		}
	}
}
