/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.persistence.TemporalType;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.CalendarComparator;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.TemporalSqlDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Calendar} handling, but just for the time portion.
 *
 * @author Steve Ebersole
 */
public class CalendarTimeJavaDescriptor
		extends AbstractBasicJavaDescriptor<Calendar>
		implements TemporalJavaDescriptor<Calendar> {

	public static final CalendarTimeJavaDescriptor INSTANCE = new CalendarTimeJavaDescriptor();

	/**
	 * Note that this is the pattern used exclusively to read/write these "Calendar date"
	 * values as Strings, not to format nor consume them as JDBC literals.  Uses
	 * java.time.format.DateTimeFormatter#ISO_OFFSET_TIME
	 */
	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_TIME;

	protected CalendarTimeJavaDescriptor() {
		super( Calendar.class, CalendarJavaDescriptor.CalendarMutabilityPlan.INSTANCE );
	}

	public String toString(Calendar calendar) {
		return calendar.toInstant().atZone( calendar.getTimeZone().toZoneId() ).format( FORMATTER );
	}

	public Calendar fromString(String string) {
		final OffsetTime parsedOffsetTime = OffsetTime.parse( string, FORMATTER );
		return GregorianCalendar.from( parsedOffsetTime.atDate( LocalDate.MIN ).atZoneSameInstant( parsedOffsetTime.getOffset() ) );
	}

	@Override
	public boolean areEqual(Calendar one, Calendar another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}

		return one.get(Calendar.DAY_OF_MONTH) == another.get(Calendar.DAY_OF_MONTH)
			&& one.get(Calendar.MONTH) == another.get(Calendar.MONTH)
			&& one.get(Calendar.YEAR) == another.get(Calendar.YEAR);
	}

	@Override
	public int extractHashCode(Calendar value) {
		int hashCode = 1;
		hashCode = 31 * hashCode + value.get(Calendar.DAY_OF_MONTH);
		hashCode = 31 * hashCode + value.get(Calendar.MONTH);
		hashCode = 31 * hashCode + value.get(Calendar.YEAR);
		return hashCode;
	}

	@Override
	public TemporalSqlDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return (TemporalSqlDescriptor) context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.TIME );
	}

	@Override
	public Comparator<Calendar> getComparator() {
		return CalendarComparator.INSTANCE;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Calendar value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) new java.sql.Date( value.getTimeInMillis() );
		}
		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return (X) new java.sql.Time( value.getTimeInMillis() );
		}
		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			return (X) new java.sql.Timestamp( value.getTimeInMillis() );
		}
		if ( Date.class.isAssignableFrom( type ) ) {
			return (X) new  Date( value.getTimeInMillis() );
		}
		throw unknownUnwrap( type );
	}

	public <X> Calendar wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Calendar.class.isInstance( value ) ) {
			return (Calendar) value;
		}

		if ( ! Date.class.isInstance( value ) ) {
			throw unknownWrap( value.getClass() );
		}

		Calendar cal = new GregorianCalendar();
		if ( Environment.jvmHasTimestampBug() ) {
			final long milliseconds = ( (Date) value ).getTime();
			final long nanoseconds = java.sql.Timestamp.class.isInstance( value )
					? ( (java.sql.Timestamp) value ).getNanos()
					: 0;
			cal.setTime( new Date( milliseconds + nanoseconds / 1000000 ) );
		}
		else {
			cal.setTime( (Date) value );
		}
		return cal;
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIME;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalJavaDescriptor<X> resolveTypeForPrecision(
			TemporalType precision,
			TypeConfiguration scope) {
		if ( precision == TemporalType.TIME ) {
			return (TemporalJavaDescriptor<X>) this;
		}

		final TemporalJavaDescriptor baseCalendarDescriptor = (TemporalJavaDescriptor) scope.getJavaTypeDescriptorRegistry()
				.getDescriptor( Calendar.class );
		return baseCalendarDescriptor.resolveTypeForPrecision( precision, scope );
	}
}
