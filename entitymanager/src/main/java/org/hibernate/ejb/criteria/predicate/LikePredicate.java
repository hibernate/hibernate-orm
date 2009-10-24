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
package org.hibernate.ejb.criteria.predicate;

import javax.persistence.criteria.Expression;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.expression.LiteralExpression;
import org.hibernate.ejb.criteria.expression.ExpressionImplementor;

/**
 * Models a SQL <tt>LIKE</tt> expression.
 *
 * @author Steve Ebersole
 */
public class LikePredicate extends AbstractSimplePredicate {
	private final Expression<String> matchExpression;
	private final Expression<String> pattern;
	private final Expression<Character> escapeCharacter;

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			Expression<String> pattern) {
		this( criteriaBuilder, matchExpression, pattern, null );
	}

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			String pattern) {
		this( criteriaBuilder, matchExpression, new LiteralExpression<String>( criteriaBuilder, pattern) );
	}

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			Expression<String> pattern,
			Expression<Character> escapeCharacter) {
		super( criteriaBuilder );
		this.matchExpression = matchExpression;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
	}

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			Expression<String> pattern,
			char escapeCharacter) {
		this(
				criteriaBuilder,
				matchExpression,
				pattern,
				new LiteralExpression<Character>( criteriaBuilder, escapeCharacter )
		);
	}

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			String pattern,
			char escapeCharacter) {
		this(
				criteriaBuilder,
				matchExpression,
				new LiteralExpression<String>( criteriaBuilder, pattern ),
				new LiteralExpression<Character>( criteriaBuilder, escapeCharacter )
		);
	}

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			String pattern,
			Expression<Character> escapeCharacter) {
		this(
				criteriaBuilder,
				matchExpression,
				new LiteralExpression<String>( criteriaBuilder, pattern ),
				escapeCharacter
		);
	}

	public Expression<Character> getEscapeCharacter() {
		return escapeCharacter;
	}

	public Expression<String> getMatchExpression() {
		return matchExpression;
	}

	public Expression<String> getPattern() {
		return pattern;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getEscapeCharacter(), registry );
		Helper.possibleParameter( getMatchExpression(), registry );
		Helper.possibleParameter( getPattern(), registry );
	}

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		StringBuilder likeExpr = new StringBuilder();
		likeExpr.append( ( (ExpressionImplementor) getMatchExpression() ).render( renderingContext ) )
				.append( " like " )
				.append( ( (ExpressionImplementor) getPattern() ).render( renderingContext ) );
		if ( escapeCharacter != null ) {
			likeExpr.append( " escape " )
					.append( ( (ExpressionImplementor) getEscapeCharacter() ).render( renderingContext ) );
		}
		return likeExpr.toString();
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
