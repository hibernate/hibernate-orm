package org.hibernate.orm.test.envers.entities.customtype;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ParametrizedTestUserType implements UserType<String>, ParameterizedType {

	private String param1;
	private String param2;

	public void setParameterValues(Properties parameters) {
		param1 = parameters.getProperty( "param1" );
		param2 = parameters.getProperty( "param2" );
	}

	@Nonnull
	public Class<String> returnedClass() {
		return String.class;
	}

	@Override
	@Nullable
	public String nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		final String string = rs.getString( position );
		return rs.wasNull() ? null : string;
	}

	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable String value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		if ( value != null ) {
			if ( !value.startsWith( param1 ) ) {
				value = param1 + value;
			}
			if ( !value.endsWith( param2 ) ) {
				value = value + param2;
			}
		}
		VarcharJdbcType.INSTANCE.getBinder( StringJavaType.INSTANCE )
				.bind( st, value, index, options );
	}

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Nullable
	public String assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return (String) cached;
	}

	@Nullable
	public String deepCopy(@Nullable String value) throws HibernateException {
		return value;
	}

	@Nullable
	public Serializable disassemble(@Nullable String value) throws HibernateException {
		return value;
	}

	public boolean equals(@Nullable String x, @Nullable String y) throws HibernateException {
		//noinspection ObjectEquality
		if ( x == y ) {
			return true;
		}

		if ( x == null || y == null ) {
			return false;
		}

		return x.equals( y );
	}

	public int hashCode(@Nonnull String x) throws HibernateException {
		return x.hashCode();
	}

	public boolean isMutable() {
		return false;
	}

	@Nullable
	public String replace(
			@Nullable String original,
			@Nullable String target,
			@Nullable Object owner) throws HibernateException {
		return original;
	}
}
