/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.EnhancedUserType;

/**
 * A custom type for mapping {@link Classification} instances
 * to the respective db column.
 * </p>
 * THis is largely intended to mimic JDK5 enum support in JPA.  Here we are
 * using the approach of storing the ordinal values, rather than the names.
 *
 * @author Steve Ebersole
 */
public class ClassificationType implements EnhancedUserType<Classification>, ValueExtractor<Classification> {

	@Override
	public int getSqlType() {
		return Types.TINYINT;
	}

	@Override
	public Class<Classification> returnedClass() {
		return Classification.class;
	}

	@Override
	public boolean equals(Classification x, Classification y) throws HibernateException {
		if ( x == null && y == null ) {
			return false;
		}
		else if ( x != null ) {
			return x.equals( y );
		}
		else {
			return y.equals( x );
		}
	}

	@Override
	public int hashCode(Classification x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public Classification nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		final int intValue = rs.getInt( position );
		if ( rs.wasNull() ) {
			return null;
		}
		return Classification.valueOf( intValue );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Classification value, int index, WrapperOptions options)
			throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.INTEGER );
		}
		else {
			st.setInt( index, value.ordinal() );
		}
	}

	@Override
	public Classification deepCopy(Classification value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Classification value) throws HibernateException {
		return ( Classification ) value;
	}

	@Override
	public Classification assemble(Serializable cached, Object owner) throws HibernateException {
		return (Classification) cached;
	}

	@Override
	public Classification replace(Classification original, Classification target, Object owner) throws HibernateException {
		return original;
	}

	@Override
	public String toSqlLiteral(Classification value) {
		return Integer.toString( value.ordinal() );
	}

	@Override
	public String toString(Classification value) throws HibernateException {
		return value.name();
	}

	@Override
	public Classification fromStringValue(CharSequence sequence) {
		return Classification.valueOf( sequence.toString() );
	}

	@Override
	public Classification extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
		return Classification.valueOf( rs.getInt( paramIndex ) );
	}

	@Override
	public Classification extract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
		return Classification.valueOf( statement.getInt( paramIndex ) );
	}

	@Override
	public Classification extract(CallableStatement statement, String paramName, WrapperOptions options) throws SQLException {
		return Classification.valueOf( statement.getInt( paramName ) );
	}
}
