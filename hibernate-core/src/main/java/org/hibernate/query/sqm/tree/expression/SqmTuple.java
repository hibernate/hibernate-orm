/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.query.sqm.QueryException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;


/**
 * Models a tuple of values, generally defined as a series of values
 * wrapped in parentheses, e.g. `(value1, value2, ..., valueN)`
 *
 * @author Steve Ebersole
 */
public class SqmTuple implements SqmExpression {
	private final List<SqmExpression> groupedExpressions;

	public SqmTuple(SqmExpression groupedExpression) {
		this( Collections.singletonList( groupedExpression ) );
	}

	public SqmTuple(SqmExpression... groupedExpressions) {
		this( Arrays.asList( groupedExpressions ));
	}

	public SqmTuple(List<SqmExpression> groupedExpressions) {
		if ( groupedExpressions.isEmpty() ) {
			throw new QueryException( "tuple grouping cannot be constructed over zero expressions" );
		}
		this.groupedExpressions = groupedExpressions;
	}

	@Override
	public ExpressableType getExpressableType() {
		final Optional<SqmExpression> first = groupedExpressions.stream()
				.filter( sqmExpression -> sqmExpression.getExpressableType() != null )
				.findFirst();
		if ( !first.isPresent() ) {
			return null;
		}

		return first.get().getExpressableType();
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

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}

//	@Override
//	public QueryResult createDomainResult(
//			SemanticQueryWalker walker,
//			String resultVariable,
//			QueryResultCreationContext creationContext) {
//		return null;
//	}
//
//	@Override
//	public QueryResult createDomainResult(
//			Expression expression,
//			String resultVariable,
//			QueryResultCreationContext creationContext) {
//		// todo (6.0) : pretty sure this is not correct.
//		//		should return a result over all the expressions, not just the first -
//		//		a "composite" result.
//		//
//		// todo (6.0) : ultimately the problem here is expecting the "resolved" SQL AST node to be passed in.
//		//		really resolving these SQL AST nodes should be done here.
//		return groupedExpressions.get( 0 ).createDomainResult( expression, resultVariable, creationContext );
//	}

}
