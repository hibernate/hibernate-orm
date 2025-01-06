/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Java type descriptor for the {@link LocalDate} type.
 *
 * @author Steve Ebersole
 */
public class LocalDateJavaType extends AbstractTemporalJavaType<LocalDate> {
	/**
	 * Singleton access
	 */
	public static final LocalDateJavaType INSTANCE = new LocalDateJavaType();

	public LocalDateJavaType() {
		super( LocalDate.class, ImmutableMutabilityPlan.instance() );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.DATE;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		if ( context.isPreferJavaTimeJdbcTypesEnabled() ) {
			return context.getJdbcType( SqlTypes.LOCAL_DATE );
		}
		return context.getJdbcType( Types.DATE );
	}

	@Override
	protected <X> TemporalJavaType<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaType<X>) this;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(LocalDate value) {
		return DateTimeFormatter.ISO_LOCAL_DATE.format( value );
	}

	@Override
	public LocalDate fromString(CharSequence string) {
		return LocalDate.from( DateTimeFormatter.ISO_LOCAL_DATE.parse( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(LocalDate value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalDate.class.isAssignableFrom( type ) ) {
			return (X) value;
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Date.valueOf( value );
		}

		final LocalDateTime localDateTime = value.atStartOfDay();

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			/*
			 * Workaround for HHH-13266 (JDK-8061577).
			 * We could have done Timestamp.from( localDateTime.atZone( ZoneId.systemDefault() ).toInstant() ),
			 * but on top of being more complex than the line below, it won't always work.
			 * Timestamp.from() assumes the number of milliseconds since the epoch
			 * means the same thing in Timestamp and Instant, but it doesn't, in particular before 1900.
			 */
			return (X) Timestamp.valueOf( localDateTime );
		}

		final ZonedDateTime zonedDateTime = localDateTime.atZone( ZoneId.systemDefault() );

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( zonedDateTime );
		}

		final Instant instant = zonedDateTime.toInstant();

		if ( Date.class.equals( type ) ) {
			return (X) Date.from( instant );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> LocalDate wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if (value instanceof LocalDate localDate) {
			return localDate;
		}

		if (value instanceof Timestamp timestamp) {
			/*
			 * Workaround for HHH-13266 (JDK-8061577).
			 * We used to do LocalDateTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() ).toLocalDate(),
			 * but on top of being more complex than the line below, it won't always work.
			 * ts.toInstant() assumes the number of milliseconds since the epoch
			 * means the same thing in Timestamp and Instant, but it doesn't, in particular before 1900.
			 */
			return timestamp.toLocalDateTime().toLocalDate();
		}

		if (value instanceof Long longValue) {
			final Instant instant = Instant.ofEpochMilli( longValue );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() ).toLocalDate();
		}

		if (value instanceof Calendar calendar) {
			return LocalDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() ).toLocalDate();
		}

		if (value instanceof Date date) {
			if (value instanceof java.sql.Date sqlDate) {
				return sqlDate.toLocalDate();
			}
			else {
				return Instant.ofEpochMilli( date.getTime() ).atZone( ZoneId.systemDefault() ).toLocalDate();
			}
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		return switch ( javaType.getTypeName() ) {
			case "java.sql.Date" -> true;
			default -> false;
		};
	}

	@Override
	public LocalDate[] newArray(int numberOfElements) {
		return new LocalDate[numberOfElements];
	}

	@Override
	public Class<LocalDate[]> getArrayClass() {
		return LocalDate[].class;
	}

}
