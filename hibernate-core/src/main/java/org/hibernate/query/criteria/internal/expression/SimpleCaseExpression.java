/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder.SimpleCase;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaExpressionImplementor;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.sqm.parser.criteria.tree.CriteriaVisitor;
import org.hibernate.sqm.query.expression.CaseSimpleSqmExpression;
import org.hibernate.sqm.query.expression.SqmExpression;

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
public class SimpleCaseExpression<C,R>
		extends AbstractExpression<R>
		implements JpaSimpleCase<C,R>, Serializable {
	private Class<R> javaType;
	private final JpaExpressionImplementor<? extends C> expression;
	private List<WhenClause> whenClauses = new ArrayList<>();
	private JpaExpressionImplementor<? extends R> otherwiseResult;

	public class WhenClause {
		private final LiteralExpression<C> condition;
		private final JpaExpressionImplementor<? extends R> result;

		public WhenClause(LiteralExpression<C> condition, JpaExpressionImplementor<? extends R> result) {
			this.condition = condition;
			this.result = result;
		}

		public LiteralExpression<C> getCondition() {
			return condition;
		}

		public JpaExpressionImplementor<? extends R> getResult() {
			return result;
		}

	}

	public SimpleCaseExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<R> javaType,
			JpaExpressionImplementor<? extends C> expression) {
		super( criteriaBuilder, javaType);
		this.javaType = javaType;
		this.expression = expression;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Expression<C> getExpression() {
		return (Expression<C>) expression;
	}

	@Override
	public SimpleCase<C, R> when(C condition, R result) {
		return when( condition, buildLiteral(result) );
	}

	@SuppressWarnings({ "unchecked" })
	private LiteralExpression<R> buildLiteral(R result) {
		final Class<R> type = result != null
				? (Class<R>) result.getClass()
				: getJavaType();
		return new LiteralExpression<R>( criteriaBuilder(), type, result );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public SimpleCase<C, R> when(C condition, Expression<? extends R> result) {
		criteriaBuilder().checkIsJpaExpression( result );
		return when( condition, (JpaExpressionImplementor<? extends R>) result );
	}

	public SimpleCase<C, R> when(C condition, JpaExpressionImplementor<? extends R> result) {
		WhenClause whenClause = new WhenClause(
				new LiteralExpression<>( criteriaBuilder(), condition ),
				result
		);
		whenClauses.add( whenClause );
		adjustJavaType( result );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	private void adjustJavaType(Expression<? extends R> exp) {
		if ( javaType == null ) {
			javaType = (Class<R>) exp.getJavaType();
		}
	}

	@Override
	public JpaExpressionImplementor<R> otherwise(R result) {
		return otherwise( buildLiteral(result) );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpressionImplementor<R> otherwise(Expression<? extends R> result) {
		criteriaBuilder().checkIsJpaExpression( result );
		return otherwise( (JpaExpressionImplementor<? extends R>) result );
	}

	public JpaExpressionImplementor<R> otherwise(JpaExpressionImplementor<? extends R> result) {
		this.otherwiseResult = result;
		adjustJavaType( result );
		return this;
	}

	public JpaExpressionImplementor<? extends R> getOtherwiseResult() {
		return otherwiseResult;
	}

	public List<WhenClause> getWhenClauses() {
		return whenClauses;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getExpression(), registry );
		for ( WhenClause whenClause : getWhenClauses() ) {
			Helper.possibleParameter( whenClause.getResult(), registry );
		}
		Helper.possibleParameter( getOtherwiseResult(), registry );
	}

	@Override
	public SqmExpression visitExpression(CriteriaVisitor visitor) {
		final CaseSimpleSqmExpression caseExpression = new CaseSimpleSqmExpression(
				expression.visitExpression( visitor )
		);

		for ( WhenClause whenClause : whenClauses ) {
			caseExpression.when(
					whenClause.getCondition().visitExpression( visitor ),
					whenClause.getResult().visitExpression( visitor )
			);
		}

		if ( otherwiseResult != null ) {
			caseExpression.otherwise( otherwiseResult.visitExpression( visitor ) );
		}

		return caseExpression;
	}
}
