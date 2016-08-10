/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Date;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.JdbcDateTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#DATE DATE} and {@link java.sql.Date}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DateType
		extends AbstractSingleColumnStandardBasicType<Date> implements JdbcLiteralFormatter<Date> {

	public static final DateType INSTANCE = new DateType();

	protected DateType() {
		super( org.hibernate.type.spi.descriptor.sql.DateTypeDescriptor.INSTANCE, JdbcDateTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "date";
	}

	@Override
	public JdbcLiteralFormatter<Date> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Date value, Dialect dialect) {
		return StringType.INSTANCE.toJdbcLiteral( toString( value ), dialect );
	}
}
