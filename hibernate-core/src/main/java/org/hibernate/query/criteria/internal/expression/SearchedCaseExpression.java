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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.Case;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaExpressionImplementor;
import org.hibernate.query.criteria.JpaPredicateImplementor;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.sqm.parser.criteria.tree.CriteriaVisitor;
import org.hibernate.sqm.query.expression.CaseSearchedSqmExpression;
import org.hibernate.sqm.query.expression.SqmExpression;

/**
 * Models what ANSI SQL terms a <tt>searched case expression</tt>.  This is a <tt>CASE</tt> expression
 * in the form<pre>
 * CASE
 *     WHEN [firstCondition] THEN [firstResult]
 *     WHEN [secondCondition] THEN [secondResult]
 *     ELSE [defaultResult]
 * END
 * </pre>
 *
 * @author Steve Ebersole
 */
public class SearchedCaseExpression<R>
		extends AbstractExpression<R>
		implements JpaSearchedCase<R>, Serializable {
	private Class<R> javaType; // overrides the javaType kept on tuple-impl so that we can adjust it
	private List<WhenClause> whenClauses = new ArrayList<WhenClause>();
	private JpaExpressionImplementor<? extends R> otherwiseResult;

	@Override
	public SqmExpression visitExpression(CriteriaVisitor visitor) {
		final CaseSearchedSqmExpression caseExpression = new CaseSearchedSqmExpression();

		for ( WhenClause whenClause : whenClauses ) {
			caseExpression.when(
					whenClause.getCondition().visitPredicate( visitor ),
					whenClause.getResult().visitExpression( visitor )
			);
		}

		if ( otherwiseResult != null ) {
			caseExpression.otherwise( otherwiseResult.visitExpression( visitor ) );
		}

		return caseExpression;
	}

	public class WhenClause {
		private final JpaPredicateImplementor condition;
		private final JpaExpressionImplementor<? extends R> result;

		public WhenClause(JpaPredicateImplementor condition, JpaExpressionImplementor<? extends R> result) {
			this.condition = condition;
			this.result = result;
		}

		public JpaPredicateImplementor getCondition() {
			return condition;
		}

		public JpaExpressionImplementor<? extends R> getResult() {
			return result;
		}
	}

	public SearchedCaseExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<R> javaType) {
		super( criteriaBuilder, javaType );
		this.javaType = javaType;
	}

	public Case<R> when(Expression<Boolean> condition, R result) {
		return when( condition, buildLiteral( result ) );
	}

	@SuppressWarnings({"unchecked"})
	private LiteralExpression<R> buildLiteral(R result) {
		final Class<R> type = result != null
				? (Class<R>) result.getClass()
				: getJavaType();
		return new LiteralExpression<R>( criteriaBuilder(), type, result );
	}

	public Case<R> when(JpaPredicateImplementor condition, JpaExpressionImplementor<? extends R> result) {
		WhenClause whenClause = new WhenClause( condition, result );
		whenClauses.add( whenClause );
		adjustJavaType( result );
		return this;
	}

	@SuppressWarnings({"unchecked"})
	private void adjustJavaType(Expression<? extends R> exp) {
		if ( javaType == null ) {
			javaType = (Class<R>) exp.getJavaType();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Case<R> when(Expression<Boolean> condition, Expression<? extends R> result) {
		criteriaBuilder().checkIsJpaExpression( result );
		return when( criteriaBuilder().wrap( condition ), (JpaExpressionImplementor<? extends R>) result );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Expression<R> otherwise(Expression<? extends R> result) {
		criteriaBuilder().checkIsJpaExpression( result );
		return otherwise( (JpaExpressionImplementor<? extends R>) result );
	}

	public Expression<R> otherwise(R result) {
		return otherwise( buildLiteral( result ) );
	}

	public Expression<R> otherwise(JpaExpressionImplementor<? extends R> result) {
		this.otherwiseResult = result;
		adjustJavaType( result );
		return this;
	}

	public Expression<? extends R> getOtherwiseResult() {
		return otherwiseResult;
	}

	public List<WhenClause> getWhenClauses() {
		return whenClauses;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getOtherwiseResult(), registry );
		for ( WhenClause whenClause : getWhenClauses() ) {
			Helper.possibleParameter( whenClause.getCondition(), registry );
			Helper.possibleParameter( whenClause.getResult(), registry );
		}
	}
}
