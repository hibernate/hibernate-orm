/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.ArrayList;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.builder.ColumnValueBindingBuilder;

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
	public void consume(int valueIndex, Object value, SelectableMapping jdbcValueMapping) {
		final ColumnValueBinding columnValueBinding = createValueBinding(
				jdbcValueMapping.getSelectionExpression(),
				value == null ? null : jdbcValueMapping.getWriteExpression(),
				jdbcValueMapping.getJdbcMapping()
		);
		add( columnValueBinding );
	}

	@Internal @Incubating
	public void addRestriction(ColumnValueBinding valueBinding) {
		add( valueBinding );
	}

	public void addNullRestriction(SelectableMapping column) {
		add( createValueBinding( column.getSelectionExpression(), null, column.getJdbcMapping() ) );
	}

	public void addRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
		add( createValueBinding( columnName, columnWriteFragment, jdbcMapping ) );
	}

	protected ColumnValueBinding createValueBinding(
			String columnName,
			String customWriteExpression,
			JdbcMapping jdbcMapping) {
		return ColumnValueBindingBuilder.createValueBinding(
				columnName,
				customWriteExpression,
				jdbcMapping,
				mutatingTable,
				parameterUsage,
				parameters::apply
		);
	}

	public boolean containsColumn(String columnName, JdbcMapping jdbcMapping) {
		final ColumnReference reference = new ColumnReference( mutatingTable, columnName, jdbcMapping );
		for ( int i = 0; i < size(); i++ ) {
			if ( get( i ).getColumnReference().equals( reference ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "ColumnValueBindingList" + super.toString();
	}
}
