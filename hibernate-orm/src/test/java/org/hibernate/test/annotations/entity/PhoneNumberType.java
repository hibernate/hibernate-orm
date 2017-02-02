/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.entity;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * Used to persist and retrieve objects of type 'PhoneNumber' 
 *
 * @author Sharath Reddy 
 */
public class PhoneNumberType implements UserType {

	public int[] sqlTypes() {
		return new int[]{Types.VARCHAR};
	}

	public Class<?> returnedClass() {
		return PhoneNumber.class;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		return ( x == y ) || ( x != null && x.equals( y ) );
	}

	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
		String result = rs.getString( names[0] );
		if ( rs.wasNull() ) return null;
		
		if (result.length() <= 6) {
			return new PhoneNumber(result);
		}
		else {
			return new OverseasPhoneNumber(result);
		}
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, sqlTypes()[0] );
			return;
		}
		
		PhoneNumber phoneNumber = (PhoneNumber) value;
		String number = phoneNumber.getNumber();
		st.setString( index, number);
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

}
