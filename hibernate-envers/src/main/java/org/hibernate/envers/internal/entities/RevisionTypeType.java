/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.envers.RevisionType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

/**
 * A hibernate type for the {@link RevisionType} enum.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionTypeType implements UserType<RevisionType>, Serializable {
	private static final long serialVersionUID = -1053201518229282688L;

	@Override
	public int getSqlType() {
		return Types.TINYINT;
	}

	@Override
	public Class<RevisionType> returnedClass() {
		return RevisionType.class;
	}

	@Override
	public RevisionType nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		byte byteValue = rs.getByte( position );
		if ( rs.wasNull() ) {
			return null;
		}
		return RevisionType.fromRepresentation( byteValue );
	}

	@Override
	public void nullSafeSet(PreparedStatement preparedStatement, RevisionType value, int index, WrapperOptions options)
			throws SQLException {
		if ( value == null ) {
			preparedStatement.setNull( index, Types.TINYINT );
		}
		else {
			preparedStatement.setByte( index, value.getRepresentation() );
		}
	}

	@Override
	public RevisionType deepCopy(RevisionType value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public RevisionType assemble(Serializable cached, Object owner) throws HibernateException {
		return (RevisionType) cached;
	}

	@Override
	public Serializable disassemble(RevisionType value) throws HibernateException {
		return value;
	}

	@Override
	public RevisionType replace(RevisionType original, RevisionType target, Object owner) throws HibernateException {
		return original;
	}

	@Override
	public int hashCode(RevisionType x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public boolean equals(RevisionType x, RevisionType y) throws HibernateException {
		return Objects.equals( x, y );
	}
}
