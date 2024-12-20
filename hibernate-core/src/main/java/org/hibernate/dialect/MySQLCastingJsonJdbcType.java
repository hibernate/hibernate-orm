/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;

/**
 * @author Christian Beikov
 */
public class MySQLCastingJsonJdbcType extends JsonJdbcType {
	/**
	 * Singleton access
	 */
	public static final JsonJdbcType INSTANCE = new MySQLCastingJsonJdbcType( null );

	public MySQLCastingJsonJdbcType(EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType );
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new MySQLCastingJsonJdbcType( mappingType );
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
			SqlAppender appender,
			Dialect dialect) {
		appender.append( "cast(" );
		appender.append( writeExpression );
		appender.append( " as json)" );
	}
}
