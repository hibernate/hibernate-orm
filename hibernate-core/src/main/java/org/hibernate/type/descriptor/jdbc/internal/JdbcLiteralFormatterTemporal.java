package org.hibernate.type.descriptor.jdbc.internal;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;
import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.spi.SqlAppender;
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
	public void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions options) {
		final TimeZone jdbcTimeZone = getJdbcTimeZone( options );
		// for performance reasons, avoid conversions if we can
		if ( value instanceof java.util.Date date ) {
			dialect.getLiteralSupport().appendDateTimeLiteral( appender, date, precision, jdbcTimeZone );
		}
		else if ( value instanceof java.util.Calendar calendar ) {
			dialect.getLiteralSupport().appendDateTimeLiteral( appender, calendar, precision, jdbcTimeZone );
		}
		else if ( value instanceof TemporalAccessor temporalAccessor ) {
			if ( TemporalPreservationGuard.isEnabled( options ) ) {
				TemporalPreservationGuard.checkLiteral( value, getJavaType(), options );
				TemporalPreservationGuard.checkLiteralOffset( value, getJavaType() );
				if ( precision == TemporalType.TIME ) {
					TemporalPreservationGuard.checkTimeLiteral( value, getJavaType(), dialect, options );
				}
				if ( precision == TemporalType.TIMESTAMP ) {
					final Instant instant = TemporalPreservationGuard.instantOf( value );
					if ( instant != null && TemporalPreservationGuard.isUtcByConvention( getJavaType(), options ) ) {
						// The columns of this database which hold an instant hold its date
						// and time at UTC, and know of no time zone. A literal with an offset
						// would be converted by the database to the time zone of its session,
						// so the literal is the date and time at UTC as well.
						temporalAccessor = LocalDateTime.ofInstant( instant, ZoneOffset.UTC );
					}
					else if ( value instanceof Instant ) {
						// An instant rendered as date and time fields in the JDBC time zone
						// is interpreted by the database in the time zone of its session,
						// and is ambiguous when clocks are set back. Render it with its
						// offset instead, which leaves nothing to interpret.
						temporalAccessor = instant.atOffset( ZoneOffset.UTC );
					}
					if ( temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
						TemporalPreservationGuard.checkOffsetIsRendered( value, getJavaType(), dialect, jdbcTimeZone );
					}
				}
			}
			dialect.getLiteralSupport().appendDateTimeLiteral( appender, temporalAccessor, precision, jdbcTimeZone );
		}
		else {
			dialect.getLiteralSupport().appendDateTimeLiteral(
					appender,
					unwrap( value, options ),
					precision,
					jdbcTimeZone
			);
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
