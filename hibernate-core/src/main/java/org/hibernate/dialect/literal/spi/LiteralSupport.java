/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.literal.spi;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import jakarta.persistence.TemporalType;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Renders database-specific SQL literal expressions.
///
/// Append directly to the supplied appender and do not retain it or the literal
/// value. Override the appender operation rather than a String-valued
/// convenience so every consumer observes the same database syntax.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getLiteralSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface LiteralSupport {
	/// Append a SQL string literal.
	default void appendLiteral(SqlAppender appender, String literal) {
		appender.appendSingleQuoteEscapedString( literal );
	}

	/// Append a SQL binary literal.
	default void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "X'" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
	}

	/// Append a SQL Boolean literal expression.
	default void appendBooleanValueString(SqlAppender appender, boolean value) {
		appender.appendSql( value ? '1' : '0' );
	}

	/// Render a complete SQL Boolean literal through the canonical appender hook.
	default String toBooleanValueString(boolean value) {
		final var result = new StringBuilder();
		appendBooleanValueString( new StringBuilderSqlAppender( result ), value );
		return result.toString();
	}

	/// Append a SQL interval literal for a duration.
	default void appendIntervalLiteral(SqlAppender appender, Duration literal) {
		final int nano = literal.getNano();
		final int secondsPart = literal.toSecondsPart();
		final int minutesPart = literal.toMinutesPart();
		final int hoursPart = literal.toHoursPart();
		final long daysPart = literal.toDaysPart();
		enum Unit { day, hour, minute }
		final Unit unit;
		if ( daysPart != 0 ) {
			unit = hoursPart == 0 && minutesPart == 0 && secondsPart == 0 && nano == 0
					? Unit.day
					: null;
		}
		else if ( hoursPart != 0 ) {
			unit = minutesPart == 0 && secondsPart == 0 && nano == 0
					? Unit.hour
					: null;
		}
		else if ( minutesPart != 0 ) {
			unit = secondsPart == 0 && nano == 0
					? Unit.minute
					: null;
		}
		else {
			unit = null;
		}
		appender.appendSql( "interval '" );
		if ( unit != null ) {
			appender.appendSql( switch ( unit ) {
				case day -> daysPart;
				case hour -> hoursPart;
				case minute -> minutesPart;
			} );
			appender.appendSql( "' " );
			appender.appendSql( unit.toString() );
		}
		else {
			appender.appendSql( literal.getSeconds() );
			if ( nano > 0 ) {
				appender.appendSql( '.' );
				appender.appendSql( nano );
			}
			appender.appendSql( "' second" );
		}
	}

	/// Append a SQL interval literal for a supported temporal amount.
	default void appendIntervalLiteral(SqlAppender appender, TemporalAmount literal) {
		if ( literal instanceof Duration duration ) {
			appendIntervalLiteral( appender, duration );
		}
		else if ( literal instanceof Period period ) {
			final int years = period.getYears();
			final int months = period.getMonths();
			final int days = period.getDays();
			final boolean parenthesis = years != 0 && months != 0
					|| years != 0 && days != 0
					|| months != 0 && days != 0;
			if ( parenthesis ) {
				appender.appendSql( '(' );
			}
			boolean first = true;
			for ( var unit : literal.getUnits() ) {
				final long value = literal.get( unit );
				if ( value != 0 ) {
					if ( first ) {
						first = false;
					}
					else {
						appender.appendSql( "+" );
					}
					appender.appendSql( "interval '" );
					appender.appendSql( value );
					appender.appendSql( "' " );
					if ( unit == ChronoUnit.YEARS ) {
						appender.appendSql( "year" );
					}
					else if ( unit == ChronoUnit.MONTHS ) {
						appender.appendSql( "month" );
					}
					else {
						assert unit == ChronoUnit.DAYS;
						appender.appendSql( "day" );
					}
				}
			}
			if ( parenthesis ) {
				appender.appendSql( ')' );
			}
		}
		else {
			throw new IllegalArgumentException( "Unsupported temporal amount type: " + literal );
		}
	}

	/// Append a SQL UUID literal expression.
	default void appendUUIDLiteral(SqlAppender appender, UUID literal) {
		appender.appendSql( "cast('" );
		appender.appendSql( literal.toString() );
		appender.appendSql( "' as uuid)" );
	}

	/// Append a SQL array literal.
	///
	/// Append immediately and do not retain the appender, values, formatter, or
	/// wrapper options.
	@SPI({ USE, IMPLEMENT })
	void appendArrayLiteral(
			SqlAppender appender,
			Object[] literal,
			JdbcLiteralFormatter<Object> elementFormatter,
			WrapperOptions wrapperOptions);

	/// Append a datetime literal for a `java.time` value.
	@SPI({ USE, IMPLEMENT })
	void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone);

	/// Append a datetime literal for a legacy [Date] value.
	@SPI({ USE, IMPLEMENT })
	void appendDateTimeLiteral(
			SqlAppender appender,
			Date date,
			TemporalType precision,
			TimeZone jdbcTimeZone);

	/// Append a datetime literal for a [Calendar] value.
	@SPI({ USE, IMPLEMENT })
	void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			TemporalType precision,
			TimeZone jdbcTimeZone);
}
