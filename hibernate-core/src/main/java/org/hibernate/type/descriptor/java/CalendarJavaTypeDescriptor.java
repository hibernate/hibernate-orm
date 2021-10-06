/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.CalendarComparator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.TimeJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Calendar} handling.
 *
 * @author Steve Ebersole
 */
public class CalendarJavaTypeDescriptor extends AbstractTemporalJavaTypeDescriptor<Calendar> implements VersionJavaTypeDescriptor<Calendar> {
	public static final CalendarJavaTypeDescriptor INSTANCE = new CalendarJavaTypeDescriptor();

	public static class CalendarMutabilityPlan extends MutableMutabilityPlan<Calendar> {
		public static final CalendarMutabilityPlan INSTANCE = new CalendarMutabilityPlan();

		public Calendar deepCopyNotNull(Calendar value) {
			return (Calendar) value.clone();
		}
	}

	protected CalendarJavaTypeDescriptor() {
		super( Calendar.class, CalendarMutabilityPlan.INSTANCE, CalendarComparator.INSTANCE );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	public JdbcTypeDescriptor getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		return TimestampJdbcTypeDescriptor.INSTANCE;
	}

	@Override
	protected <X> TemporalJavaTypeDescriptor<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaTypeDescriptor<X>) this;
	}

	@Override
	protected <X> TemporalJavaTypeDescriptor<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaTypeDescriptor<X>) CalendarDateJavaTypeDescriptor.INSTANCE;
	}

	@Override
	protected <X> TemporalJavaTypeDescriptor<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaTypeDescriptor<X>) CalendarTimeJavaTypeDescriptor.INSTANCE;
	}

	public String toString(Calendar value) {
		return DateJavaTypeDescriptor.INSTANCE.toString( value.getTime() );
	}

	public Calendar fromString(CharSequence string) {
		Calendar result = new GregorianCalendar();
		result.setTime( DateJavaTypeDescriptor.INSTANCE.fromString( string.toString() ) );
		return result;
	}

	@Override
	public boolean areEqual(Calendar one, Calendar another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}

		return one.get(Calendar.MILLISECOND) == another.get(Calendar.MILLISECOND)
			&& one.get(Calendar.SECOND) == another.get(Calendar.SECOND)
			&& one.get(Calendar.MINUTE) == another.get(Calendar.MINUTE)
			&& one.get(Calendar.HOUR_OF_DAY) == another.get(Calendar.HOUR_OF_DAY)
			&& one.get(Calendar.DAY_OF_MONTH) == another.get(Calendar.DAY_OF_MONTH)
			&& one.get(Calendar.MONTH) == another.get(Calendar.MONTH)
			&& one.get(Calendar.YEAR) == another.get(Calendar.YEAR);
	}

	@Override
	public int extractHashCode(Calendar value) {
		int hashCode = 1;
		hashCode = 31 * hashCode + value.get(Calendar.MILLISECOND);
		hashCode = 31 * hashCode + value.get(Calendar.SECOND);
		hashCode = 31 * hashCode + value.get(Calendar.MINUTE);
		hashCode = 31 * hashCode + value.get(Calendar.HOUR_OF_DAY);
		hashCode = 31 * hashCode + value.get(Calendar.DAY_OF_MONTH);
		hashCode = 31 * hashCode + value.get(Calendar.MONTH);
		hashCode = 31 * hashCode + value.get(Calendar.YEAR);
		return hashCode;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Calendar value, Class<X> type, WrapperOptions options) {
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
		if ( java.util.Date.class.isAssignableFrom( type ) ) {
			return (X) new  java.util.Date( value.getTimeInMillis() );
		}
		throw unknownUnwrap( type );
	}

	public <X> Calendar wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Calendar.class.isInstance( value ) ) {
			return (Calendar) value;
		}

		if ( ! java.util.Date.class.isInstance( value ) ) {
			throw unknownWrap( value.getClass() );
		}

		Calendar cal = new GregorianCalendar();
		cal.setTime( (java.util.Date) value );
		return cal;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect) {
		return dialect.getDefaultTimestampPrecision();
	}

	@Override
	public Calendar next(Calendar current, SharedSessionContractImplementor session) {
		return seed( session );
	}

	@Override
	public Calendar seed(SharedSessionContractImplementor session) {
		return Calendar.getInstance();
	}
}
