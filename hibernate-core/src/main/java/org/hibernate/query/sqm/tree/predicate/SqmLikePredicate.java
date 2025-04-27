/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import java.util.Objects;

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
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		matchExpression.appendHqlString( hql, context );
		if ( isNegated() ) {
			hql.append( " not" );
		}
		hql.append( " like " );
		pattern.appendHqlString( hql, context );
		if ( escapeCharacter != null ) {
			hql.append( " escape " );
			escapeCharacter.appendHqlString( hql, context );
		}
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmLikePredicate that
			&& this.isNegated() == that.isNegated()
			&& isCaseSensitive == that.isCaseSensitive
			&& Objects.equals( this.matchExpression, that.matchExpression )
			&& Objects.equals( pattern, that.pattern )
			&& Objects.equals( escapeCharacter, that.escapeCharacter );
	}

	@Override
	public int hashCode() {
		return Objects.hash( isNegated(), matchExpression, pattern, escapeCharacter, isCaseSensitive );
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
