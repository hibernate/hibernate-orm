/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.internal;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.AbstractRestrictedTableMutation;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableUpdate;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;

import static java.util.Collections.emptyList;

/**
 * A skipped update
 *
 * @author Steve Ebersole
 */
public class TableUpdateNoSet
		extends AbstractRestrictedTableMutation<MutationOperation>
		implements TableUpdate<MutationOperation> {
	public TableUpdateNoSet(MutatingTableReference mutatingTable, MutationTarget mutationTarget) {
		super(
				mutatingTable,
				mutationTarget,
				"no-op",
				emptyList(),
				emptyList(),
				emptyList()
		);
	}

	@Override
	protected String getLoggableName() {
		return "TableUpdateNoSet";
	}

	@Override
	public boolean isCustomSql() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker walker) {
	}

	@Override
	public void forEachValueBinding(BiConsumer<Integer, ColumnValueBinding> consumer) {
		// there are none
	}

	@Override
	protected JdbcMutationOperation createMutationOperation(
			TableMapping tableDetails,
			String sql,
			List<JdbcParameterBinder> effectiveBinders) {
		// no operation
		return null;
	}

	@Override
	public Expectation getExpectation() {
		return Expectation.None.INSTANCE;
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	@Override
	public List<ColumnValueBinding> getValueBindings() {
		return emptyList();
	}

	@Override
	public void forEachParameter(Consumer<ColumnValueParameter> consumer) {
	}

	@Override
	public List<ColumnReference> getReturningColumns() {
		return emptyList();
	}

	@Override
	public void forEachReturningColumn(BiConsumer<Integer, ColumnReference> consumer) {
		// nothing to do
	}
}
