/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmCaseSimple<T,R> extends AbstractSqmExpression<R> implements JpaSimpleCase<T,R> {
	private final SqmExpression<T> fixture;
	private List<WhenFragment<T,R>> whenFragments = new ArrayList<>();
	private SqmExpression<R> otherwise;

	public SqmCaseSimple(SqmExpression<T> fixture, NodeBuilder nodeBuilder) {
		this( fixture, null, nodeBuilder );
	}

	public SqmCaseSimple(SqmExpression<T> fixture, ExpressableType<R> inherentType, NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.fixture = fixture;
	}

	public SqmExpression<T> getFixture() {
		return fixture;
	}

	public List<WhenFragment<T,R>> getWhenFragments() {
		return whenFragments;
	}

	public SqmExpression<R> getOtherwise() {
		return otherwise;
	}

	public void otherwise(SqmExpression<R> otherwiseExpression) {
		this.otherwise = otherwiseExpression;

		applyInferableType( otherwiseExpression.getExpressableType() );
	}

	public void when(SqmExpression<T> test, SqmExpression<R> result) {
		whenFragments.add( new WhenFragment<>( test, result ) );

		applyInferableType( result.getExpressableType() );
	}

	@Override
	public BasicValuedExpressableType<R> getExpressableType() {
		return (BasicValuedExpressableType<R>) super.getExpressableType();
	}

	@Override
	protected void internalApplyInferableType(ExpressableType<?> newType) {
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
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSimpleCaseExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<simple-case>";
	}

	public static class WhenFragment<T,R> {
		private final SqmExpression<T> checkValue;
		private final SqmExpression<R> result;

		public WhenFragment(SqmExpression<T> checkValue, SqmExpression<R> result) {
			this.checkValue = checkValue;
			this.result = result;
		}

		public SqmExpression<T> getCheckValue() {
			return checkValue;
		}

		public SqmExpression<R> getResult() {
			return result;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public JpaExpression<T> getExpression() {
		return getFixture();
	}

	@Override
	public JpaSimpleCase<T, R> when(T condition, R result) {
		when( nodeBuilder().literal( condition ), nodeBuilder().literal( result ) );
		return this;
	}

	@Override
	public JpaSimpleCase<T, R> when(T condition, Expression<? extends R> result) {
		//noinspection unchecked
		when( nodeBuilder().literal( condition ), (SqmExpression) result );
		return this;
	}

	@Override
	public JpaSimpleCase<T, R> otherwise(R result) {
		otherwise( nodeBuilder().literal( result ) );
		return this;
	}

	@Override
	public JpaSimpleCase<T, R> otherwise(Expression<? extends R> result) {
		//noinspection unchecked
		otherwise( (SqmExpression) result );
		return this;
	}
}
