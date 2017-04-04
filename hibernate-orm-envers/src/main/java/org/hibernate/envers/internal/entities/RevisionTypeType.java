/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.type.IntegerType;
import org.hibernate.usertype.UserType;

/**
 * A hibernate type for the {@link RevisionType} enum.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionTypeType implements UserType, Serializable {
	private static final long serialVersionUID = -1053201518229282688L;

	private static final int[] SQL_TYPES = {Types.TINYINT};

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class returnedClass() {
		return RevisionType.class;
	}

	@Override
	public RevisionType nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		final Integer representationInt = IntegerType.INSTANCE.nullSafeGet( resultSet, names[0], session );
		return representationInt == null ?
				null :
				RevisionType.fromRepresentation( representationInt.byteValue() );
	}

	@Override
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		IntegerType.INSTANCE.nullSafeSet(
				preparedStatement,
				(value == null ? null : ( (RevisionType) value ).getRepresentation().intValue()),
				index,
				session
		);
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
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return EqualsHelper.equals( x, y );
	}
}
