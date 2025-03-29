/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;

/**
 */
public class AggregateColumnWriteExpression implements Expression, AggregateSupport.AggregateColumnWriteExpression {

	private final ColumnReference aggregateColumnReference;
	private final SelectableMapping[] selectableMappings;
	private final Expression[] valueExpressions;
	private final AggregateSupport.WriteExpressionRenderer columnWriter;

	public AggregateColumnWriteExpression(
			ColumnReference aggregateColumnReference,
			AggregateSupport.WriteExpressionRenderer columnWriter,
			SelectableMapping[] selectableMappings,
			Expression[] valueExpressions) {
		this.aggregateColumnReference = aggregateColumnReference;
		this.selectableMappings = selectableMappings;
		this.valueExpressions = valueExpressions;
		this.columnWriter = columnWriter;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return aggregateColumnReference.getExpressionType();
	}

	@Override
	public ColumnReference getColumnReference() {
		return aggregateColumnReference;
	}

	public ColumnReference getAggregateColumnReference() {
		return aggregateColumnReference;
	}

	public SelectableMapping[] getSelectableMappings() {
		return selectableMappings;
	}

	public Expression[] getValueExpressions() {
		return valueExpressions;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitAggregateColumnWriteExpression( this );
	}

	@Override
	public Expression getValueExpression(SelectableMapping selectableMapping) {
		for ( int i = 0; i < selectableMappings.length; i++ ) {
			if ( selectableMapping == selectableMappings[i] ) {
				return valueExpressions[i];
			}
		}

		throw new IllegalArgumentException( "Couldn't find value expression for selectable mapping: " + selectableMapping );
	}

	public void appendWriteExpression(SqlAstTranslator<?> translator, SqlAppender appender) {
		appendWriteExpression( translator, appender, aggregateColumnReference.getQualifier() );
	}

	public void appendWriteExpression(SqlAstTranslator<?> translator, SqlAppender appender, String qualifier) {
		columnWriter.render( appender, translator, this, qualifier );
	}
}
