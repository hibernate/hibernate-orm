/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.SQLException;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for aggregate handling like {@link SqlTypes#STRUCT STRUCT}, {@link SqlTypes#JSON JSON} and {@link SqlTypes#SQLXML SQLXML}.
 */
public interface AggregateJdbcType extends JdbcType {

	AggregateJdbcType resolveAggregateJdbcType(EmbeddableMappingType mappingType, String sqlType);

	EmbeddableMappingType getEmbeddableMappingType();

	Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException;

	Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException;
}
