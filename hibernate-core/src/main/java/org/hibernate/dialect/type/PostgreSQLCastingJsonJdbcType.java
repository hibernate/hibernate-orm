/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;

/**
 * @author Christian Beikov
 */
public class PostgreSQLCastingJsonJdbcType extends JsonJdbcType {

	public static final PostgreSQLCastingJsonJdbcType JSON_INSTANCE = new PostgreSQLCastingJsonJdbcType( false, null );
	public static final PostgreSQLCastingJsonJdbcType JSONB_INSTANCE = new PostgreSQLCastingJsonJdbcType( true, null );

	private final boolean jsonb;

	public PostgreSQLCastingJsonJdbcType(boolean jsonb, EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType );
		this.jsonb = jsonb;
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.JSON;
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new PostgreSQLCastingJsonJdbcType( jsonb, mappingType );
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
			@Nullable Size size,
			SqlAppender appender,
			Dialect dialect) {
		appender.append( "cast(" );
		appender.append( writeExpression );
		appender.append( " as " );
		if ( jsonb ) {
			appender.append( "jsonb)" );
		}
		else {
			appender.append( "json)" );
		}
	}

	@Override
	public boolean isWriteExpressionTyped(Dialect dialect) {
		return true;
	}
}
