package org.hibernate.orm.test.rowid;

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
 * @author Gavin King
 */
public class RowIdType implements UserType<Object>{

	@Override
	public int getSqlType() {
		return Types.ROWID;
	}

	@Nonnull
	public Class<Object> returnedClass() {
		return Object.class;
	}

	public boolean equals(@Nullable Object x, @Nullable Object y) throws HibernateException {
		return x == null ? y == null : x.equals( y );
	}

	public int hashCode(@Nonnull Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	@Nullable
	public Object nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		return rs.getObject( position );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable Object value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Nullable
	public Object deepCopy(@Nullable Object value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	@Nullable
	public Serializable disassemble(@Nullable Object value) throws HibernateException {
		return null;
	}

	@Nullable
	public Object assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return null;
	}

	@Nullable
	public Object replace(
			@Nullable Object original,
			@Nullable Object target,
			@Nullable Object owner) throws HibernateException {
		return null;
	}

}
