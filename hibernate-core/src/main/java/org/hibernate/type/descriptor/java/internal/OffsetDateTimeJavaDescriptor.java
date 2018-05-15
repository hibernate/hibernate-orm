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
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
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
import org.hibernate.metamodel.model.domain.spi.OffsetDateTimeVersionSupport;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;

import org.jboss.logging.Logger;

/**
 * Java type descriptor for the LocalDateTime type.
 *
 * @author Steve Ebersole
 */
public class OffsetDateTimeJavaDescriptor
		extends AbstractBasicJavaDescriptor<OffsetDateTime>
		implements TemporalJavaDescriptor<OffsetDateTime> {
	private static final Logger log = Logger.getLogger( OffsetDateTimeJavaDescriptor.class );

	/**
	 * Singleton access
	 */
	public static final OffsetDateTimeJavaDescriptor INSTANCE = new OffsetDateTimeJavaDescriptor();

	@SuppressWarnings("unchecked")
	public OffsetDateTimeJavaDescriptor() {
		super( OffsetDateTime.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.TIMESTAMP );
	}

	@Override
	public String toString(OffsetDateTime value) {
		return value.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME );
	}

	@Override
	public OffsetDateTime fromString(String string) {
		return OffsetDateTime.parse( string, DateTimeFormatter.ISO_OFFSET_DATE_TIME );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(OffsetDateTime offsetDateTime, Class<X> type, SharedSessionContractImplementor session) {
		if ( offsetDateTime == null ) {
			return null;
		}

		if ( OffsetDateTime.class.isAssignableFrom( type ) ) {
			return (X) offsetDateTime;
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( offsetDateTime.toZonedDateTime() );
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			return (X) Timestamp.from( offsetDateTime.toInstant() );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Date.from( offsetDateTime.toInstant() );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Time.from( offsetDateTime.toInstant() );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			return (X) Date.from( offsetDateTime.toInstant() );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( offsetDateTime.toInstant().toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> OffsetDateTime wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		if ( OffsetDateTime.class.isInstance( value ) ) {
			return (OffsetDateTime) value;
		}

		if ( Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			return OffsetDateTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() );
		}

		if ( Date.class.isInstance( value ) ) {
			final Date date = (Date) value;
			return OffsetDateTime.ofInstant( date.toInstant(), ZoneId.systemDefault() );
		}

		if ( Long.class.isInstance( value ) ) {
			return OffsetDateTime.ofInstant( Instant.ofEpochMilli( (Long) value ), ZoneId.systemDefault() );
		}

		if ( Calendar.class.isInstance( value ) ) {
			final Calendar calendar = (Calendar) value;
			return OffsetDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalJavaDescriptor<X> resolveTypeForPrecision(TemporalType precision, TypeConfiguration scope) {
		if ( precision == TemporalType.TIMESTAMP ) {
			return (TemporalJavaDescriptor<X>) this;
		}
		if ( precision == TemporalType.DATE ) {
			log.debugf( "No JPA TemporalType#TIME Java representation for LocalDateTime, using LocalDateTime" );
			return (TemporalJavaDescriptor<X>) this;
		}
		if ( precision == TemporalType.TIME ) {
			return (TemporalJavaDescriptor<X>) scope.getJavaTypeDescriptorRegistry().getDescriptor( OffsetTime.class );
		}

		throw new IllegalArgumentException( "Unrecognized JPA TemporalType precision [" + precision + "]" );
	}

	@Override
	public VersionSupport<OffsetDateTime> getVersionSupport() {
		return OffsetDateTimeVersionSupport.INSTANCE;
	}
}
