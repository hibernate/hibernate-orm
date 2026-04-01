/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/// Provides access to details needed while binding
/// @author Steve Ebersole
public interface JdbcValueDescriptorAccess {
	/// Locate type details about
	JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage);

	/// @deprecated Used by the mutation handling from the legacy action queue.  It is not needed for
	/// the graph-based queue.
	@Deprecated(since = "8.0", forRemoval = true)
	default String resolvePhysicalTableName(String tableName) {
		return tableName;
	}
}
