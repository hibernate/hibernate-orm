package org.hibernate.orm.test.annotations.usertype;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.HibernateException;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.EnhancedUserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class MyType implements EnhancedUserType<MyId> {
	@Override
	public String toSqlLiteral(MyId value) {
		return "'" + value.text.replace("'", "''") + "'";
	}

	@Override
	public String toString(MyId value) throws HibernateException {
		return value == null ? null : value.text;
	}

	@Override
	public MyId fromStringValue(CharSequence sequence) throws HibernateException {
		return new MyId(sequence.toString());
	}

	@Override
	public int getSqlType() {
		return SqlTypes.VARCHAR;
	}

	@Override
	@Nonnull
	public Class<MyId> returnedClass() {
		return MyId.class;
	}

	@Override
	public boolean equals(@Nullable MyId x, @Nullable MyId y) {
		return x == y || x != null && y != null && x.text.equals(y.text);
	}

	@Override
	public int hashCode(@Nonnull MyId x) {
		return x.text.hashCode();
	}

	@Override
	@Nullable
	public MyId nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		return new MyId(rs.getString(position));
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable MyId value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		st.setString(index, value == null ? null : value.text);
	}

	@Override
	@Nullable
	public MyId deepCopy(@Nullable MyId value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable MyId value) {
		return value == null ? null : value.text;
	}

	@Override
	@Nullable
	public MyId assemble(@Nullable Serializable cached, @Nullable Object owner) {
		return cached == null ? null : new MyId((String) cached);
	}
}
