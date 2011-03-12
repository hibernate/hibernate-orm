/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type;

import java.util.Date;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.JdbcDateTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#DATE DATE} and {@link java.sql.Date}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DateType
		extends AbstractSingleColumnStandardBasicType<Date>
		implements IdentifierType<Date>, LiteralType<Date> {

	public static final DateType INSTANCE = new DateType();

	public DateType() {
		super( org.hibernate.type.descriptor.sql.DateTypeDescriptor.INSTANCE, JdbcDateTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "date";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {
				getName(),
				java.sql.Date.class.getName()
		};
	}

//	@Override
//	protected boolean registerUnderJavaType() {
//		return true;
//	}

	public String objectToSQLString(Date value, Dialect dialect) throws Exception {
		final java.sql.Date jdbcDate = java.sql.Date.class.isInstance( value )
				? ( java.sql.Date ) value
				: new java.sql.Date( value.getTime() );
		// TODO : use JDBC date literal escape syntax? -> {d 'date-string'} in yyyy-mm-dd format
		return StringType.INSTANCE.objectToSQLString( jdbcDate.toString(), dialect );
	}

	public Date stringToObject(String xml) {
		return fromString( xml );
	}
}
