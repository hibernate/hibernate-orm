/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Time;
import java.util.Date;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TemporalType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.java.internal.JdbcTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.TimeSqlDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIME TIME} and {@link Time}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TimeType extends TemporalTypeImpl<Date> {

	public static final TimeType INSTANCE = new TimeType();

	public TimeType() {
		super( JdbcTimeJavaDescriptor.INSTANCE, TimeSqlDescriptor.INSTANCE );
	}

	public String getName() {
		return "time";
	}

	@Override
	public JdbcLiteralFormatter<Date> getJdbcLiteralFormatter() {
		return TimeSqlDescriptor.INSTANCE.getJdbcLiteralFormatter( JdbcTimeJavaDescriptor.INSTANCE );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalType<X> resolveTypeForPrecision(
			javax.persistence.TemporalType precision, TypeConfiguration typeConfiguration) {
		switch ( precision ) {
			case DATE: {
				return (TemporalType<X>) DateType.INSTANCE;
			}
			case TIMESTAMP: {
				return (TemporalType<X>) TimestampType.INSTANCE;
			}
			default: {
				return (TemporalType<X>) this;
			}
		}
	}

}
