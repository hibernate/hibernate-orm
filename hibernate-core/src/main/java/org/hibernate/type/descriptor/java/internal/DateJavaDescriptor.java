/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.sql.Types;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.persistence.TemporalType;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.MutableMutabilityPlan;
import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.TimestampVersionSupport;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;

/**
 * Descriptor for {@link Date} handling.
 *
 * @author Steve Ebersole
 */
public class DateJavaDescriptor
		extends AbstractBasicJavaDescriptor<Date>
		implements TemporalJavaDescriptor<Date> {
	public static final DateJavaDescriptor INSTANCE = new DateJavaDescriptor();

	/**
	 * Note that this is the pattern used exclusively to read/write these "Calendar date"
	 * values as Strings, not to format nor consume them as JDBC literals.  Uses
	 * java.time.format.DateTimeFormatter#ISO_LOCAL_DATE_TIME
	 */
	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalJavaDescriptor<X> resolveTypeForPrecision(TemporalType precision, TypeConfiguration scope) {
		final TemporalJavaDescriptor jdbcTimestampDescriptor = (TemporalJavaDescriptor) scope.getJavaTypeDescriptorRegistry()
				.getDescriptor( java.sql.Timestamp.class );
		return jdbcTimestampDescriptor.resolveTypeForPrecision( precision, scope );
	}

	public static class DateMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final DateMutabilityPlan INSTANCE = new DateMutabilityPlan();
		@Override
		public Date deepCopyNotNull(Date value) {
			return new Date( value.getTime() );
		}
	}

	public DateJavaDescriptor() {
		super( Date.class, DateMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(Date value) {
		return FORMATTER.format( value.toInstant() );
	}

	@Override
	public Date fromString(String string) {
		return Date.from( ZonedDateTime.parse( string, FORMATTER ).toInstant() );
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another) {
			return true;
		}
		return !( one == null || another == null ) && one.getTime() == another.getTime();

	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.DATE );
	}

	@Override
	public int extractHashCode(Date value) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime( value );
		return CalendarJavaDescriptor.INSTANCE.extractHashCode( calendar );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Date value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			final java.sql.Date rtn = java.sql.Date.class.isInstance( value )
					? ( java.sql.Date ) value
					: new java.sql.Date( value.getTime() );
			return (X) rtn;
		}
		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			final java.sql.Time rtn = java.sql.Time.class.isInstance( value )
					? ( java.sql.Time ) value
					: new java.sql.Time( value.getTime() );
			return (X) rtn;
		}
		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			final java.sql.Timestamp rtn = java.sql.Timestamp.class.isInstance( value )
					? ( java.sql.Timestamp ) value
					: new java.sql.Timestamp( value.getTime() );
			return (X) rtn;
		}
		if ( Date.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Calendar.class.isAssignableFrom( type ) ) {
			final GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis( value.getTime() );
			return (X) cal;
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( value.getTime() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Date wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Date.class.isInstance( value ) ) {
			return (Date) value;
		}

		if ( Long.class.isInstance( value ) ) {
			return new Date( (Long) value );
		}

		if ( Calendar.class.isInstance( value ) ) {
			return new Date( ( (Calendar) value ).getTimeInMillis() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public VersionSupport<Date> getVersionSupport() {
		return TimestampVersionSupport.INSTANCE;
	}
}
