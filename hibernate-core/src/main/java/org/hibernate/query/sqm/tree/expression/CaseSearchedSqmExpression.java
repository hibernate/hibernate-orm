/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.type.SqmDomainType;
import org.hibernate.query.sqm.domain.SqmExpressableType;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * @author Steve Ebersole
 */
public class CaseSearchedSqmExpression implements SqmExpression, ImpliedTypeSqmExpression {
	private List<WhenFragment> whenFragments = new ArrayList<>();
	private SqmExpression otherwise;

	private SqmExpressableType expressableType;
	private SqmExpressableType impliedType;

	public List<WhenFragment> getWhenFragments() {
		return whenFragments;
	}

	public SqmExpression getOtherwise() {
		return otherwise;
	}

	public void when(SqmPredicate predicate, SqmExpression result) {
		whenFragments.add( new WhenFragment( predicate, result ) );
	}

	public void otherwise(SqmExpression otherwiseExpression) {
		this.otherwise = otherwiseExpression;
		// todo : inject implied type?
	}

	@Override
	public void impliedType(SqmExpressableType type) {
		this.impliedType = type;
		// todo : visit whenFragments and otherwise
	}

	@Override
	public SqmDomainType getExportedDomainType() {
		if ( impliedType != null ) {
			return impliedType.getExportedDomainType();
		}

		if ( otherwise != null ) {
			return otherwise.getExpressionType().getExportedDomainType();
		}

		for ( WhenFragment whenFragment : whenFragments ) {
			if ( whenFragment.result.getExpressionType() != null ) {
				return whenFragment.result.getExpressionType().getExportedDomainType();
			}
		}

		return null;
	}

	@Override
	public SqmExpressableType getExpressionType() {
		return expressableType;
	}

	@Override
	public SqmExpressableType getInferableType() {
		if ( otherwise != null ) {
			return otherwise.getInferableType();
		}

		for ( WhenFragment whenFragment : whenFragments ) {
			if ( whenFragment.result.getExpressionType() != null ) {
				return whenFragment.result.getInferableType();
			}
		}

		return expressableType;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitSearchedCaseExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<searched-case>";
	}

	public static class WhenFragment {
		private final SqmPredicate predicate;
		private final SqmExpression result;

		public WhenFragment(SqmPredicate predicate, SqmExpression result) {
			this.predicate = predicate;
			this.result = result;
		}

		public SqmPredicate getPredicate() {
			return predicate;
		}

		public SqmExpression getResult() {
			return result;
		}
	}
}
