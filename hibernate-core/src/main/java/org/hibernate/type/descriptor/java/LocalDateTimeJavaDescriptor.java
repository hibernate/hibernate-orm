/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.TimestampTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Java type descriptor for the LocalDateTime type.
 *
 * @author Steve Ebersole
 */
public class LocalDateTimeJavaDescriptor extends AbstractTemporalTypeDescriptor<LocalDateTime> {
	/**
	 * Singleton access
	 */
	public static final LocalDateTimeJavaDescriptor INSTANCE = new LocalDateTimeJavaDescriptor();

	@SuppressWarnings("unchecked")
	public LocalDateTimeJavaDescriptor() {
		super( LocalDateTime.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	public JdbcTypeDescriptor getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		return TimestampTypeDescriptor.INSTANCE;
	}

	@Override
	protected <X> TemporalJavaTypeDescriptor<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaTypeDescriptor<X>) this;
	}

	@Override
	public String toString(LocalDateTime value) {
		return DateTimeFormatter.ISO_DATE_TIME.format( value );
	}

	@Override
	public LocalDateTime fromString(CharSequence string) {
		return LocalDateTime.from( DateTimeFormatter.ISO_DATE_TIME.parse( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(LocalDateTime value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalDateTime.class.isAssignableFrom( type ) ) {
			return (X) value;
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			/*
			 * Workaround for HHH-13266 (JDK-8061577).
			 * We used to do Timestamp.from( value.atZone( ZoneId.systemDefault() ).toInstant() ),
			 * but on top of being more complex than the line below, it won't always work.
			 * Timestamp.from() assumes the number of milliseconds since the epoch
			 * means the same thing in Timestamp and Instant, but it doesn't, in particular before 1900.
			 */
			return (X) Timestamp.valueOf( value );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			Instant instant = value.atZone( ZoneId.systemDefault() ).toInstant();
			return (X) java.sql.Date.from( instant );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			Instant instant = value.atZone( ZoneId.systemDefault() ).toInstant();
			return (X) java.sql.Time.from( instant );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			Instant instant = value.atZone( ZoneId.systemDefault() ).toInstant();
			return (X) Date.from( instant );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( value.atZone( ZoneId.systemDefault() ) );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			Instant instant = value.atZone( ZoneId.systemDefault() ).toInstant();
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> LocalDateTime wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalDateTime.class.isInstance( value ) ) {
			return (LocalDateTime) value;
		}

		if ( Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			/*
			 * Workaround for HHH-13266 (JDK-8061577).
			 * We used to do LocalDateTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() ),
			 * but on top of being more complex than the line below, it won't always work.
			 * ts.toInstant() assumes the number of milliseconds since the epoch
			 * means the same thing in Timestamp and Instant, but it doesn't, in particular before 1900.
			 */
			return ts.toLocalDateTime();
		}

		if ( Long.class.isInstance( value ) ) {
			final Instant instant = Instant.ofEpochMilli( (Long) value );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() );
		}

		if ( Calendar.class.isInstance( value ) ) {
			final Calendar calendar = (Calendar) value;
			return LocalDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() );
		}

		if ( Date.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			final Instant instant = Instant.ofEpochMilli( ts.getTime() );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect) {
		return dialect.getDefaultTimestampPrecision();
	}
}
