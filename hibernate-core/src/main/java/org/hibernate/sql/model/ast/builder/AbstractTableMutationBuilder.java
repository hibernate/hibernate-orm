/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameterList;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableMutation;

/**
 * Base support for TableMutationBuilder implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableMutationBuilder<M extends TableMutation<?>> implements TableMutationBuilder<M> {
	private final SessionFactoryImplementor sessionFactory;

	private final MutationType mutationType;
	private final MutationTarget<?> mutationTarget;

	private final MutatingTableReference mutatingTable;
	private final ColumnValueParameterList parameters;

	public AbstractTableMutationBuilder(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		this( mutationType, mutationTarget, new MutatingTableReference( table ), sessionFactory );
	}

	public AbstractTableMutationBuilder(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			MutatingTableReference mutatingTable,
			SessionFactoryImplementor sessionFactory) {
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
		this.sessionFactory = sessionFactory;

		this.mutatingTable = mutatingTable;
		this.parameters = new ColumnValueParameterList( mutatingTable, null, 0 );
	}

	protected MutationTarget<?> getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public MutatingTableReference getMutatingTable() {
		return mutatingTable;
	}

	protected ColumnValueParameterList getParameters() {
		return parameters;
	}

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	protected JdbcServices getJdbcServices() {
		return sessionFactory.getJdbcServices();
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
				getMutatingTable(),
				parameterUsage,
				parameters::apply
		);
	}

	@SafeVarargs
	protected final <T> List<T> combine(List<T> list1, List<T>... additionalLists) {
		final ArrayList<T> combined = list1 == null
				? new ArrayList<>()
				: new ArrayList<>( list1 );

		if ( additionalLists != null ) {
			for ( int i = 0; i < additionalLists.length; i++ ) {
				if ( additionalLists[i] == null ) {
					continue;
				}
				combined.addAll( additionalLists[i] );
			}
		}

		return combined;
	}

	@Override
	public String toString() {
		return "TableMutationBuilder( " + mutationType + " - '" + mutatingTable.getTableName() + "')";
	}
}
