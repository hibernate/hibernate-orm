/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import java.util.function.Consumer;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/// Provides access to details needed while binding
/// @author Steve Ebersole
public interface JdbcValueDescriptorAccess {
	/// Locate type details about
	JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage);

	/// Visit all the JDBC values used for the given column and parameter usage.
	///
	/// A SQL AST translator may render an expression more than once while
	/// emulating a predicate, so a single logical value can correspond to
	/// multiple JDBC parameters.
	default int forEachValueDescriptor(
			String tableName,
			String columnName,
			ParameterUsage usage,
			Consumer<JdbcValueDescriptor> consumer) {
		final var descriptor = resolveValueDescriptor( tableName, columnName, usage );
		if ( descriptor != null ) {
			consumer.accept( descriptor );
			return 1;
		}
		return 0;
	}

	/// @deprecated Used by the mutation handling from the legacy action queue.  It is not needed for
	/// the graph-based queue.
	@Deprecated(since = "8.0", forRemoval = true)
	default String resolvePhysicalTableName(String tableName) {
		return tableName;
	}
}
