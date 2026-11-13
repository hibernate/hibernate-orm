package org.hibernate.orm.test.type.contributor.usertype;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import org.jboss.logging.Logger;

/**
 * @author Christian Beikov
 */
public class StringWrapperUserType implements UserType<StringWrapper> {

	public static final StringWrapperUserType INSTANCE = new StringWrapperUserType();

	private static final Logger log = Logger.getLogger( StringWrapperUserType.class );

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	@Nonnull
	public Class<StringWrapper> returnedClass() {
		return StringWrapper.class;
	}

	@Override
	public boolean equals(@Nullable StringWrapper x, @Nullable StringWrapper y)
			throws HibernateException {
		return Objects.equals( x, y );
	}

	@Override
	public int hashCode(@Nonnull StringWrapper x)
			throws HibernateException {
		return Objects.hashCode( x );
	}

	@Override
	@Nullable
	public StringWrapper nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		String columnValue = (String) rs.getObject( position );
		log.debugv( "Result set column {0} value is {1}", position, columnValue );
		return columnValue == null ? null : fromString( columnValue );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st, @Nullable StringWrapper value, int index, @Nonnull WrapperOptions options)
			throws SQLException {
		if ( value == null ) {
			log.debugv("Binding null to parameter {0} ",index);
			st.setNull( index, Types.VARCHAR );
		}
		else {
			String stringValue = toString( value );
			log.debugv("Binding {0} to parameter {1} ", stringValue, index);
			st.setString( index, stringValue );
		}
	}
	public String toString(StringWrapper value) {
		return value.getValue();
	}

	public StringWrapper fromString(String string) {
		if ( string == null || string.isEmpty() ) {
			return null;
		}
		return new StringWrapper( string );
	}

	@Override
	@Nullable
	public StringWrapper deepCopy(@Nullable StringWrapper value)
			throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable StringWrapper value)
			throws HibernateException {
		return value == null ? null : value.getValue().getBytes();
	}

	@Override
	@Nullable
	public StringWrapper assemble(@Nullable Serializable cached, @Nullable Object owner)
			throws HibernateException {
		return cached == null ? null : new StringWrapper( new String( (byte[]) cached ) );
	}

	@Override
	@Nullable
	public StringWrapper replace(
			@Nullable StringWrapper original,
			@Nullable StringWrapper target,
			@Nullable Object owner)
			throws HibernateException {
		return deepCopy( original );
	}
}
