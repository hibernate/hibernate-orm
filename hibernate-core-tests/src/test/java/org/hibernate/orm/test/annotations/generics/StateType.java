package org.hibernate.orm.test.annotations.generics;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

	@Nonnull
	public Class<State> returnedClass() {
		return State.class;
	}

	public boolean equals(@Nullable State x, @Nullable State y) throws HibernateException {
		return x == y;
	}

	public int hashCode(@Nonnull State x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	@Nullable
	public State nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		int result = rs.getInt( position );
		if ( rs.wasNull() ) return null;
		return State.values()[result];
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable State value,
			int index,
			@Nonnull WrapperOptions options)
			throws HibernateException, SQLException {
		if (value == null) {
			st.setNull( index, Types.INTEGER );
		}
		else {
			st.setInt( index, value.ordinal() );
		}
	}

	@Nullable
	public State deepCopy(@Nullable State value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	@Nullable
	public Serializable disassemble(@Nullable State value) throws HibernateException {
		return value;
	}

	@Nullable
	public State assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return (State) cached;
	}

	@Nullable
	public State replace(
			@Nullable State original,
			@Nullable State target,
			@Nullable Object owner) throws HibernateException {
		return original;
	}
}
