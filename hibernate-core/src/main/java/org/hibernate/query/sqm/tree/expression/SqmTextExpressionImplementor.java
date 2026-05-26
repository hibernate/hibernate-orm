/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * @author Steve Ebersole
 */
public interface SqmTextExpressionImplementor
		extends SqmComparableExpressionImplementor<String>, SqmTextExpression {
	char LIKE_ESCAPE_CHARACTER = '\\';

	SqmCriteriaNodeBuilder nodeBuilder();

	@Nonnull
	@Override
	default SqmPredicate like(@Nonnull Expression<String> pattern) {
		return nodeBuilder().like( this, pattern );
	}

	@Nonnull
	@Override
	default SqmPredicate like(@Nonnull String pattern) {
		return nodeBuilder().like( this, pattern );
	}

	@Nonnull
	@Override
	default SqmPredicate like(@Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar) {
		return nodeBuilder().like( this, pattern, escapeChar );
	}

	@Nonnull
	@Override
	default SqmPredicate like(@Nonnull Expression<String> pattern, char escapeChar) {
		return nodeBuilder().like( this, pattern, escapeChar );
	}

	@Nonnull
	@Override
	default SqmPredicate like(@Nonnull String pattern, @Nonnull Expression<Character> escapeChar) {
		return nodeBuilder().like( this, pattern, escapeChar );
	}

	@Nonnull
	@Override
	default SqmPredicate like(@Nonnull String pattern, char escapeChar) {
		return nodeBuilder().like( this, pattern, escapeChar );
	}

	@Nonnull
	@Override
	default SqmPredicate notLike(@Nonnull Expression<String> pattern) {
		return nodeBuilder().notLike( this, pattern );
	}

	@Nonnull
	@Override
	default SqmPredicate notLike(@Nonnull String pattern) {
		return nodeBuilder().notLike( this, pattern );
	}

	@Nonnull
	@Override
	default SqmPredicate notLike(@Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar) {
		return nodeBuilder().notLike( this, pattern, escapeChar );
	}

	@Nonnull
	@Override
	default SqmPredicate notLike(@Nonnull Expression<String> pattern, char escapeChar) {
		return nodeBuilder().notLike( this, pattern, escapeChar );
	}

	@Nonnull
	@Override
	default SqmPredicate notLike(@Nonnull String pattern, @Nonnull Expression<Character> escapeChar) {
		return nodeBuilder().notLike( this, pattern, escapeChar );
	}

	@Nonnull
	@Override
	default SqmPredicate notLike(@Nonnull String pattern, char escapeChar) {
		return nodeBuilder().notLike( this, pattern, escapeChar );
	}

	@Nonnull
	@Override
	default SqmPredicate contains(@Nonnull String substring) {
		return like( "%" + escapeLikePattern( substring ) + "%", LIKE_ESCAPE_CHARACTER );
	}

	@Nonnull
	@Override
	default SqmPredicate notContains(@Nonnull String substring) {
		return notLike( "%" + escapeLikePattern( substring ) + "%", LIKE_ESCAPE_CHARACTER );
	}

	@Nonnull
	@Override
	default SqmPredicate startsWith(@Nonnull String prefix) {
		return like( escapeLikePattern( prefix ) + "%", LIKE_ESCAPE_CHARACTER );
	}

	@Nonnull
	@Override
	default SqmPredicate notStartsWith(@Nonnull String prefix) {
		return notLike( escapeLikePattern( prefix ) + "%", LIKE_ESCAPE_CHARACTER );
	}

	@Nonnull
	@Override
	default SqmPredicate endsWith(@Nonnull String suffix) {
		return like( "%" + escapeLikePattern( suffix ), LIKE_ESCAPE_CHARACTER );
	}

	@Nonnull
	@Override
	default SqmPredicate notEndsWith(@Nonnull String suffix) {
		return notLike( "%" + escapeLikePattern( suffix ), LIKE_ESCAPE_CHARACTER );
	}

	@Nonnull
	@Override
	default SqmTextExpression append(@Nonnull Expression<String> y) {
		return wrapTextExpression( nodeBuilder().concat( this, y ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression append(@Nonnull String y) {
		return wrapTextExpression( nodeBuilder().concat( this, y ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression prepend(@Nonnull Expression<String> y) {
		return wrapTextExpression( nodeBuilder().concat( y, this ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression prepend(@Nonnull String y) {
		return wrapTextExpression( nodeBuilder().concat( y, this ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression substring(@Nonnull Expression<Integer> from) {
		return wrapTextExpression( nodeBuilder().substring( this, from ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression substring(int from) {
		return wrapTextExpression( nodeBuilder().substring( this, from ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression substring(@Nonnull Expression<Integer> from, @Nonnull Expression<Integer> len) {
		return wrapTextExpression( nodeBuilder().substring( this, from, len ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression substring(int from, int len) {
		return wrapTextExpression( nodeBuilder().substring( this, from, len ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression trim() {
		return wrapTextExpression( nodeBuilder().trim( this ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression trim(@Nonnull CriteriaBuilder.Trimspec ts) {
		return wrapTextExpression( nodeBuilder().trim( ts, this ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression trim(@Nonnull Expression<Character> t) {
		return wrapTextExpression( nodeBuilder().trim( t, this ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression trim(@Nonnull CriteriaBuilder.Trimspec ts, @Nonnull Expression<Character> t) {
		return wrapTextExpression( nodeBuilder().trim( ts, t, this ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression trim(char t) {
		return wrapTextExpression( nodeBuilder().trim( t, this ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression trim(@Nonnull CriteriaBuilder.Trimspec ts, char t) {
		return wrapTextExpression( nodeBuilder().trim( ts, t, this ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression lower() {
		return wrapTextExpression( nodeBuilder().lower( this ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression upper() {
		return wrapTextExpression( nodeBuilder().upper( this ) );
	}

	@Nonnull
	@Override
	default SqmExpression<Integer> length() {
		return nodeBuilder().length( this );
	}

	@Nonnull
	@Override
	default SqmTextExpression left(int len) {
		return wrapTextExpression( nodeBuilder().left( this, len ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression right(int len) {
		return wrapTextExpression( nodeBuilder().right( this, len ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression left(@Nonnull Expression<Integer> len) {
		return wrapTextExpression( nodeBuilder().left( this, len ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression right(@Nonnull Expression<Integer> len) {
		return wrapTextExpression( nodeBuilder().right( this, len ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression replace(@Nonnull Expression<String> substring, @Nonnull Expression<String> replacement) {
		return wrapTextExpression( nodeBuilder().replace( this, substring, replacement ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression replace(@Nonnull String substring, @Nonnull Expression<String> replacement) {
		return wrapTextExpression( nodeBuilder().replace( this, substring, replacement ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression replace(@Nonnull Expression<String> substring, @Nonnull String replacement) {
		return wrapTextExpression( nodeBuilder().replace( this, substring, replacement ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression replace(@Nonnull String substring, @Nonnull String replacement) {
		return wrapTextExpression( nodeBuilder().replace( this, substring, replacement ) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Integer> locate(@Nonnull Expression<String> pattern) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().locate( this, pattern ) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Integer> locate(@Nonnull String pattern) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().locate( this, pattern ) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Integer> locate(@Nonnull Expression<String> pattern, @Nonnull Expression<Integer> from) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().locate( this, pattern, from ) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Integer> locate(@Nonnull String pattern, int from) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().locate( this, pattern, from ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression coalesce(@Nonnull Expression<? extends String> y) {
		return new SqmTextExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression coalesce(String y) {
		return new SqmTextExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression nullif(@Nonnull Expression<? extends String> y) {
		return new SqmTextExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	default SqmTextExpression nullif(String y) {
		return new SqmTextExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	private static String escapeLikePattern(String value) {
		return value
				.replace( "\\", "\\\\" )
				.replace( "%", "\\%" )
				.replace( "_", "\\_" );
	}

	private static SqmTextExpression wrapTextExpression(SqmExpression<String> expression) {
		return expression instanceof SqmTextExpression textExpression
				? textExpression
				: new SqmTextExpressionWrapper( expression );
	}
}
