/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.internal;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.AbstractRestrictedTableMutation;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableUpdate;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import static java.util.Collections.emptyList;

/**
 * A skipped update
 *
 * @author Steve Ebersole
 */
public class TableUpdateNoSet
		extends AbstractRestrictedTableMutation<MutationOperation>
		implements TableUpdate<MutationOperation> {
	public TableUpdateNoSet(MutatingTableReference mutatingTable, MutationTarget<?> mutationTarget) {
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
