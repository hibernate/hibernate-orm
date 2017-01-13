/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Calendar;
import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TemporalType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.java.internal.CalendarJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimestampTypeDescriptor;
import org.hibernate.type.spi.VersionSupport;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link Calendar}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CalendarType
		extends TemporalTypeImpl<Calendar>
		implements VersionSupport<Calendar> {

	public static final CalendarType INSTANCE = new CalendarType();

	public CalendarType() {
		super( CalendarJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "calendar";
	}

	@Override
	public VersionSupport<Calendar> getVersionSupport() {
		return this;
	}

	@Override
	public Calendar next(Calendar current, SharedSessionContractImplementor session) {
		return seed( session );
	}

	@Override
	public Calendar seed(SharedSessionContractImplementor session) {
		return Calendar.getInstance();
	}

	@Override
	public Comparator<Calendar> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}

	@Override
	public JdbcLiteralFormatter<Calendar> getJdbcLiteralFormatter() {
		return TimestampTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( CalendarJavaDescriptor.INSTANCE );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalType<X> resolveTypeForPrecision(javax.persistence.TemporalType precision, TypeConfiguration typeConfiguration) {
		switch ( precision ) {
			case DATE: {
				return (TemporalType<X>) CalendarDateType.INSTANCE;
			}
			case TIME: {
				return (TemporalType<X>) CalendarTimeType.INSTANCE;
			}
			default: {
				return (TemporalType<X>) this;
			}
		}
	}
}
