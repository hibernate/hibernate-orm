/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

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
import java.util.Locale;

import javax.persistence.TemporalType;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Java type descriptor for the LocalDateTime type.
 *
 * @author Steve Ebersole
 */
public class LocalDateJavaDescriptor
		extends AbstractBasicJavaDescriptor<LocalDate>
		implements TemporalJavaDescriptor<LocalDate> {
	/**
	 * Singleton access
	 */
	public static final LocalDateJavaDescriptor INSTANCE = new LocalDateJavaDescriptor();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH );

	@SuppressWarnings("unchecked")
	public LocalDateJavaDescriptor() {
		super( LocalDate.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.DATE );
	}

	@Override
	public String toString(LocalDate value) {
		return FORMATTER.format( value );
	}

	@Override
	public LocalDate fromString(String string) {
		return LocalDate.from( FORMATTER.parse( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(LocalDate value, Class<X> type, SharedSessionContractImplementor session) {
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
	public <X> LocalDate wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		if ( LocalDate.class.isInstance( value ) ) {
			return (LocalDate) value;
		}

		if ( Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			return LocalDateTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() ).toLocalDate();
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

	@Override
	public TemporalType getPrecision() {
		return TemporalType.DATE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalJavaDescriptor<X> resolveTypeForPrecision(TemporalType precision, TypeConfiguration scope) {
		if ( precision == TemporalType.DATE ) {
			return (TemporalJavaDescriptor<X>) this;
		}
		if ( precision == TemporalType.TIMESTAMP ) {
			return (TemporalJavaDescriptor<X>) scope.getJavaTypeDescriptorRegistry().getDescriptor( LocalDateTime.class );
		}
		if ( precision == TemporalType.TIME ) {
			throw new IllegalArgumentException( "Cannot treat LocalDateTime as javax.persistence.TemporalType#TIME" );
		}

		throw new IllegalArgumentException( "Unrecognized JPA TemporalType precision [" + precision + "]" );
	}
}
