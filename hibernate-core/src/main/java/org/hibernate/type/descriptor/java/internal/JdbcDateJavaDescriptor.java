/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.sql.Types;
import java.time.LocalDate;
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
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link java.sql.Date} handling.
 *
 * @author Steve Ebersole
 */
public class JdbcDateJavaDescriptor extends AbstractBasicJavaDescriptor<Date> implements TemporalJavaDescriptor<Date> {
	public static final JdbcDateJavaDescriptor INSTANCE = new JdbcDateJavaDescriptor();

	/**
	 * Note that this is the pattern used exclusively to read/write these "Calendar date"
	 * values as Strings, not to format nor consume them as JDBC literals.  Uses
	 * java.time.format.DateTimeFormatter#ISO_LOCAL_DATE
	 */
	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	public static class DateMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final DateMutabilityPlan INSTANCE = new DateMutabilityPlan();
		@Override
		public Date deepCopyNotNull(Date value) {
			return java.sql.Date.class.isInstance( value )
					? new java.sql.Date( value.getTime() )
					: new Date( value.getTime() );
		}
	}

	public JdbcDateJavaDescriptor() {
		super( Date.class, DateMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(Date value) {
		if ( value instanceof java.sql.Date ) {
			return FORMATTER.format( ( (java.sql.Date) value ).toLocalDate() );
		}

		return FORMATTER.format( value.toInstant() );
	}

	@Override
	public Date fromString(String string) {
		return java.sql.Date.valueOf( LocalDate.parse( string, FORMATTER ) );
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}

		if ( one.getTime() == another.getTime() ) {
			return true;
		}

		Calendar calendar1 = Calendar.getInstance();
		Calendar calendar2 = Calendar.getInstance();
		calendar1.setTime( one );
		calendar2.setTime( another );

		return calendar1.get( Calendar.MONTH ) == calendar2.get( Calendar.MONTH )
				&& calendar1.get( Calendar.DAY_OF_MONTH ) == calendar2.get( Calendar.DAY_OF_MONTH )
				&& calendar1.get( Calendar.YEAR ) == calendar2.get( Calendar.YEAR );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.DATE );
	}

	@Override
	public int extractHashCode(Date value) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime( value );
		int hashCode = 1;
		hashCode = 31 * hashCode + calendar.get( Calendar.MONTH );
		hashCode = 31 * hashCode + calendar.get( Calendar.DAY_OF_MONTH );
		hashCode = 31 * hashCode + calendar.get( Calendar.YEAR );
		return hashCode;
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
			return new java.sql.Date( ( (Long) value ).longValue() );
		}

		if ( Calendar.class.isInstance( value ) ) {
			return new java.sql.Date( ( (Calendar) value ).getTimeInMillis() );
		}

		if ( Date.class.isInstance( value ) ) {
			return new java.sql.Date( ( (Date) value ).getTime() );
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
		final TemporalJavaDescriptor jdbcTimestampDescriptor = (TemporalJavaDescriptor) scope.getJavaTypeDescriptorRegistry()
				.getDescriptor( java.sql.Timestamp.class );
		return jdbcTimestampDescriptor.resolveTypeForPrecision( precision, scope );
	}
}
