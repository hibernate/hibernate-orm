/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.sql.internal.SqmExpressionInterpretation;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
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
public class SqmTuple<T>
		extends AbstractSqmExpression<T>
		implements JpaCompoundSelection<T>, SqmExpressionInterpretation<T> {
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
	public Expression toSqlExpression(
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		final List<Expression> groupedSqlExpressions  = new ArrayList<>();

		for ( SqmExpression groupedExpression : groupedExpressions ) {
			final SqmExpressionInterpretation interpretation = (SqmExpressionInterpretation) groupedExpression.accept( walker );
			final Expression sqlExpression = interpretation.toSqlExpression(
					clause, walker,
					sqlAstCreationState
			);

			groupedSqlExpressions.add( sqlExpression );
		}

		return new SqlTuple(
				groupedSqlExpressions,
				sqlAstCreationState.getCreationContext().getDomainModel()
						.resolveMappingExpressable( getExpressableType() )
		);
	}

	@Override
	public DomainResultProducer getDomainResultProducer(
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		return null;
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

}
