/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.SQLException;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for aggregate handling like {@link SqlTypes#STRUCT STRUCT}, {@link SqlTypes#JSON JSON} and {@link SqlTypes#SQLXML SQLXML}.
 */
public interface AggregateJdbcType extends JdbcType {

	AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext);

	EmbeddableMappingType getEmbeddableMappingType();

	Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException;

	Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException;
}
