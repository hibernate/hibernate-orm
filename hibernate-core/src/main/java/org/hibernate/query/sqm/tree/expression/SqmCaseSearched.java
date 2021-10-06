/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public class SqmCaseSearched<R>
		extends AbstractSqmExpression<R>
		implements JpaSearchedCase<R>, DomainResultProducer<R> {
	private final List<WhenFragment<R>> whenFragments;
	private SqmExpression<R> otherwise;

	public SqmCaseSearched(NodeBuilder nodeBuilder) {
		this( null, nodeBuilder );
	}

	public SqmCaseSearched(SqmExpressable<R> inherentType, NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.whenFragments = new ArrayList<>();
	}

	public SqmCaseSearched(SqmExpressable<R> inherentType, int estimateWhenSize, NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.whenFragments = new ArrayList<>( estimateWhenSize );
	}

	public List<WhenFragment<R>> getWhenFragments() {
		return whenFragments;
	}

	public SqmExpression<R> getOtherwise() {
		return otherwise;
	}

	public SqmCaseSearched<R> when(SqmPredicate predicate, SqmExpression<R> result) {
		whenFragments.add( new WhenFragment<>( predicate, result ) );
		applyInferableResultType( result.getNodeType() );
		return this;
	}

	public SqmCaseSearched<R> otherwise(SqmExpression<R> otherwiseExpression) {
		this.otherwise = otherwiseExpression;
		applyInferableResultType( otherwiseExpression.getNodeType() );
		return this;
	}

	private void applyInferableResultType(SqmExpressable<?> type) {
		if ( type == null ) {
			return;
		}

		final SqmExpressable<?> oldType = getNodeType();

		final SqmExpressable<?> newType = QueryHelper.highestPrecedenceType2( oldType, type );
		if ( newType != null && newType != oldType ) {
			internalApplyInferableType( newType );
		}
	}

	@Override
	protected void internalApplyInferableType(SqmExpressable<?> newType) {
		super.internalApplyInferableType( newType );

		if ( otherwise != null ) {
			otherwise.applyInferableType( newType );
		}

		if ( whenFragments != null ) {
			whenFragments.forEach(
					whenFragment -> whenFragment.getResult().applyInferableType( newType )
			);
		}
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitSearchedCaseExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<searched-case>";
	}

	@Override
	public DomainResult<R> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	public static class WhenFragment<R> {
		private final SqmPredicate predicate;
		private final SqmExpression<R> result;

		public WhenFragment(SqmPredicate predicate, SqmExpression<R> result) {
			this.predicate = predicate;
			this.result = result;
		}

		public SqmPredicate getPredicate() {
			return predicate;
		}

		public SqmExpression<R> getResult() {
			return result;
		}
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "case" );
		for ( WhenFragment<R> whenFragment : whenFragments ) {
			sb.append( " when " );
			whenFragment.predicate.appendHqlString( sb );
			sb.append( " then " );
			whenFragment.result.appendHqlString( sb );
		}

		if ( otherwise != null ) {
			sb.append( " else " );
			otherwise.appendHqlString( sb );
		}
		sb.append( " end" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCaseSearched<R> when(Expression<Boolean> condition, R result) {
		when( nodeBuilder().wrap( condition ), nodeBuilder().value( result, otherwise ) );
		return this;
	}

	@Override
	public SqmCaseSearched<R> when(Expression<Boolean> condition, Expression<? extends R> result) {
		//noinspection unchecked
		when( nodeBuilder().wrap( condition ), (SqmExpression) result );
		return this;
	}

	@Override
	public SqmExpression<R> otherwise(R result) {
		otherwise( nodeBuilder().value( result ) );
		return this;
	}

	@Override
	public SqmExpression<R> otherwise(Expression<? extends R> result) {
		//noinspection unchecked
		otherwise( (SqmExpression) result );
		return this;
	}

}
