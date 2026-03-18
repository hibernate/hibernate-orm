/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.ast.MutatingTableReference;
import org.hibernate.action.queue.mutation.ast.TableUpdate;
import org.hibernate.action.queue.mutation.jdbc.JdbcUpdate;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;

import java.util.ArrayList;
import java.util.List;

/// Base support for graph-based UPDATE mutation builders.
///
/// @author Steve Ebersole
@Incubating
public abstract class AbstractGraphTableUpdateBuilder
		extends AbstractGraphRestrictedTableMutationBuilder<JdbcUpdate, TableUpdate>
		implements GraphTableUpdateBuilder {

	protected final List<ColumnValueBinding> valueBindings = new ArrayList<>();

	private String sqlComment;

	protected AbstractGraphTableUpdateBuilder(
			GraphMutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super(MutationType.UPDATE, mutationTarget, tableReference, sessionFactory);
		this.sqlComment = "update for " + mutationTarget.getRolePath();
	}

	protected AbstractGraphTableUpdateBuilder(
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor sessionFactory) {
		super(MutationType.UPDATE, mutationTarget, tableDescriptor, sessionFactory);
		this.sqlComment = "update for " + mutationTarget.getRolePath();
	}

	public String getSqlComment() {
		return sqlComment;
	}

	public void setSqlComment(String sqlComment) {
		this.sqlComment = sqlComment;
	}

	@Override
	public void addValueColumn(ColumnDescriptor columnDescriptor) {
		final var binding = createValueBinding(columnDescriptor.writeFragment(), columnDescriptor, ParameterUsage.SET);
		valueBindings.add(binding);
	}

	@Override
	public void addValueColumn(String valueExpression, ColumnDescriptor columnDescriptor) {
		final var binding = createValueBinding(valueExpression, columnDescriptor, ParameterUsage.SET);
		valueBindings.add(binding);
	}

	@Override
	public void addValueColumn(SelectableMapping selectableMapping) {
		final var binding = createValueBinding(selectableMapping.getWriteExpression(), selectableMapping, ParameterUsage.SET);
		valueBindings.add(binding);
	}

	@Override
	public void addValueColumn(String valueExpression, SelectableMapping selectableMapping) {
		final var binding = createValueBinding(valueExpression, selectableMapping, ParameterUsage.SET);
		valueBindings.add(binding);
	}

	@Override
	public void addValueColumn(ColumnValueBinding binding) {
		valueBindings.add(binding);
	}

	@Override
	public boolean hasValueBindings() {
		return !valueBindings.isEmpty();
	}
}
