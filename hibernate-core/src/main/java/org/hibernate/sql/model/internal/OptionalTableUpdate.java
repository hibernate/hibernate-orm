/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.internal;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.InsertCoordinatorStandard;
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
import org.hibernate.sql.model.jdbc.JdbcUpdateMutation;
import org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation;

import static org.hibernate.sql.model.ast.AbstractTableUpdate.collectParameters;

/**
 * @apiNote Implements {@link TableUpdate} because it is fundamentally an update
 *
 * @author Steve Ebersole
 */
public class OptionalTableUpdate
		extends AbstractRestrictedTableMutation<MutationOperation>
		implements RestrictedTableMutation<MutationOperation> {
	private final List<ColumnValueBinding> valueBindings;

	public OptionalTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		this(
				mutatingTable,
				mutationTarget,
				"upsert for " + mutationTarget.getRolePath(),
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings
		);
	}

	public OptionalTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			String comment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		super(
				mutatingTable,
				mutationTarget,
				comment,
				keyRestrictionBindings,
				optLockRestrictionBindings,
				collectParameters( valueBindings, keyRestrictionBindings, optLockRestrictionBindings )
		);
		this.valueBindings = valueBindings;
	}

	@Override
	protected String getLoggableName() {
		return "OptionalTableUpdate";
	}

	@Override
	public boolean isCustomSql() {
		return false;
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

	@Override
	public void forEachParameter(Consumer<ColumnValueParameter> consumer) {
		final BiConsumer<Integer,ColumnValueBinding> intermediateConsumer = (index, binding) -> {
			for ( ColumnValueParameter parameter : binding.getValueExpression().getParameters() ) {
				consumer.accept( parameter );
			}
		};

		forEachThing( getValueBindings(), intermediateConsumer );
		forEachThing( getKeyBindings(), intermediateConsumer );
		forEachThing( getOptimisticLockBindings(), intermediateConsumer );
	}

	public List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}

	@Override
	public void forEachValueBinding(BiConsumer<Integer, ColumnValueBinding> consumer) {
		forEachThing( valueBindings, consumer );
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitOptionalTableUpdate( this );
	}

	@Override
	public MutationOperation createMutationOperation(ValuesAnalysis valuesAnalysis, SessionFactoryImplementor factory) {
		final TableMapping tableMapping = getMutatingTable().getTableMapping();
		assert ! (valuesAnalysis instanceof InsertCoordinatorStandard.InsertValuesAnalysis );
		if ( tableMapping.getInsertDetails().getCustomSql() != null
				|| tableMapping.getInsertDetails().isDynamicMutation()
				|| tableMapping.getDeleteDetails().getCustomSql() != null
				|| tableMapping.getUpdateDetails().getCustomSql() != null
				|| tableMapping.getUpdateDetails().isDynamicMutation() ) {
			// Fallback to the optional table mutation operation because we have to execute user specified SQL
			// and with dynamic update insert we have to avoid using merge for optional table because
			// it can cause the deletion of the row when an attribute is set to null
			return new OptionalTableUpdateOperation( getMutationTarget(), this, factory );
		}
		return factory.getJdbcServices().getDialect().createOptionalTableUpdateOperation(
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
		return new JdbcUpdateMutation(
				tableDetails,
				getMutationTarget(),
				updateSql,
				isCallable(),
				getExpectation(),
				effectiveBinders
		);
	}
}
