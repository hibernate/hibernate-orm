/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: DoubleStringType.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

public class DoubleStringType implements CompositeUserType {

	private static final int[] TYPES = {Types.VARCHAR, Types.VARCHAR};

	public int[] sqlTypes() {
		return TYPES;
	}

	@Override
	public Class returnedClass() {
		return String[].class;
	}

	@Override
	public boolean equals(Object x, Object y) {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return ( (String[]) x )[0].equals( ( (String[]) y )[0] ) && ( (String[]) x )[1].equals( ( (String[]) y )[1] );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		String[] a = (String[]) x;
		return a[0].hashCode() + 31 * a[1].hashCode();
	}

	@Override
	public Object deepCopy(Object x) {
		if ( x == null ) {
			return null;
		}
		String[] result = new String[2];
		String[] input = (String[]) x;
		result[0] = input[0];
		result[1] = input[1];
		return result;
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {

		String first = StringType.INSTANCE.nullSafeGet( rs, names[0], session );
		String second = StringType.INSTANCE.nullSafeGet( rs, names[1], session );

		return ( first == null && second == null ) ? null : new String[] {first, second};
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

		String[] strings = ( value == null ) ? new String[2] : (String[]) value;

		StringType.INSTANCE.nullSafeSet( st, strings[0], index, session );
		StringType.INSTANCE.nullSafeSet( st, strings[1], index + 1, session );
	}

	@Override
	public String[] getPropertyNames() {
		return new String[] {"s1", "s2"};
	}

	@Override
	public Type[] getPropertyTypes() {
		return new Type[] {StringType.INSTANCE, StringType.INSTANCE};
	}

	@Override
	public Object getPropertyValue(Object component, int property) {
		return ( (String[]) component )[property];
	}

	@Override
	public void setPropertyValue(
			Object component,
			int property,
			Object value) {

		( (String[]) component )[property] = (String) value;
	}

	@Override
	public Object assemble(
			Serializable cached,
			SharedSessionContractImplementor session,
			Object owner) {

		return deepCopy( cached );
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session) {
		return (Serializable) deepCopy( value );
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return original;
	}

}
