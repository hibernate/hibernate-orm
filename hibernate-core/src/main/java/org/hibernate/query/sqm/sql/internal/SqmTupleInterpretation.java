/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;

/**
 * @author Steve Ebersole
 */
public class SqmTupleInterpretation<T> extends SqlTuple {

	public static <T> SqmTupleInterpretation<T> from(
			SqmTuple<T> sqmTuple,
			SemanticQueryWalker<?> walker,
			SqlAstCreationState sqlAstCreationState) {
		final List<Expression> groupedSqlExpressions  = new ArrayList<>();

		for ( SqmExpression<?> groupedExpression : sqmTuple.getGroupedExpressions() ) {
			groupedSqlExpressions.add( (Expression) groupedExpression.accept( walker ) );
		}

		return new SqmTupleInterpretation<>(
				sqmTuple,
				groupedSqlExpressions,
				SqmMappingModelHelper.resolveMappingModelExpressable( sqmTuple, sqlAstCreationState )
		);
	}

	private final SqmTuple<T> interpretedSqmTuple;

	public SqmTupleInterpretation(
			SqmTuple<T> sqmTuple,
			List<? extends Expression> expressions,
			MappingModelExpressable valueMapping) {

		super( expressions, valueMapping );
		interpretedSqmTuple = sqmTuple;
	}

	public SqmTuple<T> getInterpretedSqmTuple() {
		return interpretedSqmTuple;
	}
}
