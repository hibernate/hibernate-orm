/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Emmanuel Bernard
 */
public class BasicTypeImpl<J> implements BasicDomainType<J>, SqmDomainType<J>, JdbcMapping, Serializable {
	private final JavaType<J> javaType;
	private final JdbcType jdbcType;

	public BasicTypeImpl(JavaType<J> javaType, JdbcType jdbcType) {
		this.javaType = javaType;
		this.jdbcType = jdbcType;
	}

	@Override
	public Class<J> getJavaType() {
		return BasicDomainType.super.getJavaType();
	}

	@Override
	public String getTypeName() {
		return javaType.getTypeName();
	}

	@Override
	public SqmDomainType<J> getSqmType() {
		return this;
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return javaType;
	}

	@Override
	public boolean canDoExtraction() {
		return true;
	}

	@Override
	public JdbcType getJdbcType() {
		return jdbcType;
	}

	@Override
	public J extract(
			CallableStatement statement,
			int paramIndex,
			SharedSessionContractImplementor session) throws SQLException {
		return jdbcType.getExtractor( javaType ).extract( statement, paramIndex, session );
	}

	@Override
	public J extract(
			CallableStatement statement,
			String paramName,
			SharedSessionContractImplementor session) throws SQLException {
		return jdbcType.getExtractor( javaType ).extract( statement, paramName, session );
	}

	@Override
	public JavaType<?> getJavaTypeDescriptor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValueExtractor<J> getJdbcValueExtractor() {
		return jdbcType.getExtractor( javaType );
	}

	@Override
	public ValueBinder<J> getJdbcValueBinder() {
		return jdbcType.getBinder( javaType );
	}
}
