/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link Timestamp}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TimestampType
		extends AbstractSingleColumnStandardBasicType<Date> {

	public static final TimestampType INSTANCE = new TimestampType();

	public TimestampType() {
		super( TimestampJdbcType.INSTANCE, JdbcTimestampJavaTypeDescriptor.INSTANCE );
	}

	protected TimestampType(JdbcType jdbcType, JavaType<Date> javaTypeDescriptor) {
		super( jdbcType, javaTypeDescriptor );
	}

	@Override
	public String getName() {
		return "timestamp";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), Timestamp.class.getName(), Date.class.getName() };
	}

}
