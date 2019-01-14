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
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmCaseSearched extends AbstractSqmExpression {
	private List<WhenFragment> whenFragments = new ArrayList<>();
	private SqmExpression otherwise;

	public SqmCaseSearched() {
		this( null );
	}

	public SqmCaseSearched(BasicValuedExpressableType inherentType) {
		super( inherentType );
	}

	public List<WhenFragment> getWhenFragments() {
		return whenFragments;
	}

	public SqmExpression getOtherwise() {
		return otherwise;
	}

	public void when(SqmPredicate predicate, SqmExpression result) {
		whenFragments.add( new WhenFragment( predicate, result ) );
		setInherentType( result.getExpressableType() );
	}

	public void otherwise(SqmExpression otherwiseExpression) {
		this.otherwise = otherwiseExpression;
		setInherentType( otherwiseExpression.getExpressableType() );
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
