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
