package org.hibernate.orm.test.typeparameters;

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
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import org.jboss.logging.Logger;


/**
 * @author Michi
 */
public class DefaultValueIntegerType implements UserType<Integer>, ParameterizedType, Serializable {
	private static final Logger log = Logger.getLogger( DefaultValueIntegerType.class );

	private Integer defaultValue;

	@Override
	public int getSqlType() {
		return Types.INTEGER;
	}

	@Nonnull
	public Class returnedClass() {
		return int.class;
	}

	public boolean equals(@Nullable Integer x, @Nullable Integer y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return x.equals( y );
	}

	@Override
	@Nullable
	public Integer nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		Number result = (Number) rs.getObject( position );
		return result == null ? defaultValue : Integer.valueOf( result.intValue() );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable Integer value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		if ( value == null || defaultValue.equals( value ) ) {
			log.trace( "binding null to parameter: " + index );
			st.setNull( index, Types.INTEGER );
		}
		else {
			log.trace( "binding " + value + " to parameter: " + index );
			st.setInt( index, value );
		}
	}

	@Nullable
	public Integer deepCopy(@Nullable Integer value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	public int hashCode(@Nonnull Integer x) throws HibernateException {
		return x.hashCode();
	}

	@Nullable
	public Integer assemble(@Nullable Serializable cached, @Nullable Object owner)
			throws HibernateException {
		return (Integer) cached;
	}

	@Nullable
	public Serializable disassemble(@Nullable Integer value) throws HibernateException {
		return value;
	}

	@Nullable
	public Integer replace(@Nullable Integer original, @Nullable Integer target, @Nullable Object owner)
			throws HibernateException {
		return original;
	}

	public void setParameterValues(Properties parameters) {
		this.defaultValue = Integer.valueOf( (String) parameters.get( "default" ) );
	}

}
