/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.function.Function;

import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.expression.SqmSelfRenderingExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * A function for producing an {@link Expression} from a {@link NavigablePath} for a {@link TableGroup} and {@link SelectableMapping}.
 */
public class SelectableMappingExpressionConverter implements Function<SemanticQueryWalker<?>, Expression> {

	private final NavigablePath navigablePath;
	private final SelectableMapping selectableMapping;

	private SelectableMappingExpressionConverter(NavigablePath navigablePath, SelectableMapping selectableMapping) {
		this.navigablePath = navigablePath;
		this.selectableMapping = selectableMapping;
	}

	public static <T> SqmSelection<T> forSelectableMapping(SqmFrom<?, T> from, SelectableMapping selectableMapping) {
		return new SqmSelection<>(
				new SqmSelfRenderingExpression<>(
						new SelectableMappingExpressionConverter( from.getNavigablePath(), selectableMapping ),
						(SqmExpressible<T>) selectableMapping.getJdbcMapping(),
						from.nodeBuilder()
				),
				from.nodeBuilder()
		);
	}

	@Override
	public Expression apply(SemanticQueryWalker semanticQueryWalker) {
		final SqmToSqlAstConverter converter = (SqmToSqlAstConverter) semanticQueryWalker;
		final TableGroup tableGroup = converter.getFromClauseAccess().getTableGroup( navigablePath );
		final Expression expression = converter.getCurrentProcessingState()
				.getSqlExpressionResolver()
				.resolveSqlExpression(
						tableGroup.getTableReference( selectableMapping.getContainingTableExpression() ),
						selectableMapping
				);
		return new ExpressionDomainResultProducer( expression );
	}
}
