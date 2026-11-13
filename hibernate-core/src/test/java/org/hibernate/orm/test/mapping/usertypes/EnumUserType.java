package org.hibernate.orm.test.mapping.usertypes;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

public class EnumUserType<T extends Enum<T>> implements UserType<T>, ParameterizedType {

	private Class<T> clazz = null;

	public static <T extends Enum<T>> EnumUserType<T> createInstance(Class<T> clazz) {
		if ( !clazz.isEnum() ) {
			throw new IllegalArgumentException( "Parameter has to be an enum-class" );
		}
		EnumUserType<T> that = new EnumUserType<>();
		Properties p = new Properties();
		p.setProperty( "enumClassName", clazz.getName() );
		that.setParameterValues( p );
		return that;
	}

	public void setParameterValues(@Nonnull Properties params) {
		String enumClassName = params.getProperty( "enumClassName" );
		if ( enumClassName == null ) {
			throw new MappingException( "enumClassName parameter not specified" );
		}

		try {
			//noinspection unchecked
			this.clazz = (Class<T>) Class.forName( enumClassName );
		}
		catch (ClassNotFoundException e) {
			throw new MappingException( "enumClass " + enumClassName + " not found", e );
		}
		if ( !this.clazz.isEnum() ) {
			throw new MappingException( "enumClass " + enumClassName + " doesn't refer to an Enum" );
		}
	}

	@Override
	public int getSqlType() {
		return Types.CHAR;
	}

	@Nonnull
	public Class<T> returnedClass() {
		return clazz;
	}

	@Override
	@Nullable
	public T nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		final String name = rs.getString( position );
		if ( rs.wasNull() ) {
			return null;
		}
		return Enum.valueOf( clazz, name.trim() );
	}

	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index)
			throws HibernateException, SQLException {
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement preparedStatement,
			@Nullable T value,
			int index,
			@Nonnull WrapperOptions options) throws SQLException {
		if ( null == value ) {
			preparedStatement.setNull( index, Types.VARCHAR );
		}
		else {
			preparedStatement.setString( index, value.name() );
		}
	}

	@Nullable
	public T deepCopy(@Nullable T value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	@Nullable
	public T assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		//noinspection unchecked
		return (T) cached;
	}

	@Nullable
	public Serializable disassemble(@Nullable T value) throws HibernateException {
		return value;
	}

	@Nullable
	public T replace(@Nullable T original, @Nullable T target, @Nullable Object owner) throws HibernateException {
		return original;
	}

	public int hashCode(@Nonnull T x) throws HibernateException {
		return x.hashCode();
	}

	public boolean equals(@Nullable T x, @Nullable T y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( null == x || null == y ) {
			return false;
		}
		return x.equals( y );
	}
}
