/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.predicate;

import org.hibernate.sql.sqm.ast.expression.Expression;
import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;

/**
 * @author Steve Ebersole
 */
public class LikePredicate implements Predicate {
	private final Expression matchExpression;
	private final Expression pattern;
	private final Expression escapeCharacter;
	private final boolean negated;

	public LikePredicate(
			Expression matchExpression,
			Expression pattern,
			Expression escapeCharacter) {
		this( matchExpression, pattern, escapeCharacter, false );
	}

	public LikePredicate(
			Expression matchExpression,
			Expression pattern,
			Expression escapeCharacter, boolean negated) {
		this.matchExpression = matchExpression;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
		this.negated = negated;
	}

	public LikePredicate(Expression matchExpression, Expression pattern) {
		this( matchExpression, pattern, null );
	}

	public Expression getMatchExpression() {
		return matchExpression;
	}

	public Expression getPattern() {
		return pattern;
	}

	public Expression getEscapeCharacter() {
		return escapeCharacter;
	}

	public boolean isNegated() {
		return negated;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitLikePredicate( this );
	}
}
