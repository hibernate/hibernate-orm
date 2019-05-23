/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.components.dynamic;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.usertype.UserType;

public class AgeType implements UserType {
	@Override
	public int sqlTypeCode() {
		return StandardSpiBasicTypes.INTEGER.getSqlTypeDescriptor().getJdbcTypeCode();
	}

	@Override
	public Class<Age> returnedClass() {
		return Age.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return Objects.equals( x, y );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x != null ? x.hashCode() : 1;
	}

	@Override
	public Object nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session)
	throws HibernateException, SQLException {
		return new Age( rs.getInt( position ) );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
	throws HibernateException, SQLException {
		st.setInt( index, ( (Age) value ).getAgeInYears() );
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return new Age( ( (Age) value ).getAgeInYears() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return null;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return null;
	}
}
