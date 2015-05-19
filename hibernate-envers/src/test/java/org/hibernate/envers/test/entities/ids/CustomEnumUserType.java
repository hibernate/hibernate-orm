/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.ids;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

/**
 * @author Slawek Garwol (slawekgarwol at gmail dot com)
 */
public class CustomEnumUserType implements UserType {
	private static final int[] SQL_TYPES = {Types.VARCHAR};

	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	public Class returnedClass() {
		return CustomEnum.class;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( (x == null) || (y == null) ) {
			return false;
		}
		return x.equals( y );
	}

	public int hashCode(Object x) throws HibernateException {
		return (x == null) ? 0 : x.hashCode();
	}

	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		String name = rs.getString( names[0] );
		if ( rs.wasNull() ) {
			return null;
		}
		return CustomEnum.fromYesNo( name );
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
			throws HibernateException, SQLException {
		CustomEnum val = (CustomEnum) value;
		if ( val == null ) {
			st.setNull( index, Types.VARCHAR );
		}
		else {
			st.setString( index, val.toYesNo() );
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
}
