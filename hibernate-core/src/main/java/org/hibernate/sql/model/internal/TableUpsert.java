/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.AbstractRestrictedTableMutation;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.TableUpdate;

/**
 * @apiNote Implements {@link TableUpdate} because it is fundamentally an update
 *
 * @author Steve Ebersole
 */
public class TableUpsert
		extends AbstractRestrictedTableMutation<MutationOperation>
		implements RestrictedTableMutation<MutationOperation> {
	private final List<ColumnValueBinding> valueBindings;

	public TableUpsert(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		this(
				mutatingTable,
				mutationTarget,
				"upsert for " + mutationTarget.getRolePath(),
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings,
				parameters
		);
	}

	public TableUpsert(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			String comment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super( mutatingTable, mutationTarget, comment, keyRestrictionBindings, optLockRestrictionBindings, parameters );
		this.valueBindings = valueBindings;
	}

	@Override
	public EntityMutationTarget getMutationTarget() {
		return (EntityMutationTarget) super.getMutationTarget();
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	@Override
	public Expectation getExpectation() {
		return getMutatingTable().getTableMapping().getUpdateDetails().getExpectation();
	}

	public List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}

	public void forEachValueBinding(BiConsumer<Integer, ColumnValueBinding> consumer) {
		forEachThing( valueBindings, consumer );
	}

	@Override
	public void accept(SqlAstWalker walker) {
		throw new UnsupportedOperationException();
	}

	public MutationOperation createMutationOperation(
			ValuesAnalysis valuesAnalysis,
			SessionFactoryImplementor factory) {
		return factory.getJdbcServices().getDialect().createUpsertOperation(
				getMutationTarget(),
				this,
				factory
		);
	}

	@Override
	protected MutationOperation createMutationOperation(
			TableMapping tableDetails,
			String updateSql,
			List<JdbcParameterBinder> effectiveBinders) {
		throw new UnsupportedOperationException();
	}
}
