/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Java type descriptor for the LocalDateTime type.
 *
 * @author Steve Ebersole
 */
public class LocalDateJavaTypeDescriptor extends AbstractTemporalJavaTypeDescriptor<LocalDate> {
	/**
	 * Singleton access
	 */
	public static final LocalDateJavaTypeDescriptor INSTANCE = new LocalDateJavaTypeDescriptor();

	@SuppressWarnings("unchecked")
	public LocalDateJavaTypeDescriptor() {
		super( LocalDate.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.DATE;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		return context.getTypeConfiguration().getJdbcTypeDescriptorRegistry().getDescriptor( Types.DATE );
	}

	@Override
	protected <X> TemporalJavaTypeDescriptor<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaTypeDescriptor<X>) this;
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

		if ( LocalDate.class.isInstance( value ) ) {
			return (LocalDate) value;
		}

		if ( Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			/*
			 * Workaround for HHH-13266 (JDK-8061577).
			 * We used to do LocalDateTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() ).toLocalDate(),
			 * but on top of being more complex than the line below, it won't always work.
			 * ts.toInstant() assumes the number of milliseconds since the epoch
			 * means the same thing in Timestamp and Instant, but it doesn't, in particular before 1900.
			 */
			return ts.toLocalDateTime().toLocalDate();
		}

		if ( Long.class.isInstance( value ) ) {
			final Instant instant = Instant.ofEpochMilli( (Long) value );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() ).toLocalDate();
		}

		if ( Calendar.class.isInstance( value ) ) {
			final Calendar calendar = (Calendar) value;
			return LocalDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() ).toLocalDate();
		}

		if ( Date.class.isInstance( value ) ) {
			if ( java.sql.Date.class.isInstance( value ) ) {
				return ((java.sql.Date) value).toLocalDate();
			}
			else {
				return Instant.ofEpochMilli( ((Date) value).getTime() ).atZone( ZoneId.systemDefault() ).toLocalDate();
			}
		}

		throw unknownWrap( value.getClass() );
	}

}
