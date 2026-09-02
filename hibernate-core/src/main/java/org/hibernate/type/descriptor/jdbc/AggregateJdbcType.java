/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.SQLException;

import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.WrapperOptions;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Describes aggregate handling such as [org.hibernate.type.SqlTypes#STRUCT],
/// [org.hibernate.type.SqlTypes#JSON], and
/// [org.hibernate.type.SqlTypes#SQLXML].
///
/// A prototype descriptor supplies its mapping-specific descriptor through
/// [#resolveAggregateJdbcType(EmbeddableMappingType, String, RuntimeModelCreationContext)].
///
/// @see #resolveAggregateJdbcType(EmbeddableMappingType, String, RuntimeModelCreationContext)
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface AggregateJdbcType extends JdbcType {

	/// Resolve and supply a descriptor bound to the aggregate mapping and SQL
	/// type name.
	///
	/// @see AggregateJdbcType
	/// @see StructuredJdbcType
	@SPI(SUPPLY)
	AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext);

	EmbeddableMappingType getEmbeddableMappingType();

	Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException;

	Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException;
}
