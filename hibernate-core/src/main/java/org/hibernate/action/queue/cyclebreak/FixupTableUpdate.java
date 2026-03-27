/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/// Specialized build for cycle-break fixup updates.
/// Defined as a template that works for both foreign-key and unique-key fixups.
/// Here we build SQL directly as this SQL is extremely simple and there is no
/// need for AST and translation overhead.
///
/// Builds the [SQL][#getSql()] as well as the [JdbcValueDescriptor][#getJdbcValueDescriptorMap] map
/// needed later to properly bind parameter values.
///
///Specialized mutation for FK fixup UPDATE statements.
///
///Generates SQL: UPDATE <table> SET fk1=?, fk2=?, ... WHERE pk1=?, pk2=?, ...
///
///All table/column names are pre-normalized.
///Provides column metadata for binding without relying on entity attribute mappings.
///
///@author Steve Ebersole
public class FixupTableUpdate {
	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final List<ColumnDescriptor> fixupColumnDescriptors;
	private final List<ColumnDescriptor> pkColumnDescriptors;

	private final String sql;
	private final Map<String, JdbcValueDescriptor> jdbcValueDescriptorMap;

	/// Creates the fixup update
	///
	/// @param tableDescriptor The table to update
	/// @param fixupColumnDescriptors The columns to fixup, aka set
	/// @param pkColumnDescriptors The pk columns for the table used for restriction.
	public FixupTableUpdate(
			EntityTableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			List<ColumnDescriptor> fixupColumnDescriptors,
			List<ColumnDescriptor> pkColumnDescriptors) {
		// todo (ActionQueue2) : not sure we need to actually pass in the pk column details here
		//		we can get them from the table descriptor as well
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.fixupColumnDescriptors = fixupColumnDescriptors;
		this.pkColumnDescriptors = pkColumnDescriptors;

		// Generate SQL at construction time
		this.jdbcValueDescriptorMap = new HashMap<>();
		this.sql = buildSql( tableDescriptor, fixupColumnDescriptors, pkColumnDescriptors, jdbcValueDescriptorMap::put );
	}

	public TableDescriptor getTableDescriptor() {
		return tableDescriptor;
	}

	public String getSql() {
		return sql;
	}

	public Map<String, JdbcValueDescriptor> getJdbcValueDescriptorMap() {
		return jdbcValueDescriptorMap;
	}

	public List<ColumnDescriptor> getNormalizedFkColumns() {
		return fixupColumnDescriptors;
	}

	public List<ColumnDescriptor> getNormalizedPkColumns() {
		return pkColumnDescriptors;
	}

	public PreparableMutationOperation buildJdbcUpdate() {
		return new FixupJdbcUpdate(
				sql,
				tableDescriptor,
				entityPersister,
				jdbcValueDescriptorMap
		);
	}

	private static String buildSql(
			TableDescriptor tableDescriptor,
			List<ColumnDescriptor> fixupColumnDescriptors,
			List<ColumnDescriptor> pkColumnDescriptors,
			BiConsumer<String, JdbcValueDescriptor> consumer) {
		final var sql = new StringBuilder( "update " );
		sql.append( tableDescriptor.name() ).append( " set " );

		int parameterIndex = 1;

		// SET clause - FK columns
		boolean first = true;
		for ( var fkCol : fixupColumnDescriptors ) {
			if ( !first ) {
				sql.append( ", " );
			}
			sql.append( fkCol.name() ).append( "=?" );
			consumer.accept( fkCol.name(), fkCol.createValueDescriptor( ParameterUsage.SET, parameterIndex++ ) );
			first = false;
		}

		// WHERE clause - PK columns
		sql.append( " where " );
		first = true;
		for ( var pkCol : pkColumnDescriptors ) {
			if ( !first ) {
				sql.append( " and " );
			}
			sql.append( pkCol.name() ).append( "=?" );
			consumer.accept( pkCol.name(), pkCol.createValueDescriptor( ParameterUsage.RESTRICT, parameterIndex++ ) );
			first = false;
		}

		return sql.toString();
	}
}
