/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Time;
import java.util.Date;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.internal.descriptor.DateTimeUtils;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.JdbcTimeTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIME TIME} and {@link Time}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TimeType extends BasicTypeImpl<Date> implements JdbcLiteralFormatter<Date> {

	public static final TimeType INSTANCE = new TimeType();

	public TimeType() {
		super( JdbcTimeTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.TimeTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "time";
	}

	@Override
	public JdbcLiteralFormatter<Date> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Date value, Dialect dialect) {
		return DateTimeUtils.formatAsJdbcLiteralTime( value );
	}
}
