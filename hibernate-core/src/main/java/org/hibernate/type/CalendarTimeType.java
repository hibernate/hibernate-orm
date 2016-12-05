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
import org.hibernate.type.spi.descriptor.java.CalendarTimeTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimeTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#TIME TIME} and {@link Calendar}.
 * <p/>
 * For example, a Calendar attribute annotated with {@link javax.persistence.Temporal} and specifying
 * {@link javax.persistence.TemporalType#TIME}
 *
 * @author Steve Ebersole
 */
public class CalendarTimeType extends BasicTypeImpl<Calendar>
		implements JdbcLiteralFormatter<Calendar> {
	public static final CalendarTimeType INSTANCE = new CalendarTimeType();

	public CalendarTimeType() {
		super( CalendarTimeTypeDescriptor.INSTANCE, TimeTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "calendar_time";
	}

	@Override
	public JdbcLiteralFormatter<Calendar> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Calendar value, Dialect dialect) {
		return DateTimeUtils.formatAsJdbcLiteralTime( value );
	}
}
