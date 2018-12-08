/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Models a SQL <tt>LIKE</tt> expression.
 *
 * @author Steve Ebersole
 */
public class LikePredicate extends AbstractSimplePredicate {
	private final ExpressionImplementor<String> matchExpression;
	private final ExpressionImplementor<String> pattern;
	private final ExpressionImplementor<Character> escapeCharacter;

	public LikePredicate(
			ExpressionImplementor<String> matchExpression,
			ExpressionImplementor<String> pattern,
			ExpressionImplementor<Character> escapeCharacter,
			CriteriaNodeBuilder nodeBuilder) {
		this( matchExpression, pattern, escapeCharacter, false, nodeBuilder );
	}

	public LikePredicate(
			ExpressionImplementor<String> matchExpression,
			ExpressionImplementor<String> pattern,
			ExpressionImplementor<Character> escapeCharacter,
			boolean negated,
			CriteriaNodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.matchExpression = matchExpression;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
	}

	public ExpressionImplementor<String> getMatchExpression() {
		return matchExpression;
	}

	public ExpressionImplementor<String> getPattern() {
		return pattern;
	}

	public ExpressionImplementor<Character> getEscapeCharacter() {
		return escapeCharacter;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitLikePredicate( this );
	}
}
