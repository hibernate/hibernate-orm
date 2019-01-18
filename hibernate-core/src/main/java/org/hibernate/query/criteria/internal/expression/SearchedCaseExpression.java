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
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder.Case;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

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
		extends ExpressionImpl<R>
		implements Case<R>, Serializable {
	private List<WhenClause> whenClauses = new ArrayList<WhenClause>();
	private Expression<? extends R> otherwiseResult;

	public class WhenClause {
		private final Expression<Boolean> condition;
		private final Expression<? extends R> result;

		public WhenClause(Expression<Boolean> condition, Expression<? extends R> result) {
			this.condition = condition;
			this.result = result;
		}

		public Expression<Boolean> getCondition() {
			return condition;
		}

		public Expression<? extends R> getResult() {
			return result;
		}
	}

	public SearchedCaseExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<R> javaType) {
		super( criteriaBuilder, javaType );
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

	public Case<R> when(Expression<Boolean> condition, Expression<? extends R> result) {
		WhenClause whenClause = new WhenClause( condition, result );
		whenClauses.add( whenClause );
		resetJavaType( result.getJavaType() );
		return this;
	}

	public Expression<R> otherwise(R result) {
		return otherwise( buildLiteral( result ) );
	}

	public Expression<R> otherwise(Expression<? extends R> result) {
		this.otherwiseResult = result;
		resetJavaType( result.getJavaType() );
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

	public String render(RenderingContext renderingContext) {
		StringBuilder caseStatement = new StringBuilder( "case" );
		for ( WhenClause whenClause : getWhenClauses() ) {
			caseStatement.append( " when " )
					.append( ( (Renderable) whenClause.getCondition() ).render( renderingContext ) )
					.append( " then " )
					.append( ( (Renderable) whenClause.getResult() ).render( renderingContext ) );
		}

		Expression<?> otherwiseResult = getOtherwiseResult();

		if ( otherwiseResult != null ) {
			caseStatement.append( " else " )
					.append( ( (Renderable) otherwiseResult ).render( renderingContext ) );
		}

		caseStatement.append( " end" );

		return caseStatement.toString();
	}
}
