/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Gavin King
 */
public class CastTarget implements Expression, SqlAstNode, SqlTypedMapping {
	private final JdbcMapping type;
	private final @Nullable String sqlType;
	private final @Nullable Long length;
	private final @Nullable Integer arrayLength;
	private final @Nullable Integer precision;
	private final @Nullable Integer scale;

	public CastTarget(JdbcMapping type) {
		this( type, null, null, null, null, null );
	}

	public CastTarget(JdbcMapping type, @Nullable Long length, @Nullable Integer precision, @Nullable Integer scale) {
		this( type, null, length, precision, scale );
	}

	public CastTarget(JdbcMapping type, @Nullable Long length, @Nullable Integer arrayLength, @Nullable Integer precision, @Nullable Integer scale) {
		this( type, null, length, arrayLength, precision, scale );
	}

	public CastTarget(JdbcMapping type, @Nullable String sqlType, @Nullable Long length, @Nullable Integer precision, @Nullable Integer scale) {
		this( type, sqlType, length, null, precision, scale );
	}

	public CastTarget(JdbcMapping type, @Nullable String sqlType, @Nullable Long length, @Nullable Integer arrayLength, @Nullable Integer precision, @Nullable Integer scale) {
		this.type = type;
		this.sqlType = sqlType;
		this.length = length;
		this.arrayLength = arrayLength;
		this.precision = precision;
		this.scale = scale;
	}

	public @Nullable String getSqlType() {
		return sqlType;
	}

	@Override
	public @Nullable String getColumnDefinition() {
		return sqlType;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return type;
	}

	@Override
	public @Nullable Long getLength() {
		return length;
	}

	@Override
	public @Nullable Integer getArrayLength() {
		return arrayLength;
	}

	@Override
	public @Nullable Integer getPrecision() {
		return precision;
	}

	@Override
	public @Nullable Integer getTemporalPrecision() {
		return null;
	}

	@Override
	public @Nullable Integer getScale() {
		return scale;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCastTarget( this );
	}

}
