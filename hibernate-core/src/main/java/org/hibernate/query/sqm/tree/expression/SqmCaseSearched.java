/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmCaseSearched extends AbstractInferableTypeSqmExpression {
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
	public void impliedType(Supplier<? extends ExpressableType> inference) {
		super.impliedType( inference );

		// apply the inference to `when` and `otherwise` fragments...

		for ( WhenFragment whenFragment : whenFragments ) {
			if ( whenFragment.getResult() instanceof InferableTypeSqmExpression ) {
				( (InferableTypeSqmExpression) whenFragment.getResult() ).impliedType( inference );
			}
		}

		if ( otherwise != null ) {
			if ( otherwise instanceof InferableTypeSqmExpression ) {
				( (InferableTypeSqmExpression) otherwise ).impliedType( inference );
			}
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
