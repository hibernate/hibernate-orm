/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.cyclebreak;


import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

import java.util.List;
import java.util.Map;

///Custom JDBC UPDATE mutation for FK fixup operations.
///
///This bypasses the standard mutation builders and provides value descriptors
///using normalized column names and JDBC mappings extracted from SelectableMapping objects.
///
///Compatible with bytecode-enhanced entities since it doesn't rely on attribute traversal.
///
///@author Steve Ebersole
public class FixupJdbcUpdate implements PreparableMutationOperation {
	private final String sql;
	private final EntityPersister entityPersister;
	private final TableDescriptor tableDescriptor;
	private final Expectation expectation;

	private final List<JdbcParameterBinder> parameterBinders;

	private final Map<String, JdbcValueDescriptor> jdbcValueDescriptorMap;

	public FixupJdbcUpdate(
			String sql,
			TableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			Map<String, JdbcValueDescriptor> jdbcValueDescriptorMap) {
		this.sql = sql;
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.expectation = Expectation.RowCount.INSTANCE;

		this.jdbcValueDescriptorMap = jdbcValueDescriptorMap;
		this.parameterBinders = CollectionHelper.arrayList( jdbcValueDescriptorMap.size() );
		jdbcValueDescriptorMap.forEach( (column, jdbcValueDescriptor ) -> {
			parameterBinders.add( new JdbcParameterImpl( jdbcValueDescriptor.getJdbcMapping() ) );
	} );
	}

	@Override
	public String getSqlString() {
		return sql;
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.UPDATE;
	}

	@Override
	public MutationTarget<?,?> getMutationTarget() {
		return entityPersister;
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	@Override
	public Expectation getExpectation() {
		return expectation;
	}

	@Override
	public JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage) {
		return jdbcValueDescriptorMap.get( columnName );
	}

	@Override
	public List<JdbcParameterBinder> getParameterBinders() {
		return parameterBinders;
	}

	@Override
	public TableMapping getTableDetails() {
		// Adapt TableDescriptor to TableMapping
		final boolean isIdentifierTable = tableDescriptor instanceof EntityTableDescriptor etd
				&& etd.isIdentifierTable();
		return new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition
				isIdentifierTable,
				false // isInverse
		);
	}

	@Override
	public String toString() {
		return "FixupJdbcUpdate(" + tableDescriptor.name() + ")";
	}

}
