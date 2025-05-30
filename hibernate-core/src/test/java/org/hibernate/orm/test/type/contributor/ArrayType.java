/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.persistence.AttributeConverter;
import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.UserType;

/**
 * @author Vlad Mihalcea
 */
public class ArrayType implements UserType<Array>, AttributeConverter<Array, String> {

	public static final ArrayType INSTANCE = new ArrayType();

	private final BasicJavaType<Array> javaType = ArrayJavaType.INSTANCE;
	private final JdbcType jdbcType = VarcharJdbcType.INSTANCE;

	@Override
	public int getSqlType() {
		return jdbcType.getJdbcTypeCode();
	}

	@Override
	public Class<Array> returnedClass() {
		return Array.class;
	}

	@Override
	public boolean equals(Array x, Array y) throws HibernateException {
		return javaType.areEqual( x, y );
	}

	@Override
	public int hashCode(Array x) throws HibernateException {
		return javaType.extractHashCode( x );
	}

	@Override
	public Array nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		return jdbcType.getExtractor( javaType ).extract( rs, position, options );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Array value, int index, WrapperOptions options)
			throws SQLException {
		jdbcType.getBinder( javaType ).bind( st, value, index, options );
	}

	@Override
	public AttributeConverter<Array, String> getValueConverter() {
		return this;
	}

	@Override
	public String convertToDatabaseColumn(Array domainForm) {
		return (String) disassemble( domainForm );
	}

	@Override
	public Array convertToEntityAttribute(String relationalForm) {
		return assemble( relationalForm, null );
	}

	@Override
	public Array deepCopy(Array value) throws HibernateException {
		return ArrayMutabilityPlan.INSTANCE.deepCopy( value );
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(Array value) throws HibernateException {
		return ArrayMutabilityPlan.INSTANCE.disassemble( value, null );
	}

	@Override
	public Array assemble(Serializable cached, Object owner) throws HibernateException {
		return ArrayMutabilityPlan.INSTANCE.assemble( cached, null );
	}

	@Override
	public Array replace(Array original, Array target, Object owner) throws HibernateException {
		return null;
	}
}
