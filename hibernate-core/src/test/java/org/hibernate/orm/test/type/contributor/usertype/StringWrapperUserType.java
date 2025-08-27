/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import org.jboss.logging.Logger;

/**
 * @author Christian Beikov
 */
public class StringWrapperUserType implements UserType<StringWrapper> {

	public static final StringWrapperUserType INSTANCE = new StringWrapperUserType();

	private static final Logger log = Logger.getLogger( StringWrapperUserType.class );

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	public Class<StringWrapper> returnedClass() {
		return StringWrapper.class;
	}

	@Override
	public boolean equals(StringWrapper x, StringWrapper y)
			throws HibernateException {
		return Objects.equals( x, y );
	}

	@Override
	public int hashCode(StringWrapper x)
			throws HibernateException {
		return Objects.hashCode( x );
	}

	@Override
	public StringWrapper nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		String columnValue = (String) rs.getObject( position );
		log.debugv( "Result set column {0} value is {1}", position, columnValue );
		return columnValue == null ? null : fromString( columnValue );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st, StringWrapper value, int index, WrapperOptions options)
			throws SQLException {
		if ( value == null ) {
			log.debugv("Binding null to parameter {0} ",index);
			st.setNull( index, Types.VARCHAR );
		}
		else {
			String stringValue = toString( value );
			log.debugv("Binding {0} to parameter {1} ", stringValue, index);
			st.setString( index, stringValue );
		}
	}
	public String toString(StringWrapper value) {
		return value.getValue();
	}

	public StringWrapper fromString(String string) {
		if ( string == null || string.isEmpty() ) {
			return null;
		}
		return new StringWrapper( string );
	}

	@Override
	public StringWrapper deepCopy(StringWrapper value)
			throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(StringWrapper value)
			throws HibernateException {
		return value == null ? null : value.getValue().getBytes();
	}

	@Override
	public StringWrapper assemble(Serializable cached, Object owner)
			throws HibernateException {
		return new StringWrapper( new String( (byte[]) cached ) );
	}

	@Override
	public StringWrapper replace(StringWrapper original, StringWrapper target, Object owner)
			throws HibernateException {
		return deepCopy( original );
	}
}
