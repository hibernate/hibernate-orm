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
public class DollarValueUserType implements UserType<DollarValue> {

	@Override
	public int getSqlType() {
		return Types.BIGINT;
	}

	@Override
	@Nonnull
	public Class<DollarValue> returnedClass() {
		return DollarValue.class;
	}

	@Override
	public boolean equals(@Nullable DollarValue x, @Nullable DollarValue y) throws HibernateException {
		return x == y || x != null && y != null && x.getAmount().equals( y.getAmount() );
	}

	@Override
	public int hashCode(@Nonnull DollarValue x) throws HibernateException {
		return x.getAmount().hashCode();
	}

	@Override
	@Nullable
	public DollarValue nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		return new DollarValue( rs.getBigDecimal( position ) );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable DollarValue value,
			int index,
			@Nonnull WrapperOptions options) throws SQLException {
		st.setBigDecimal( index, value == null ? null : value.getAmount() );
	}

	@Override
	@Nullable
	public DollarValue deepCopy(@Nullable DollarValue value) throws HibernateException {
		return value == null ? null : new DollarValue( value.getAmount() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable DollarValue value) throws HibernateException {
		return null;
	}

	@Override
	@Nullable
	public DollarValue assemble(@Nullable Serializable cached, @Nullable Object owner)
			throws HibernateException {
		return null;
	}

	@Override
	@Nullable
	public DollarValue replace(@Nullable DollarValue original, @Nullable DollarValue target, @Nullable Object owner)
			throws HibernateException {
		return null;
	}
}
