/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Models what ANSI SQL terms a simple case statement.  This is a <tt>CASE</tt> expression in the form<pre>
 * CASE [expression]
 *     WHEN [firstCondition] THEN [firstResult]
 *     WHEN [secondCondition] THEN [secondResult]
 *     ELSE [defaultResult]
 * END
 * </pre>
 *
 * @author Steve Ebersole
 */
public class SimpleCase<C,R> extends AbstractExpression<R> implements JpaSimpleCase<C,R> {
	private final ExpressionImplementor<? extends C> expression;
	private List<WhenClause> whenClauses = new ArrayList<>();
	private ExpressionImplementor<? extends R> otherwiseResult;

	@SuppressWarnings("unchecked")
	public SimpleCase(
			ExpressionImplementor<? extends C> expression,
			CriteriaNodeBuilder nodeBuilder) {
		super( (JavaTypeDescriptor) expression.getJavaTypeDescriptor(), nodeBuilder );
		this.expression = expression;
	}

	public SimpleCase(
			Class<R> javaType,
			ExpressionImplementor<? extends C> expression,
			CriteriaNodeBuilder nodeBuilder) {
		super( javaType, nodeBuilder );
		this.expression = expression;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public ExpressionImplementor<C> getExpression() {
		return (ExpressionImplementor<C>) expression;
	}

	public List<WhenClause> getWhenClauses() {
		return whenClauses;
	}

	public ExpressionImplementor<? extends R> getOtherwiseResult() {
		return otherwiseResult;
	}

	@Override
	public JpaSimpleCase<C, R> when(C condition, R result) {
		return when( condition, buildLiteral(result) );
	}

	@SuppressWarnings({ "unchecked" })
	private LiteralExpression<R> buildLiteral(R result) {
		final Class type = result != null
				? result.getClass()
				: getJavaType();
		return new LiteralExpression<R>( type, result, nodeBuilder() );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaSimpleCase<C, R> when(C condition, Expression<? extends R> result) {
		final WhenClause whenClause = new WhenClause(
				new LiteralExpression( condition, nodeBuilder() ),
				(ExpressionImplementor) result
		);
		whenClauses.add( whenClause );
		setJavaType( result.getJavaType() );
		return this;
	}

	@Override
	public JpaSimpleCase<C, R> otherwise(R result) {
		return otherwise( buildLiteral(result) );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaSimpleCase<C, R> otherwise(Expression<? extends R> result) {
		this.otherwiseResult = (ExpressionImplementor<? extends R>) result;
		setJavaType( result.getJavaType() );
		return this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <V> V accept(CriteriaVisitor visitor) {
		return (V) visitor.visitSimpleCase( this );
	}

	public class WhenClause {
		private final LiteralExpression<C> condition;
		private final ExpressionImplementor<? extends R> result;

		WhenClause(LiteralExpression<C> condition, ExpressionImplementor<? extends R> result) {
			this.condition = condition;
			this.result = result;
		}

		public LiteralExpression<C> getCondition() {
			return condition;
		}

		public ExpressionImplementor<? extends R> getResult() {
			return result;
		}

	}
}
