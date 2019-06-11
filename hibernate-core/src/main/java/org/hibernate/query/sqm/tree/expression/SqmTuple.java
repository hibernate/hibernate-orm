/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.Arrays;
import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Models a tuple of values, generally defined as a series of values
 * wrapped in parentheses, e.g. `(value1, value2, ..., valueN)`.
 *
 * Differs from {@link SqmJpaCompoundSelection} in that this node can be used
 * anywhere in the SQM tree, whereas SqmJpaCompoundSelection is only valid
 * in the SELECT clause per JPA
 *
 * @author Steve Ebersole
 */
public class SqmTuple<T> extends AbstractSqmExpression<T> implements JpaCompoundSelection<T> {
	private final List<SqmExpression<?>> groupedExpressions;

	public SqmTuple(NodeBuilder nodeBuilder, SqmExpression<?>... groupedExpressions) {
		this( Arrays.asList( groupedExpressions ), nodeBuilder );
	}

	public SqmTuple(NodeBuilder nodeBuilder, SqmExpressable<T> type, SqmExpression<?>... groupedExpressions) {
		this( Arrays.asList( groupedExpressions ), nodeBuilder );
		applyInferableType( type );
	}

	public SqmTuple(List<SqmExpression<?>> groupedExpressions, NodeBuilder nodeBuilder) {
		super( null, nodeBuilder );
		if ( groupedExpressions.isEmpty() ) {
			throw new QueryException( "tuple grouping cannot be constructed over zero expressions" );
		}
		this.groupedExpressions = groupedExpressions;
	}

	public SqmTuple(List<SqmExpression<?>> groupedExpressions, SqmExpressable<T> type, NodeBuilder nodeBuilder) {
		this( groupedExpressions, nodeBuilder );
		applyInferableType( type );
	}

	@Override
	public SqmExpressable<T> getNodeType() {
		final SqmExpressable<T> expressableType = super.getNodeType();
		if ( expressableType != null ) {
			return expressableType;
		}

		for ( SqmExpression groupedExpression : groupedExpressions ) {
			//noinspection unchecked
			final SqmExpressable<T> groupedExpressionExpressableType = groupedExpression.getNodeType();
			if ( groupedExpressionExpressableType != null ) {
				return groupedExpressionExpressableType;
			}
		}

		return null;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitTuple( this );
	}

	@Override
	public String asLoggableText() {
		return toString();
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getNodeType().getExpressableJavaTypeDescriptor();
	}

	@Override
	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		return groupedExpressions;
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
