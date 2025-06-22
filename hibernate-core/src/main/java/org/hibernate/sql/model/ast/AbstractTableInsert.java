/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.jdbc.JdbcInsertMutation;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableInsert extends AbstractTableMutation<JdbcInsertMutation> implements TableInsert {
	private final List<ColumnValueBinding> valueBindings;

	public AbstractTableInsert(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			List<ColumnValueParameter> parameters,
			List<ColumnValueBinding> valueBindings) {
		this(
				mutatingTable,
				mutationTarget,
				"insert for " + mutationTarget.getRolePath(),
				parameters,
				valueBindings
		);
	}

	public AbstractTableInsert(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			String comment,
			List<ColumnValueParameter> parameters,
			List<ColumnValueBinding> valueBindings) {
		super( mutatingTable, mutationTarget, comment, parameters );
		this.valueBindings = valueBindings;
	}

	@Override
	protected String getLoggableName() {
		return "TableInsert";
	}

	@Override
	public Expectation getExpectation() {
		return getMutatingTable().getTableMapping().getInsertDetails().getExpectation();
	}

	@Override
	public List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}

	@Override
	public void forEachValueBinding(BiConsumer<Integer, ColumnValueBinding> consumer) {
		forEachThing( valueBindings, consumer );
	}

	@Override
	protected JdbcInsertMutation createMutationOperation(
			TableMapping tableDetails,
			String sql,
			List<JdbcParameterBinder> effectiveBinders) {
		return new JdbcInsertMutation(
				tableDetails,
				getMutationTarget(),
				sql,
				isCallable(),
				getExpectation(),
				effectiveBinders
		);
	}
}
