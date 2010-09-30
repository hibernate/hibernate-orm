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

import java.sql.Time;
import java.util.Date;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.JdbcTimeTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIME TIME} and {@link Time}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TimeType
		extends AbstractSingleColumnStandardBasicType<Date>
		implements LiteralType<Date> {

	public static final TimeType INSTANCE = new TimeType();

	public TimeType() {
		super( org.hibernate.type.descriptor.sql.TimeTypeDescriptor.INSTANCE, JdbcTimeTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "time";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {
				getName(),
				java.sql.Time.class.getName()
		};
	}

	//	@Override
//	protected boolean registerUnderJavaType() {
//		return true;
//	}

	public String objectToSQLString(Date value, Dialect dialect) throws Exception {
		Time jdbcTime = Time.class.isInstance( value )
				? ( Time ) value
				: new Time( value.getTime() );
		// TODO : use JDBC time literal escape syntax? -> {t 'time-string'} in hh:mm:ss format
		return StringType.INSTANCE.objectToSQLString( jdbcTime.toString(), dialect );
	}
}
