/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// An aggregate [JdbcType] represented by a named SQL structured type.
///
/// Mapping-specific instances are supplied by
/// [AggregateJdbcType#resolveAggregateJdbcType(org.hibernate.metamodel.mapping.EmbeddableMappingType, String, org.hibernate.metamodel.spi.RuntimeModelCreationContext)].
///
/// @see AggregateJdbcType#resolveAggregateJdbcType(org.hibernate.metamodel.mapping.EmbeddableMappingType, String, org.hibernate.metamodel.spi.RuntimeModelCreationContext)
@Incubating
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface StructuredJdbcType extends AggregateJdbcType, SqlTypedJdbcType {

	String getStructTypeName();

	@Override
	default String getSqlTypeName() {
		return getStructTypeName();
	}
}
