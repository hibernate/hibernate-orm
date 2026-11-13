package org.hibernate.orm.test.envers.entities.ids;

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
 * @author Slawek Garwol (slawekgarwol at gmail dot com)
 */
public class CustomEnumUserType implements UserType<CustomEnum> {

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Nonnull
	public Class<CustomEnum> returnedClass() {
		return CustomEnum.class;
	}

	public boolean equals(@Nullable CustomEnum x, @Nullable CustomEnum y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( (x == null) || (y == null) ) {
			return false;
		}
		return x.equals( y );
	}

	public int hashCode(@Nonnull CustomEnum x) throws HibernateException {
		return (x == null) ? 0 : x.hashCode();
	}

	@Override
	@Nullable
	public CustomEnum nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		String name = rs.getString( position );
		if ( rs.wasNull() ) {
			return null;
		}
		return CustomEnum.fromYesNo( name );
	}

	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable CustomEnum value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.VARCHAR );
		}
		else {
			st.setString( index, value.toYesNo() );
		}
	}

	@Nullable
	public CustomEnum deepCopy(@Nullable CustomEnum value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	@Nullable
	public Serializable disassemble(@Nullable CustomEnum value) throws HibernateException {
		return value;
	}

	@Nullable
	public CustomEnum assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return (CustomEnum) cached;
	}

	@Nullable
	public CustomEnum replace(
			@Nullable CustomEnum original,
			@Nullable CustomEnum target,
			@Nullable Object owner) throws HibernateException {
		return original;
	}
}
