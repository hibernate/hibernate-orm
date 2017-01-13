/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Date;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.java.internal.JdbcDateJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.DateSqlDescriptor;

/**
 * A type that maps between {@link java.sql.Types#DATE DATE} and {@link java.sql.Date}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DateType extends TemporalTypeImpl<Date> {

	public static final DateType INSTANCE = new DateType();

	protected DateType() {
		super( JdbcDateJavaDescriptor.INSTANCE, DateSqlDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "date";
	}

	@Override
	public JdbcLiteralFormatter<Date> getJdbcLiteralFormatter() {
		return DateSqlDescriptor.INSTANCE.getJdbcLiteralFormatter( JdbcDateJavaDescriptor.INSTANCE );
	}
}
