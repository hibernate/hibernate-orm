/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.ids;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * @author Slawek Garwol (slawekgarwol at gmail dot com)
 */
public class CustomEnumUserType implements UserType {
	private static final int[] SQL_TYPES = {Types.VARCHAR};

	@Override
	public int sqlTypeCode() {
		return Types.VARCHAR;
	}

	@Override
	public Class returnedClass() {
		return CustomEnum.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( (x == null) || (y == null) ) {
			return false;
		}
		return x.equals( y );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return (x == null) ? 0 : x.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, int parameterPosition, SharedSessionContractImplementor session)
	throws HibernateException, SQLException {
		String name = rs.getString( parameterPosition );
		if ( rs.wasNull() ) {
			return null;
		}
		return CustomEnum.fromYesNo( name );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
	throws HibernateException, SQLException {
		CustomEnum val = (CustomEnum) value;
		if ( val == null ) {
			st.setNull( index, Types.VARCHAR );
		}
		else {
			st.setString( index, val.toYesNo() );
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
}
