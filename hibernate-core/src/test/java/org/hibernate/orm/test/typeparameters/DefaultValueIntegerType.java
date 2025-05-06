/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.typeparameters;

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

	public Class returnedClass() {
		return int.class;
	}

	public boolean equals(Integer x, Integer y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return x.equals( y );
	}

	@Override
	public Integer nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		Number result = (Number) rs.getObject( position );
		return result == null ? defaultValue : Integer.valueOf( result.intValue() );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Integer value, int index, WrapperOptions options)
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

	public Integer deepCopy(Integer value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	public int hashCode(Integer x) throws HibernateException {
		return x.hashCode();
	}

	public Integer assemble(Serializable cached, Object owner)
			throws HibernateException {
		return (Integer) cached;
	}

	public Serializable disassemble(Integer value) throws HibernateException {
		return value;
	}

	public Integer replace(Integer original, Integer target, Object owner)
			throws HibernateException {
		return original;
	}

	public void setParameterValues(Properties parameters) {
		this.defaultValue = Integer.valueOf( (String) parameters.get( "default" ) );
	}

}
