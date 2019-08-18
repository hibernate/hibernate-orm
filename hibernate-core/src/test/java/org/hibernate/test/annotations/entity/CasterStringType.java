/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.entity;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * Sample of parameter type
 *
 * @author Emmanuel Bernard
 */
public class CasterStringType implements UserType, ParameterizedType {
	private static final String CAST = "cast";
	private Properties parameters;

	@Override
	public int[] sqlTypes() {
		return new int[]{Types.VARCHAR};
	}

	@Override
	public Class returnedClass() {
		return String.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return ( x == y ) || ( x != null && x.equals( y ) );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
		String result = rs.getString( names[0] );
		if ( rs.wasNull() ) return null;
		if ( parameters.getProperty( CAST ).equals( "lower" ) ) {
			return result.toLowerCase(Locale.ROOT);
		}
		else {
			return result.toUpperCase(Locale.ROOT);
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, sqlTypes()[0] );
		}
		else {
			String string = (String) value;
			if ( parameters.getProperty( CAST ).equals( "lower" ) ) {
				string = string.toLowerCase(Locale.ROOT);
			}
			else {
				string = string.toUpperCase(Locale.ROOT);
			}
			st.setString( index, string );
		}
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	@Override
	public void setParameterValues(Properties parameters) {
		this.parameters = parameters;
	}
}
