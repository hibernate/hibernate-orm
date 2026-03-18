/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.ast.MutatingTableReference;
import org.hibernate.action.queue.mutation.ast.TableMutation;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.ArrayList;
import java.util.List;

/// Base support for graph-based table mutation builders.
///
/// Works directly with {@link EntityTableDescriptor} instead of wrapping
/// {@link org.hibernate.sql.model.TableMapping} in
/// {@link org.hibernate.sql.model.ast.MutatingTableReference}.
///
/// @author Steve Ebersole
@Incubating
public abstract class AbstractGraphTableMutationBuilder<M extends TableMutation<?>>
		implements GraphTableMutationBuilder<M> {

	protected final SessionFactoryImplementor sessionFactory;
	protected final MutationType mutationType;
	protected final GraphMutationTarget<?> mutationTarget;
	protected final MutatingTableReference tableReference;

	private final List<ColumnValueParameter> parameters;
//	private final List<ColumnValueBinding> valueBindings = new ArrayList<>();

	protected AbstractGraphTableMutationBuilder(
			MutationType mutationType,
			GraphMutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
		this.tableReference = tableReference;
		this.sessionFactory = sessionFactory;

		// Create parameter list directly
		this.parameters = new ArrayList<>();
	}
	protected AbstractGraphTableMutationBuilder(
			MutationType mutationType,
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor sessionFactory) {
		this( mutationType, mutationTarget, new MutatingTableReference( tableDescriptor ),  sessionFactory );
	}

	public TableReference getTableReference() {
		return tableReference;
	}

	@Override
	public TableDescriptor getTableDescriptor() {
		return tableReference.tableDescriptor();
	}

	protected MutationType getMutationType() {
		return mutationType;
	}

	protected GraphMutationTarget<?> getMutationTarget() {
		return mutationTarget;
	}

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	protected JdbcServices getJdbcServices() {
		return sessionFactory.getJdbcServices();
	}

	protected List<ColumnValueParameter> getParameters() {
		return parameters;
	}

	protected void addColumn(
			String columnWriteFragment,
			ColumnDescriptor columnDescriptor,
			List<ColumnValueBinding> list) {
		final ColumnValueBinding valueBinding = createValueBinding( columnWriteFragment, columnDescriptor );
		list.add( valueBinding );
	}

	protected ColumnValueBinding createValueBinding(
			String columnWriteFragment,
			ColumnDescriptor columnDescriptor) {
		return createValueBinding( columnWriteFragment, columnDescriptor, ParameterUsage.SET );
	}

	protected ColumnValueBinding createValueBinding(
			String customWriteExpression,
			ColumnDescriptor columnDescriptor,
			ParameterUsage parameterUsage) {
		return ColumnValueBindingBuilder.createValueBinding(
				customWriteExpression,
				columnDescriptor,
				tableReference,
				parameterUsage,
				this::collectParameters
		);
	}

	protected void addColumn(
			String columnWriteFragment,
			SelectableMapping selectableMapping,
			List<ColumnValueBinding> list) {
		final ColumnValueBinding valueBinding = createValueBinding( columnWriteFragment, selectableMapping );
		list.add( valueBinding );
	}

	protected ColumnValueBinding createValueBinding(
			String columnWriteFragment,
			SelectableMapping selectableMapping) {
		return createValueBinding( columnWriteFragment, selectableMapping, ParameterUsage.SET );
	}

	protected ColumnValueBinding createValueBinding(
			String customWriteExpression,
			SelectableMapping selectableMapping,
			ParameterUsage parameterUsage) {
		return ColumnValueBindingBuilder.createValueBinding(
				customWriteExpression,
				selectableMapping,
				tableReference,
				parameterUsage,
				this::collectParameters
		);
	}

	public void collectParameters(Object parameterRef) {
		if ( parameterRef instanceof List listOfParameters ) {
			parameters.addAll( listOfParameters );
		}
		else {
			parameters.add( (ColumnValueParameter) parameterRef );
		}
	}

	@SafeVarargs
	protected final <T> List<T> combine(List<T> list1, List<T>... additionalLists) {
		final ArrayList<T> combined = list1 == null
			? new ArrayList<>()
			: new ArrayList<>(list1);

		if (additionalLists != null) {
			for (int i = 0; i < additionalLists.length; i++) {
				if (additionalLists[i] != null) {
					combined.addAll(additionalLists[i]);
				}
			}
		}

		return combined;
	}

	@Override
	public String toString() {
		return "GraphTableMutationBuilder(" + mutationType +
			" - '" + tableReference.tableDescriptor().name() + "')";
	}
}
