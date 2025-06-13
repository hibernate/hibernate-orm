/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;

/**
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLCastingJsonJdbcType.
 */
public class GaussDBCastingJsonJdbcType extends JsonJdbcType {

	public static final GaussDBCastingJsonJdbcType JSON_INSTANCE = new GaussDBCastingJsonJdbcType( false, null );
	public static final GaussDBCastingJsonJdbcType JSONB_INSTANCE = new GaussDBCastingJsonJdbcType( true, null );

	private final boolean jsonb;

	public GaussDBCastingJsonJdbcType(boolean jsonb, EmbeddableMappingType embeddableMappingType) {
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
		return new GaussDBCastingJsonJdbcType( jsonb, mappingType );
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
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
}
