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
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmCaseSimple extends AbstractInferableTypeSqmExpression {
	private final SqmExpression fixture;
	private List<WhenFragment> whenFragments = new ArrayList<>();
	private SqmExpression otherwise;

	public SqmCaseSimple(SqmExpression fixture) {
		this( fixture, null );
	}

	public SqmCaseSimple(SqmExpression fixture, ExpressableType inherentType) {
		super( inherentType );
		this.fixture = fixture;
	}

	public SqmExpression getFixture() {
		return fixture;
	}

	public List<WhenFragment> getWhenFragments() {
		return whenFragments;
	}

	public SqmExpression getOtherwise() {
		return otherwise;
	}

	public void otherwise(SqmExpression otherwiseExpression) {
		this.otherwise = otherwiseExpression;

		setInherentType( otherwiseExpression.getExpressableType() );
	}

	public void when(SqmExpression test, SqmExpression result) {
		whenFragments.add( new WhenFragment( test, result ) );

		setInherentType( result.getExpressableType() );
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return (BasicValuedExpressableType) getInferableType().get();
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
		return walker.visitSimpleCaseExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<simple-case>";
	}

	public static class WhenFragment {
		private final SqmExpression checkValue;
		private final SqmExpression result;

		public WhenFragment(SqmExpression checkValue, SqmExpression result) {
			this.checkValue = checkValue;
			this.result = result;
		}

		public SqmExpression getCheckValue() {
			return checkValue;
		}

		public SqmExpression getResult() {
			return result;
		}
	}
}
