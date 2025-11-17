/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

/**
 * @author Emmanuel Bernard
 */
public class StateType implements UserType<State> {

	@Override
	public int getSqlType() {
		return Types.INTEGER;
	}

	public Class<State> returnedClass() {
		return State.class;
	}

	public boolean equals(State x, State y) throws HibernateException {
		return x == y;
	}

	public int hashCode(State x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public State nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		int result = rs.getInt( position );
		if ( rs.wasNull() ) return null;
		return State.values()[result];
	}

	@Override
	public void nullSafeSet(PreparedStatement st, State value, int index, WrapperOptions options)
			throws HibernateException, SQLException {
		if (value == null) {
			st.setNull( index, Types.INTEGER );
		}
		else {
			st.setInt( index, value.ordinal() );
		}
	}

	public State deepCopy(State value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	public Serializable disassemble(State value) throws HibernateException {
		return value;
	}

	public State assemble(Serializable cached, Object owner) throws HibernateException {
		return (State) cached;
	}

	public State replace(State original, State target, Object owner) throws HibernateException {
		return original;
	}
}
