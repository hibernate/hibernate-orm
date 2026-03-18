/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.jdbc;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueDescriptorAccess;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;


/// Mutation operation designed for graph-based ActionQueue.
///
/// Unlike {@link org.hibernate.sql.model.MutationOperation}, this uses
/// {@link EntityTableDescriptor} instead of {@link org.hibernate.sql.model.TableMapping},
/// providing pre-normalized names and avoiding exposure of design-time metamodel.
///
/// The {@link EntityTableDescriptor} is built once at SessionFactory initialization
/// and reused across all sessions, eliminating runtime conversion overhead.
///
/// @author Steve Ebersole
@Incubating
public interface JdbcOperation extends JdbcValueDescriptorAccess {
	/// The type of operation (INSERT, UPDATE, DELETE)
	MutationType getMutationType();

	/// The thing being mutated
	GraphMutationTarget<?> getMutationTarget();

	/// Describes the expected outcome.
	Expectation getExpectation();

	/// The table descriptor with pre-normalized names and execution metadata.
	///
	/// Unlike {@link org.hibernate.sql.model.MutationOperation#getTableDetails()},
	/// this returns {@link EntityTableDescriptor} which is built once at SessionFactory
	/// initialization and reused across all sessions.
	TableDescriptor getTableDescriptor();

	/// Find the JDBC parameter for the specified column.
	///
	/// @param columnName The normalized column name
	/// @param usage The parameter usage (SET, RESTRICT, etc.)
	/// @return The descriptor, or null if not found
	JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage);

	/// Get JDBC parameter, throwing exception if not found.
	///
	/// @param columnName The normalized column name
	/// @param usage The parameter usage (SET, RESTRICT, etc.)
	/// @return The descriptor
	/// @throws IllegalArgumentException if not found
	default JdbcValueDescriptor getJdbcValueDescriptor(String columnName, ParameterUsage usage) {
		final JdbcValueDescriptor descriptor = findValueDescriptor(columnName, usage);
		if (descriptor == null) {
			throw new IllegalArgumentException(
				String.format(
					"No parameter found for column: %s (usage: %s) in table: %s",
					columnName,
					usage,
					getTableDescriptor().name()
				)
			);
		}
		return descriptor;
	}

	@Override
	default String resolvePhysicalTableName(String tableName) {
		return this.getTableDescriptor().name();
	}

	@Override
	default JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage) {
		return this.getJdbcValueDescriptor(columnName, usage);
	}
}
