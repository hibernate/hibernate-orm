/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
public class OffsetTimeJavaDescriptor
		extends AbstractBasicJavaDescriptor<OffsetTime>
		implements TemporalJavaDescriptor<OffsetTime> {
	/**
	 * Singleton access
	 */
	public static final OffsetTimeJavaDescriptor INSTANCE = new OffsetTimeJavaDescriptor();

	/**
	 * Note that this is the pattern used exclusively to read/write these "Calendar date"
	 * values as Strings, not to format nor consume them as JDBC literals.  Uses
	 * java.time.format.DateTimeFormatter#ISO_OFFSET_TIME
	 */
	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_TIME;

	@SuppressWarnings("unchecked")
	public OffsetTimeJavaDescriptor() {
		super( OffsetTime.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.TIME );
	}

	@Override
	public String toString(OffsetTime value) {
		return FORMATTER.format( value );
	}

	@Override
	public OffsetTime fromString(String string) {
		return OffsetTime.parse( string, FORMATTER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(OffsetTime offsetTime, Class<X> type, SharedSessionContractImplementor session) {
		if ( offsetTime == null ) {
			return null;
		}

		if ( OffsetTime.class.isAssignableFrom( type ) ) {
			return (X) offsetTime;
		}

		if ( Time.class.isAssignableFrom( type ) ) {
			return (X) Time.valueOf( offsetTime.toLocalTime() );
		}

		final ZonedDateTime zonedDateTime = offsetTime.atDate( LocalDate.of( 1970, 1, 1 ) ).toZonedDateTime();

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			return (X) Timestamp.valueOf( zonedDateTime.toLocalDateTime() );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( zonedDateTime );
		}

		final Instant instant = zonedDateTime.toInstant();

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			return (X) Date.from( instant );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> OffsetTime wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		if ( OffsetTime.class.isInstance( value ) ) {
			return (OffsetTime) value;
		}

		if ( Time.class.isInstance( value ) ) {
			return ( (Time) value ).toLocalTime().atOffset( OffsetDateTime.now().getOffset() );
		}

		if ( Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			return OffsetTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() );
		}

		if ( Date.class.isInstance( value ) ) {
			final Date date = (Date) value;
			return OffsetTime.ofInstant( date.toInstant(), ZoneId.systemDefault() );
		}

		if ( Long.class.isInstance( value ) ) {
			return OffsetTime.ofInstant( Instant.ofEpochMilli( (Long) value ), ZoneId.systemDefault() );
		}

		if ( Calendar.class.isInstance( value ) ) {
			final Calendar calendar = (Calendar) value;
			return OffsetTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIME;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalJavaDescriptor<X> resolveTypeForPrecision(TemporalType precision, TypeConfiguration scope) {
		if ( precision == TemporalType.TIME ) {
			return (TemporalJavaDescriptor<X>) this;
		}
		if ( precision == TemporalType.TIMESTAMP ) {
			return (TemporalJavaDescriptor<X>) scope.getJavaTypeDescriptorRegistry().getDescriptor( LocalDateTime.class );
		}
		if ( precision == TemporalType.DATE ) {
			throw new IllegalArgumentException( "Cannot treat OffsetTime as javax.persistence.TemporalType#DATE" );
		}

		throw new IllegalArgumentException( "Unrecognized JPA TemporalType precision [" + precision + "]" );
	}
}
