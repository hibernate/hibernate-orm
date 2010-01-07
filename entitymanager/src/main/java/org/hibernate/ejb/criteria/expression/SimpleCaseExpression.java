/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.CriteriaBuilder.SimpleCase;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.Renderable;

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
		extends ExpressionImpl<R>
		implements SimpleCase<C,R>, Serializable {
	private Class<R> javaType;
	private final Expression<? extends C> expression;
	private List<WhenClause> whenClauses = new ArrayList<WhenClause>();
	private Expression<? extends R> otherwiseResult;

	public class WhenClause {
		private final C condition;
		private final Expression<? extends R> result;

		public WhenClause(C condition, Expression<? extends R> result) {
			this.condition = condition;
			this.result = result;
		}

		public C getCondition() {
			return condition;
		}

		public Expression<? extends R> getResult() {
			return result;
		}

	}

	public SimpleCaseExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<R> javaType,
			Expression<? extends C> expression) {
		super( criteriaBuilder, javaType);
		this.javaType = javaType;
		this.expression = expression;
	}

	@SuppressWarnings({ "unchecked" })
	public Expression<C> getExpression() {
		return (Expression<C>) expression;
	}

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

	public SimpleCase<C, R> when(C condition, Expression<? extends R> result) {
		WhenClause whenClause = new WhenClause( condition, result );
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

	public Expression<R> otherwise(R result) {
		return otherwise( buildLiteral(result) );
	}

	public Expression<R> otherwise(Expression<? extends R> result) {
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
		Helper.possibleParameter( getExpression(), registry );
		for ( WhenClause whenClause : getWhenClauses() ) {
			Helper.possibleParameter( whenClause.getResult(), registry );
		}
		Helper.possibleParameter( getOtherwiseResult(), registry );
	}

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		StringBuilder caseExpr = new StringBuilder();
		caseExpr.append( "case " )
				.append(  ( (Renderable) getExpression() ).render( renderingContext ) )
				.append( ' ' );
		for ( WhenClause whenClause : getWhenClauses() ) {
			caseExpr.append( ( (Renderable) whenClause.getCondition() ).render( renderingContext ) )
					.append( " then "  )
					.append( ( (Renderable) whenClause.getResult() ).render( renderingContext ) );
		}
		caseExpr.append( " else " )
				.append( ( (Renderable) getOtherwiseResult() ).render( renderingContext ) )
				.append( " end" );
		return caseExpr.toString();
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
