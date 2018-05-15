/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.customtype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ParametrizedTestUserType implements UserType, ParameterizedType {
	private static final int[] TYPES = new int[] {Types.VARCHAR};

	private String param1;
	private String param2;

	public void setParameterValues(Properties parameters) {
		param1 = parameters.getProperty( "param1" );
		param2 = parameters.getProperty( "param2" );
	}

	public Class returnedClass() {
		return String.class;
	}

	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return StringType.INSTANCE.nullSafeGet( rs, names[0], session );
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( value != null ) {
			String v = (String) value;
			if ( !v.startsWith( param1 ) ) {
				v = param1 + v;
			}
			if ( !v.endsWith( param2 ) ) {
				v = v + param2;
			}
			StringType.INSTANCE.nullSafeSet( st, v, index, session );
		}
		else {
			StringType.INSTANCE.nullSafeSet( st, null, index, session );
		}
	}

	public int[] sqlTypes() {
		return TYPES;
	}

	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		//noinspection ObjectEquality
		if ( x == y ) {
			return true;
		}

		if ( x == null || y == null ) {
			return false;
		}

		return x.equals( y );
	}

	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	public boolean isMutable() {
		return false;
	}

	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}
}
