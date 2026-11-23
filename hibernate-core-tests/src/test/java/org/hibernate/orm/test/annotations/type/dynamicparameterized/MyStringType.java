package org.hibernate.orm.test.annotations.type.dynamicparameterized;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.models.spi.FieldDetails;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import org.junit.Assert;

/**
 * Always saves "[table-name].[field-name]" or "[table-name].[field-name].[optional-suffix]" into the database; this
 * makes it easier to verify that valid parameter values are being passed into {@link #setParameterValues(Properties)}.
 *
 * @author Daniel Gredler
 * @author Yanming Zhou
 */
public class MyStringType implements UserType<String>, DynamicParameterizedType {

	private String value;

	@Override
	public void setParameterValues(@Nonnull Properties params) {
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
		FieldDetails xproperty = (FieldDetails) params.get( DynamicParameterizedType.XPROPERTY );
		Assert.assertEquals( propertyName, xproperty.getName() );
		Assert.assertEquals( entity, xproperty.getDeclaringType().getName() );
		Assert.assertEquals( String.class.getName(), xproperty.getType().getName() );

		String tableName = propertyName.toUpperCase().split( "_" )[0];
		String columnName = propertyName.toUpperCase().split( "_" )[1];
		ParameterType parameterType = (ParameterType) params.get( DynamicParameterizedType.PARAMETER_TYPE );
		Assert.assertEquals( 1, parameterType.getColumns().length );
		Assert.assertEquals( columnName, parameterType.getColumns()[0] );
		Assert.assertEquals( String.class, parameterType.getReturnedClass() );
		Assert.assertEquals( String.class, parameterType.getReturnedJavaType() );
		Assert.assertEquals( tableName, parameterType.getTable() );

		String value = tableName + "." + columnName;
		if ( params.containsKey( "suffix" ) ) {
			value += "." + params.getProperty( "suffix" ).toUpperCase();
		}

		return value;
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable String value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		st.setString( index, this.value );
	}

	@Override
	@Nullable
	public String nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		return rs.getString( position );
	}

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	@Nonnull
	public Class<String> returnedClass() {
		return String.class;
	}

	@Override
	public boolean equals(@Nullable String x, @Nullable String y) {
		return ( x == null && y == null ) || ( x != null && x.equals( y ) );
	}

	@Override
	public int hashCode(@Nonnull String x) {
		return x.hashCode();
	}

	@Override
	@Nullable
	public String deepCopy(@Nullable String value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable String value) {
		return value;
	}

	@Override
	@Nullable
	public String assemble(@Nullable Serializable cached, @Nullable Object owner) {
		return (String) cached;
	}

	@Override
	@Nullable
	public String replace(@Nullable String original, @Nullable String target, @Nullable Object owner) {
		return original;
	}
}
