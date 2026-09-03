/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model.builder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SPI;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameterList;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableMutation;

import static org.hibernate.SPI.Role.IMPLEMENT;

/// Base for provider-owned [TableMutationBuilder] implementations.
///
/// Extend this class to collect JDBC parameters and column bindings for one
/// table mutation. Choose the constructor accepting a [TableMapping] when the
/// builder should create the mutating table reference, or supply an existing
/// [MutatingTableReference] when reference identity must be preserved.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(IMPLEMENT)
public abstract class AbstractTableMutationBuilder<M extends TableMutation<?>> implements TableMutationBuilder<M> {
	private final SessionFactoryImplementor sessionFactory;

	private final MutationType mutationType;
	private final MutationTarget mutationTarget;

	private final MutatingTableReference mutatingTable;
	private final ColumnValueParameterList parameters;

	/// Create a builder which owns a new reference to `table`.
	@SPI(IMPLEMENT)
	public AbstractTableMutationBuilder(
			MutationType mutationType,
			MutationTarget mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		this( mutationType, mutationTarget, new MutatingTableReference( table ), sessionFactory );
	}

	/// Create a builder using the supplied mutating-table reference.
	@SPI(IMPLEMENT)
	public AbstractTableMutationBuilder(
			MutationType mutationType,
			MutationTarget mutationTarget,
			MutatingTableReference mutatingTable,
			SessionFactoryImplementor sessionFactory) {
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
		this.sessionFactory = sessionFactory;

		this.mutatingTable = mutatingTable;
		this.parameters = new ColumnValueParameterList( mutatingTable, null, 0 );
	}

	protected MutationTarget getMutationTarget() {
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
