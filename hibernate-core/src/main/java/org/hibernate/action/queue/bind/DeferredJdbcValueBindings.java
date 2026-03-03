/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.JdbcValueBindingsImpl;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueBindingsImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.sql.model.TableMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.action.queue.Helper.normalizeTableName;

/// Specialized [JdbcValueBindings][JdbcValueBindingsImplementor] implementation
/// allowing for registration of deferred value bindings.
///
/// @author Steve Ebersole
public final class DeferredJdbcValueBindings implements JdbcValueBindingsImplementor {
	private final JdbcValueBindingsImpl delegate;

	// table -> deferred binds for that table
	private final Map<String, List<DeferredBinding>> deferredByTable = new HashMap<>();

	public DeferredJdbcValueBindings(JdbcValueBindingsImpl delegate) {
		this.delegate = delegate;
	}

	public JdbcValueBindingsImpl getDelegate() {
		return delegate;
	}

	public Map<String, List<DeferredBinding>> getDeferredByTable() {
		return deferredByTable;
	}

	public void bindDeferred(MutableObject<?> handle, String tableName, String columnName, ParameterUsage usage) {
		deferredByTable.computeIfAbsent(normalizeTableName(tableName), k -> new ArrayList<>())
				.add(new DeferredBinding(handle, columnName, usage));
	}

	@Override
	public void beforeStatement(PreparedStatementDetails statementDetails) {
		final TableMapping table = statementDetails.getMutatingTableDetails();
		final String tableName = normalizeTableName(table.getTableName());

		final List<DeferredBinding> list = deferredByTable.get(tableName);
		if (list != null) {
			for (DeferredBinding b : list) {
				delegate.bindValue(b.handle.get(), tableName, b.columnName, b.usage);
			}
		}

		delegate.beforeStatement(statementDetails);
	}

	@Override
	public void afterStatement(TableMapping mutatingTable) {
		delegate.afterStatement(mutatingTable);
	}

	@Override
	public void bindValue(Object value, String tableName, String columnName, ParameterUsage usage) {
		delegate.bindValue(value, tableName, columnName, usage);
	}

	@Override
	public BindingGroup getBindingGroup(String tableName) {
		return delegate.getBindingGroup(tableName);
	}

	@Override
	public Object getBoundValue(String tableName, String columnName, ParameterUsage usage) {
		return null;
	}

	@Override
	public void replaceValue(String tableName, String columnName, ParameterUsage usage, Object newValue) {

	}

	public record DeferredBinding(MutableObject<?> handle, String columnName, ParameterUsage usage) {}
}
