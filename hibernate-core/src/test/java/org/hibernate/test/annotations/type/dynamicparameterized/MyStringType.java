/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.type.dynamicparameterized;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import org.junit.Assert;

/**
 * Always saves "[table-name].[field-name]" or "[table-name].[field-name].[optional-suffix]" into the database; this
 * makes it easier to verify that valid parameter values are being passed into {@link #setParameterValues(Properties)}.
 *
 * @author Daniel Gredler
 */
public class MyStringType implements UserType, DynamicParameterizedType {

	private String value;

	@Override
	public void setParameterValues(Properties params) {
		this.value = assertInternallyConsistent( params );
	}

	/**
	 * Verifies that the specified parameters are internally consistent and valid, and then returns the value that
	 * should be persisted to the database based on the input parameters ("[table-name].[field-name]" or
	 * "[table-name].[field-name].[optional-suffix]"), so that we can later check that the parameters were externally
	 * consistent.
	 */
	protected String assertInternallyConsistent(Properties params) {

		Boolean dynamic = Boolean.valueOf( params.getProperty( DynamicParameterizedType.IS_DYNAMIC ) );
		Assert.assertTrue( dynamic );

		String returnedClass = params.getProperty( DynamicParameterizedType.RETURNED_CLASS );
		Assert.assertEquals( String.class.getName(), returnedClass );

		Boolean primaryKey = Boolean.valueOf( params.getProperty( DynamicParameterizedType.IS_PRIMARY_KEY ) );
		Assert.assertFalse( primaryKey );

		String accessType = params.getProperty( DynamicParameterizedType.ACCESS_TYPE );
		Assert.assertEquals( "field", accessType );

		String entity = params.getProperty( DynamicParameterizedType.ENTITY );
		String propertyName = params.getProperty( DynamicParameterizedType.PROPERTY );
		XProperty xproperty = (XProperty) params.get( DynamicParameterizedType.XPROPERTY );
		Assert.assertEquals( propertyName, xproperty.getName() );
		Assert.assertEquals( entity, xproperty.getDeclaringClass().getName() );
		Assert.assertEquals( String.class.getName(), xproperty.getType().getName() );

		String tableName = propertyName.toUpperCase().split( "_" )[0];
		String columnName = propertyName.toUpperCase().split( "_" )[1];
		ParameterType parameterType = (ParameterType) params.get( DynamicParameterizedType.PARAMETER_TYPE );
		Assert.assertEquals( 1, parameterType.getColumns().length );
		Assert.assertEquals( columnName, parameterType.getColumns()[0] );
		Assert.assertEquals( String.class, parameterType.getReturnedClass() );
		Assert.assertEquals( tableName, parameterType.getTable() );

		String value = tableName + "." + columnName;
		if ( params.containsKey( "suffix" ) ) {
			value += "." + params.getProperty( "suffix" ).toUpperCase();
		}

		return value;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws SQLException {
		st.setString( index, this.value );
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws SQLException {
		return rs.getString( names[0] );
	}

	@Override
	public int[] sqlTypes() {
		return new int[] { StringType.INSTANCE.sqlType() };
	}

	@Override
	public Class<String> returnedClass() {
		return String.class;
	}

	@Override
	public boolean equals(Object x, Object y) {
		return ( x == null && y == null ) || ( x != null && y != null && x.equals( y ) );
	}

	@Override
	public int hashCode(Object x) {
		return x.hashCode();
	}

	@Override
	public Object deepCopy(Object value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) {
		return (Integer) value;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) {
		return original;
	}
}
