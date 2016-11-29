/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Calendar;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.internal.descriptor.DateTimeUtils;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.CalendarDateTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.DateTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#DATE DATE} and {@link Calendar}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CalendarDateType
		extends BasicTypeImpl<Calendar>
		implements JdbcLiteralFormatter<Calendar> {
	public static final CalendarDateType INSTANCE = new CalendarDateType();

	public CalendarDateType() {
		super( CalendarDateTypeDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "calendar_date";
	}

	@Override
	public JdbcLiteralFormatter<Calendar> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Calendar value, Dialect dialect) {
		return DateTimeUtils.formatAsJdbcLiteralDate( value );
	}
}
