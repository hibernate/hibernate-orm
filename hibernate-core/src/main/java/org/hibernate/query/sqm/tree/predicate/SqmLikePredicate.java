/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import static org.hibernate.query.sqm.internal.TypecheckUtil.assertString;

/**
 * @author Steve Ebersole
 */
public class SqmLikePredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> matchExpression;
	private final SqmExpression<?> pattern;
	private final SqmExpression<?> escapeCharacter;
	private final boolean isCaseSensitive;

	public SqmLikePredicate(
			SqmExpression<?> matchExpression,
			SqmExpression<?> pattern,
			SqmExpression<?> escapeCharacter,
			NodeBuilder nodeBuilder) {
		this( matchExpression, pattern, escapeCharacter, false, nodeBuilder );
	}

	public SqmLikePredicate(
			SqmExpression<?> matchExpression,
			SqmExpression<?> pattern,
			SqmExpression<?> escapeCharacter,
			boolean negated,
			NodeBuilder nodeBuilder) {
		this( matchExpression, pattern, escapeCharacter, negated, true, nodeBuilder );
	}

	public SqmLikePredicate(
			SqmExpression<?> matchExpression,
			SqmExpression<?> pattern,
			SqmExpression<?> escapeCharacter,
			boolean negated,
			boolean isCaseSensitive,
			NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.matchExpression = matchExpression;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
		this.isCaseSensitive = isCaseSensitive;
		final SqmExpressible<?> expressibleType = QueryHelper.highestPrecedenceType(
				matchExpression.getExpressible(),
				pattern.getExpressible()
		);

		assertString( matchExpression );
		assertString( pattern );

		matchExpression.applyInferableType( expressibleType );
		pattern.applyInferableType( expressibleType );

		if ( escapeCharacter != null ) {
			escapeCharacter.applyInferableType( nodeBuilder.getCharacterType() );
		}
	}

	public SqmLikePredicate(
			SqmExpression<?> matchExpression,
			SqmExpression<?> pattern,
			NodeBuilder nodeBuilder) {
		this( matchExpression, pattern, null, nodeBuilder );
	}

	public SqmLikePredicate(
			SqmExpression<?> matchExpression,
			SqmExpression<?> pattern,
			boolean negated,
			boolean isCaseSensitive,
			NodeBuilder nodeBuilder) {
		this( matchExpression, pattern, null, negated, isCaseSensitive, nodeBuilder );
	}

	@Override
	public SqmLikePredicate copy(SqmCopyContext context) {
		final SqmLikePredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmLikePredicate predicate = context.registerCopy(
				this,
				new SqmLikePredicate(
						matchExpression.copy( context ),
						pattern.copy( context ),
						escapeCharacter == null ? null : escapeCharacter.copy( context ),
						isNegated(),
						isCaseSensitive,
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
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

	public boolean isCaseSensitive() {
		return isCaseSensitive;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitLikePredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		matchExpression.appendHqlString( sb );
		if ( isNegated() ) {
			sb.append( " not" );
		}
		sb.append( " like " );
		pattern.appendHqlString( sb );
		if ( escapeCharacter != null ) {
			sb.append( " escape " );
			escapeCharacter.appendHqlString( sb );
		}
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmLikePredicate(
				matchExpression,
				pattern,
				escapeCharacter,
				!isNegated(),
				isCaseSensitive,
				nodeBuilder()
		);
	}
}
