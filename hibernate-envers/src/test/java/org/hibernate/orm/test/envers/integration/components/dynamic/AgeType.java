package org.hibernate.orm.test.envers.integration.components.dynamic;

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

public class AgeType implements UserType<Age> {

	@Override
	public int getSqlType() {
		return Types.INTEGER;
	}

	@Override
	@Nonnull
	public Class<Age> returnedClass() {
		return Age.class;
	}

	@Override
	public boolean equals(@Nullable Age x, @Nullable Age y) throws HibernateException {
		return x != null ? x.equals( y ) : y == null;
	}

	@Override
	public int hashCode(@Nonnull Age x) throws HibernateException {
		return x != null ? x.hashCode() : 1;
	}

	@Override
	@Nullable
	public Age nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		return new Age( rs.getInt( position ) );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable Age value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		if ( value == null ) {
			st.setNull( index, getSqlType() );
		}
		else {
			st.setInt( index, value.getAgeInYears() );
		}
	}

	@Override
	@Nullable
	public Age deepCopy(@Nullable Age value) throws HibernateException {
		return value == null ? null : new Age( value.getAgeInYears() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable Age value) throws HibernateException {
		return null;
	}

	@Override
	@Nullable
	public Age assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return null;
	}

	@Override
	@Nullable
	public Age replace(@Nullable Age original, @Nullable Age target, @Nullable Object owner) throws HibernateException {
		return null;
	}
}
