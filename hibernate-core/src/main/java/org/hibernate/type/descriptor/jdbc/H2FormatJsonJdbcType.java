/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.spi.SqlAppender;

/**
 * Specialized type mapping for {@code JSON} that utilizes the custom
 * '{@code ? format json}' write expression for H2.
 *
 * @author Marco Belladelli
 * @deprecated Use {@link org.hibernate.dialect.type.H2JsonJdbcType} instead
 */
@Deprecated(forRemoval = true, since = "6.5")
public class H2FormatJsonJdbcType extends JsonJdbcType {
	/**
	 * Singleton access
	 */
	public static final H2FormatJsonJdbcType INSTANCE = new H2FormatJsonJdbcType( null );

	protected H2FormatJsonJdbcType(EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType );
	}

	@Override
	public String toString() {
		return "FormatJsonJdbcType";
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new H2FormatJsonJdbcType( mappingType );
	}

	@Override
	public void appendWriteExpression(String writeExpression, SqlAppender appender, Dialect dialect) {
		appender.append( writeExpression );
		appender.append( " format json" );
	}
}
