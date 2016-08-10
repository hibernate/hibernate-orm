/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.internal.descriptor.DateTimeUtils;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.CalendarTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimestampTypeDescriptor;
import org.hibernate.type.spi.VersionType;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link Calendar}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CalendarType
		extends AbstractSingleColumnStandardBasicType<Calendar>
		implements VersionType<Calendar>,JdbcLiteralFormatter<Calendar> {

	public static final CalendarType INSTANCE = new CalendarType();

	public CalendarType() {
		super( TimestampTypeDescriptor.INSTANCE, CalendarTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "calendar";
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
		return this;
	}

	@Override
	public String toJdbcLiteral(Calendar value, Dialect dialect) {
		return DateTimeUtils.formatAsJdbcLiteralTimestamp( value );
	}
}
