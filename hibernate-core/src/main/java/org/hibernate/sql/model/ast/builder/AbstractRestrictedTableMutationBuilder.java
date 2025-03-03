/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueBindingList;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;

/**
 * Specialization of TableMutationBuilder for mutations which contain a
 * restriction.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractRestrictedTableMutationBuilder<O extends MutationOperation, M extends RestrictedTableMutation<O>>
		extends AbstractTableMutationBuilder<M>
		implements RestrictedTableMutationBuilder<O, M> {

	private final ColumnValueBindingList keyRestrictionBindings;
	private final ColumnValueBindingList optimisticLockBindings;

	public AbstractRestrictedTableMutationBuilder(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		super( mutationType, mutationTarget, table, sessionFactory );
		this.keyRestrictionBindings = new ColumnValueBindingList( getMutatingTable(), getParameters(), ParameterUsage.RESTRICT );
		this.optimisticLockBindings = new ColumnValueBindingList( getMutatingTable(), getParameters(), ParameterUsage.RESTRICT );
	}

	public AbstractRestrictedTableMutationBuilder(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( mutationType, mutationTarget, tableReference, sessionFactory );
		this.keyRestrictionBindings = new ColumnValueBindingList( getMutatingTable(), getParameters(), ParameterUsage.RESTRICT );
		this.optimisticLockBindings = new ColumnValueBindingList( getMutatingTable(), getParameters(), ParameterUsage.RESTRICT );
	}

	@Override
	public ColumnValueBindingList getKeyRestrictionBindings() {
		return keyRestrictionBindings;
	}

	@Override
	public ColumnValueBindingList getOptimisticLockBindings() {
		return optimisticLockBindings;
	}

	@Override
	public void addNonKeyRestriction(ColumnValueBinding valueBinding) {
		optimisticLockBindings.addRestriction( valueBinding );
	}

	@Override
	public void addKeyRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
		keyRestrictionBindings.addRestriction( columnName, columnWriteFragment, jdbcMapping );
	}

	@Override
	public void addNullOptimisticLockRestriction(SelectableMapping column) {
		optimisticLockBindings.addNullRestriction( column );
	}

	@Override
	public void addOptimisticLockRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
		optimisticLockBindings.addRestriction( columnName, columnWriteFragment, jdbcMapping );
	}

	@Override
	public void addLiteralRestriction(String columnName, String sqlLiteralText, JdbcMapping jdbcMapping) {
		keyRestrictionBindings.addRestriction( columnName, sqlLiteralText, jdbcMapping );
	}

	@Override
	public void setWhere(String fragment) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addWhereFragment(String fragment) {
		throw new UnsupportedOperationException();
	}
}
