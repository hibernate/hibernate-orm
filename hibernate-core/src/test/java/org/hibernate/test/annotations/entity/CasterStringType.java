//$Id$
package org.hibernate.test.annotations.entity;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
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

	public int[] sqlTypes() {
		return new int[]{Types.VARCHAR};
	}

	public Class returnedClass() {
		return String.class;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		return ( x == y ) || ( x != null && x.equals( y ) );
	}

	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		String result = rs.getString( names[0] );
		if ( rs.wasNull() ) return null;
		if ( parameters.getProperty( CAST ).equals( "lower" ) ) {
			return result.toLowerCase();
		}
		else {
			return result.toUpperCase();
		}
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, sqlTypes()[0] );
		}
		else {
			String string = (String) value;
			if ( parameters.getProperty( CAST ).equals( "lower" ) ) {
				string = string.toLowerCase();
			}
			else {
				string = string.toUpperCase();
			}
			st.setString( index, string );
		}
	}

	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	public void setParameterValues(Properties parameters) {
		this.parameters = parameters;
	}
}
