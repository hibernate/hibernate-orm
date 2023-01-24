/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast;

import java.util.ArrayList;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

@Internal
public class ColumnValueBindingList extends ArrayList<ColumnValueBinding> implements ModelPart.JdbcValueConsumer {

	private final MutatingTableReference mutatingTable;
	private final ColumnValueParameterList parameters;
	private final ParameterUsage parameterUsage;

	public ColumnValueBindingList(
			MutatingTableReference mutatingTable,
			ColumnValueParameterList parameters,
			ParameterUsage parameterUsage) {
		this.mutatingTable = mutatingTable;
		this.parameters = parameters;
		this.parameterUsage = parameterUsage;
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void consume(Object value, SelectableMapping jdbcValueMapping) {
		final ColumnValueBinding columnValueBinding = createValueBinding(
				jdbcValueMapping.getSelectionExpression(),
				value == null ? null : jdbcValueMapping.getWriteExpression(),
				jdbcValueMapping.getJdbcMapping()
		);
		add( columnValueBinding );
	}

	public void addNullRestriction(SelectableMapping column) {
		add( createValueBinding( column.getSelectionExpression(), null, column.getJdbcMapping() ) );
	}

	public void addRestriction(SelectableMapping column) {
		add(
				createValueBinding(
						column.getSelectionExpression(),
						column.getWriteExpression(),
						column.getJdbcMapping()
				)
		);
	}

	public void addRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
		add( createValueBinding( columnName, columnWriteFragment, jdbcMapping ) );
	}

	protected ColumnValueBinding createValueBinding(
			String columnName,
			String customWriteExpression,
			JdbcMapping jdbcMapping) {
		final ColumnReference columnReference = new ColumnReference( mutatingTable, columnName, jdbcMapping );
		final ColumnWriteFragment columnWriteFragment;
		if ( customWriteExpression == null ) {
			columnWriteFragment = null;
		}
		else if ( customWriteExpression.contains( "?" ) ) {
			final JdbcType jdbcType = jdbcMapping.getJdbcType();
			final EmbeddableMappingType aggregateMappingType = jdbcType instanceof AggregateJdbcType
					? ( (AggregateJdbcType) jdbcType ).getEmbeddableMappingType()
					: null;
			if ( aggregateMappingType != null && !aggregateMappingType.shouldBindAggregateMapping() ) {
				final ColumnValueParameterList parameters = new ColumnValueParameterList(
						mutatingTable,
						parameterUsage,
						aggregateMappingType.getJdbcTypeCount()
				);
				aggregateMappingType.forEachSelectable( parameters );
				this.parameters.addAll( parameters );

				columnWriteFragment = new ColumnWriteFragment(
						customWriteExpression,
						parameters,
						jdbcMapping
				);
			}
			else {
				final ColumnValueParameter parameter = new ColumnValueParameter( columnReference, parameterUsage );
				parameters.add( parameter );
				columnWriteFragment = new ColumnWriteFragment( customWriteExpression, parameter, jdbcMapping );
			}
		}
		else {
			columnWriteFragment = new ColumnWriteFragment( customWriteExpression, jdbcMapping );
		}
		return new ColumnValueBinding( columnReference, columnWriteFragment ) ;
	}

	@Override
	public String toString() {
		return "ColumnValueBindingList" + super.toString();
	}
}
