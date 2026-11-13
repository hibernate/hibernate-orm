package org.hibernate.orm.test.annotations.type.dynamicparameterized;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

/**
 * @author Yanming Zhou
 */
public class MyGenericType implements UserType<Object>, DynamicParameterizedType {

	private ParameterType parameterType;

	public ParameterType getParameterType() {
		return parameterType;
	}

	@Override
	public void setParameterValues(Properties params) {
		parameterType = (ParameterType) params.get(PARAMETER_TYPE);
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable Object value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		st.setString( index, null );
	}

	@Override
	@Nullable
	public Object nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		return null;
	}

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	@Nonnull
	public Class<Object> returnedClass() {
		return Object.class;
	}

	@Override
	public boolean equals(@Nullable Object x, @Nullable Object y) {
		return ( x == null && y == null ) || ( x != null && x.equals( y ) );
	}

	@Override
	public int hashCode(@Nonnull Object x) {
		return x.hashCode();
	}

	@Override
	@Nullable
	public Object deepCopy(@Nullable Object value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable Object value) {
		return null;
	}

	@Override
	@Nullable
	public String assemble(@Nullable Serializable cached, @Nullable Object owner) {
		return (String) cached;
	}

}
