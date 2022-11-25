/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.jdbc.JdbcUpdateMutation;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableUpdate<O extends MutationOperation>
		extends AbstractRestrictedTableMutation<O>
		implements TableUpdate<O> {
	private final List<ColumnValueBinding> valueBindings;

	public AbstractTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			List<ColumnValueParameter> parameters,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		this(
				mutatingTable,
				mutationTarget,
				"update for " + mutationTarget.getRolePath(),
				parameters,
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings
		);
	}

	public AbstractTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueParameter> parameters,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		super( mutatingTable, mutationTarget, sqlComment, keyRestrictionBindings, optLockRestrictionBindings, parameters );
		this.valueBindings = valueBindings;
	}

	@Override
	public Expectation getExpectation() {
		return getMutatingTable().getTableMapping().getUpdateDetails().getExpectation();
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
	protected O createMutationOperation(TableMapping tableDetails, String sql, List<JdbcParameterBinder> effectiveBinders) {
		//noinspection unchecked
		return (O) new JdbcUpdateMutation(
				tableDetails,
				getMutationTarget(),
				sql,
				isCallable(),
				getExpectation(),
				effectiveBinders
		);
	}
}
