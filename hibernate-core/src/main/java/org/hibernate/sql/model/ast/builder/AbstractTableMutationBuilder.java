/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast.builder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.ColumnValueParameterList;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

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
	}

	protected MutationTarget<?> getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public MutatingTableReference getMutatingTable() {
		return mutatingTable;
	}

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	protected JdbcServices getJdbcServices() {
		return sessionFactory.getJdbcServices();
	}

	protected void addColumn(
			String columnName,
			String columnWriteFragment,
			JdbcMapping jdbcMapping,
			boolean isNullable,
			List<ColumnValueBinding> list) {
		final ColumnValueBinding valueBinding = createValueBinding( columnName, columnWriteFragment, jdbcMapping, isNullable );
		list.add( valueBinding );
	}

	protected void addColumn(
			String columnName,
			String columnWriteFragment,
			JdbcMapping jdbcMapping,
			ParameterUsage parameterUsage,
			boolean isNullable,
			List<ColumnValueBinding> list) {
		final ColumnValueBinding valueBinding = createValueBinding( columnName, columnWriteFragment, jdbcMapping, parameterUsage, isNullable );
		list.add( valueBinding );
	}

	protected ColumnValueBinding createValueBinding(
			String columnName,
			String columnWriteFragment,
			JdbcMapping jdbcMapping,
			boolean isNullable) {
		return createValueBinding( columnName, columnWriteFragment, jdbcMapping, ParameterUsage.SET, isNullable );
	}

	protected ColumnValueBinding createValueBinding(
			String columnName,
			String customWriteExpression,
			JdbcMapping jdbcMapping,
			ParameterUsage parameterUsage,
			boolean isNullable) {
		final ColumnReference columnReference = new ColumnReference( mutatingTable, columnName, jdbcMapping );
		final ColumnWriteFragment columnWriteFragment;
		if ( customWriteExpression.contains( "?" ) ) {
			final JdbcType jdbcType = jdbcMapping.getJdbcType();
			final EmbeddableMappingType aggregateMappingType = jdbcType instanceof AggregateJdbcType
					? ( (AggregateJdbcType) jdbcType ).getEmbeddableMappingType()
					: null;
			if ( aggregateMappingType != null && !aggregateMappingType.shouldBindAggregateMapping() ) {
				final ColumnValueParameterList parameters = new ColumnValueParameterList(
						getMutatingTable(),
						parameterUsage,
						aggregateMappingType.getJdbcTypeCount()
				);
				aggregateMappingType.forEachSelectable( parameters );
				for ( int i = 0; i < parameters.size(); i++ ) {
					handleParameterCreation( parameters.get( i ) );
				}

				columnWriteFragment = new ColumnWriteFragment(
						customWriteExpression,
						parameters,
						jdbcMapping
				);
			}
			else {
				final ColumnValueParameter parameter = new ColumnValueParameter( columnReference, parameterUsage );
				handleParameterCreation( parameter );
				columnWriteFragment = new ColumnWriteFragment( customWriteExpression, parameter, jdbcMapping );
			}
		}
		else {
			columnWriteFragment = new ColumnWriteFragment( customWriteExpression, jdbcMapping );
		}
		return new ColumnValueBinding( columnReference, columnWriteFragment, isNullable ) ;
	}

	protected abstract void handleParameterCreation(ColumnValueParameter parameter);

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
		return "TableMutationBuilder( " + mutationType + " - `" + mutatingTable.getTableName() + "`)";
	}
}
