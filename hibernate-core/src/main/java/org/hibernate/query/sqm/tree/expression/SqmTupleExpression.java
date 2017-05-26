/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.query.sqm.QueryException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

import static org.hibernate.query.sqm.tree.internal.Helper.firstNonNull;

/**
 * @author Steve Ebersole
 */
public class SqmTupleExpression implements SqmExpression {
	private final List<SqmExpression> groupedExpressions;

	public SqmTupleExpression(SqmExpression groupedExpression) {
		this( Collections.singletonList( groupedExpression ) );
	}

	public SqmTupleExpression(List<SqmExpression> groupedExpressions) {
		if ( groupedExpressions.isEmpty() ) {
			throw new QueryException( "tuple grouping cannot be constructed over zero expressions" );
		}
		this.groupedExpressions = groupedExpressions;
	}

	@Override
	public ExpressableType getExpressionType() {
		final Optional<SqmExpression> first = groupedExpressions.stream()
				.filter( sqmExpression -> sqmExpression.getExpressionType() != null )
				.findFirst();
		if ( !first.isPresent() ) {
			return null;
		}

		return first.get().getExpressionType();
	}

	@Override
	public ExpressableType getInferableType() {
		return null;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return null;
	}

	@Override
	public String asLoggableText() {
		return null;
	}
}
