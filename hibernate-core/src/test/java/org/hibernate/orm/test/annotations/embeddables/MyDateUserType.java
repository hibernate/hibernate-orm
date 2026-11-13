package org.hibernate.orm.test.annotations.embeddables;

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
 * @author Chris Pheby
 */
public class MyDateUserType implements UserType<MyDate> {

	@Override
	public int getSqlType() {
		return Types.DATE;
	}

	@Override
	@Nonnull
	public Class<MyDate> returnedClass() {
		return MyDate.class;
	}

	@Override
	public boolean equals(@Nullable MyDate x, @Nullable MyDate y) throws HibernateException {
		return x == y || x != null && y != null && x.getDate().equals( y.getDate() );
	}

	@Override
	public int hashCode(@Nonnull MyDate x) throws HibernateException {
		return x.getDate().hashCode();
	}

	@Override
	@Nullable
	public MyDate nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		return new MyDate( rs.getDate( position ) );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable MyDate value,
			int index,
			@Nonnull WrapperOptions options) throws SQLException {
		st.setDate( index, value == null ? null : new java.sql.Date( value.getDate().getTime() ) );
	}

	@Override
	@Nullable
	public MyDate deepCopy(@Nullable MyDate value) throws HibernateException {
		return value == null ? null : new MyDate( value.getDate() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable MyDate value) throws HibernateException {
		return null;
	}

	@Override
	@Nullable
	public MyDate assemble(@Nullable Serializable cached, @Nullable Object owner)
			throws HibernateException {
		return null;
	}

	@Override
	@Nullable
	public MyDate replace(@Nullable MyDate original, @Nullable MyDate target, @Nullable Object owner)
			throws HibernateException {
		return null;
	}
}
