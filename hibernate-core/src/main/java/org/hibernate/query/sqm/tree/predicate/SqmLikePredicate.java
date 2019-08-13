/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmLikePredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> matchExpression;
	private final SqmExpression<?> pattern;
	private final SqmExpression<?> escapeCharacter;

	public SqmLikePredicate(
			SqmExpression<?> matchExpression,
			SqmExpression<?> pattern,
			SqmExpression<?> escapeCharacter,
			NodeBuilder nodeBuilder) {
		this( matchExpression, pattern, escapeCharacter, false, nodeBuilder );
	}

	@SuppressWarnings("WeakerAccess")
	public SqmLikePredicate(
			SqmExpression<?> matchExpression,
			SqmExpression<?> pattern,
			SqmExpression<?> escapeCharacter,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.matchExpression = matchExpression;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
	}

	public SqmLikePredicate(
			SqmExpression<?> matchExpression,
			SqmExpression<?> pattern,
			NodeBuilder nodeBuilder) {
		this( matchExpression, pattern, null, nodeBuilder );
	}

	public SqmExpression<?> getMatchExpression() {
		return matchExpression;
	}

	public SqmExpression<?> getPattern() {
		return pattern;
	}

	public SqmExpression<?> getEscapeCharacter() {
		return escapeCharacter;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitLikePredicate( this );
	}
}
