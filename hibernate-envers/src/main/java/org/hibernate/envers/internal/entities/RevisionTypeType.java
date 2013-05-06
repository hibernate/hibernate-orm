/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.internal.entities;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
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
	public RevisionType nullSafeGet(ResultSet resultSet, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		final Integer representationInt = IntegerType.INSTANCE.nullSafeGet( resultSet, names[0], session );
		return representationInt == null ?
				null :
				RevisionType.fromRepresentation( representationInt.byteValue() );
	}

	@Override
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SessionImplementor session)
			throws HibernateException, SQLException {
		IntegerType.INSTANCE.nullSafeSet(
				preparedStatement,
				(value == null ? null : ((RevisionType) value).getRepresentation().intValue()),
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


