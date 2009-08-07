/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.criteria.expression.LiteralExpression;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class LikePredicate extends AbstractSimplePredicate {
	private final Expression<String> matchExpression;
	private final Expression<String> pattern;
	private final Expression<Character> escapeCharacter;

	public LikePredicate(
			QueryBuilderImpl queryBuilder,
			Expression<String> matchExpression,
			Expression<String> pattern) {
		this( queryBuilder, matchExpression, pattern, null );
	}

	public LikePredicate(
			QueryBuilderImpl queryBuilder,
			Expression<String> matchExpression,
			String pattern) {
		this( queryBuilder, matchExpression, new LiteralExpression<String>( queryBuilder, pattern) );
	}

	public LikePredicate(
			QueryBuilderImpl queryBuilder,
			Expression<String> matchExpression,
			Expression<String> pattern,
			Expression<Character> escapeCharacter) {
		super( queryBuilder );
		this.matchExpression = matchExpression;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
	}

	public LikePredicate(
			QueryBuilderImpl queryBuilder,
			Expression<String> matchExpression,
			Expression<String> pattern,
			char escapeCharacter) {
		this(
				queryBuilder,
				matchExpression,
				pattern,
				new LiteralExpression<Character>( queryBuilder, escapeCharacter )
		);
	}

	public LikePredicate(
			QueryBuilderImpl queryBuilder,
			Expression<String> matchExpression,
			String pattern,
			char escapeCharacter) {
		this(
				queryBuilder,
				matchExpression,
				new LiteralExpression<String>( queryBuilder, pattern ),
				new LiteralExpression<Character>( queryBuilder, escapeCharacter )
		);
	}

	public LikePredicate(
			QueryBuilderImpl queryBuilder,
			Expression<String> matchExpression,
			String pattern,
			Expression<Character> escapeCharacter) {
		this(
				queryBuilder,
				matchExpression,
				new LiteralExpression<String>( queryBuilder, pattern ),
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


}
