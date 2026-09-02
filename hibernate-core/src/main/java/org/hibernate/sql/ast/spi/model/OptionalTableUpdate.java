/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.InsertCoordinatorStandard;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;
import org.hibernate.sql.spi.mutation.jdbc.JdbcUpdateMutation;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;

import static org.hibernate.sql.ast.spi.model.AbstractTableUpdate.collectParameters;

/**
 * @author Steve Ebersole
 */
public class OptionalTableUpdate
		extends AbstractRestrictedTableMutation<MutationOperation>
		implements LogicalTableUpdate<MutationOperation> {
	private final List<ColumnValueBinding> valueBindings;
	private final boolean versionedTarget;

	public OptionalTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			boolean versionedTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		this(
				mutatingTable,
				mutationTarget,
				"upsert for " + mutationTarget.getRolePath(),
				versionedTarget,
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings
		);
	}

	public OptionalTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String comment,
			boolean versionedTarget,
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
		this.versionedTarget = versionedTarget;
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
			return new OptionalTableUpdateOperation( getMutationTarget(), this );
		}
		return factory.getJdbcServices().getDialect().createOptionalTableUpdateOperation(
				new OptionalTableUpdateOperationRequest( this, factory, versionedTarget )
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
