/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.dynamic;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class AgeType implements UserType<Age> {

	@Override
	public int getSqlType() {
		return Types.INTEGER;
	}

	@Override
	public Class<Age> returnedClass() {
		return Age.class;
	}

	@Override
	public boolean equals(Age x, Age y) throws HibernateException {
		return x != null ? x.equals( y ) : y == null;
	}

	@Override
	public int hashCode(Age x) throws HibernateException {
		return x != null ? x.hashCode() : 1;
	}

	@Override
	public Age nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session)
			throws SQLException {
		return new Age( rs.getInt( position ) );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Age value, int index, SharedSessionContractImplementor session)
			throws SQLException {
		st.setInt( index, value.getAgeInYears() );
	}

	@Override
	public Age deepCopy(Age value) throws HibernateException {
		return new Age( value.getAgeInYears() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Age value) throws HibernateException {
		return null;
	}

	@Override
	public Age assemble(Serializable cached, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public Age replace(Age original, Age target, Object owner) throws HibernateException {
		return null;
	}
}
