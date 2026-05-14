/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

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

	@Override
	default SqmPredicate like(Expression<String> pattern) {
		return nodeBuilder().like( this, pattern );
	}

	@Override
	default SqmPredicate like(String pattern) {
		return nodeBuilder().like( this, pattern );
	}

	@Override
	default SqmPredicate like(Expression<String> pattern, Expression<Character> escapeChar) {
		return nodeBuilder().like( this, pattern, escapeChar );
	}

	@Override
	default SqmPredicate like(Expression<String> pattern, char escapeChar) {
		return nodeBuilder().like( this, pattern, escapeChar );
	}

	@Override
	default SqmPredicate like(String pattern, Expression<Character> escapeChar) {
		return nodeBuilder().like( this, pattern, escapeChar );
	}

	@Override
	default SqmPredicate like(String pattern, char escapeChar) {
		return nodeBuilder().like( this, pattern, escapeChar );
	}

	@Override
	default SqmPredicate notLike(Expression<String> pattern) {
		return nodeBuilder().notLike( this, pattern );
	}

	@Override
	default SqmPredicate notLike(String pattern) {
		return nodeBuilder().notLike( this, pattern );
	}

	@Override
	default SqmPredicate notLike(Expression<String> pattern, Expression<Character> escapeChar) {
		return nodeBuilder().notLike( this, pattern, escapeChar );
	}

	@Override
	default SqmPredicate notLike(Expression<String> pattern, char escapeChar) {
		return nodeBuilder().notLike( this, pattern, escapeChar );
	}

	@Override
	default SqmPredicate notLike(String pattern, Expression<Character> escapeChar) {
		return nodeBuilder().notLike( this, pattern, escapeChar );
	}

	@Override
	default SqmPredicate notLike(String pattern, char escapeChar) {
		return nodeBuilder().notLike( this, pattern, escapeChar );
	}

	@Override
	default SqmPredicate contains(String substring) {
		return like( "%" + escapeLikePattern( substring ) + "%", LIKE_ESCAPE_CHARACTER );
	}

	@Override
	default SqmPredicate notContains(String substring) {
		return notLike( "%" + escapeLikePattern( substring ) + "%", LIKE_ESCAPE_CHARACTER );
	}

	@Override
	default SqmPredicate startsWith(String prefix) {
		return like( escapeLikePattern( prefix ) + "%", LIKE_ESCAPE_CHARACTER );
	}

	@Override
	default SqmPredicate notStartsWith(String prefix) {
		return notLike( escapeLikePattern( prefix ) + "%", LIKE_ESCAPE_CHARACTER );
	}

	@Override
	default SqmPredicate endsWith(String suffix) {
		return like( "%" + escapeLikePattern( suffix ), LIKE_ESCAPE_CHARACTER );
	}

	@Override
	default SqmPredicate notEndsWith(String suffix) {
		return notLike( "%" + escapeLikePattern( suffix ), LIKE_ESCAPE_CHARACTER );
	}

	@Override
	default SqmTextExpression append(Expression<String> y) {
		return wrapTextExpression( nodeBuilder().concat( this, y ) );
	}

	@Override
	default SqmTextExpression append(String y) {
		return wrapTextExpression( nodeBuilder().concat( this, y ) );
	}

	@Override
	default SqmTextExpression prepend(Expression<String> y) {
		return wrapTextExpression( nodeBuilder().concat( y, this ) );
	}

	@Override
	default SqmTextExpression prepend(String y) {
		return wrapTextExpression( nodeBuilder().concat( y, this ) );
	}

	@Override
	default SqmTextExpression substring(Expression<Integer> from) {
		return wrapTextExpression( nodeBuilder().substring( this, from ) );
	}

	@Override
	default SqmTextExpression substring(int from) {
		return wrapTextExpression( nodeBuilder().substring( this, from ) );
	}

	@Override
	default SqmTextExpression substring(Expression<Integer> from, Expression<Integer> len) {
		return wrapTextExpression( nodeBuilder().substring( this, from, len ) );
	}

	@Override
	default SqmTextExpression substring(int from, int len) {
		return wrapTextExpression( nodeBuilder().substring( this, from, len ) );
	}

	@Override
	default SqmTextExpression trim() {
		return wrapTextExpression( nodeBuilder().trim( this ) );
	}

	@Override
	default SqmTextExpression trim(CriteriaBuilder.Trimspec ts) {
		return wrapTextExpression( nodeBuilder().trim( ts, this ) );
	}

	@Override
	default SqmTextExpression trim(Expression<Character> t) {
		return wrapTextExpression( nodeBuilder().trim( t, this ) );
	}

	@Override
	default SqmTextExpression trim(CriteriaBuilder.Trimspec ts, Expression<Character> t) {
		return wrapTextExpression( nodeBuilder().trim( ts, t, this ) );
	}

	@Override
	default SqmTextExpression trim(char t) {
		return wrapTextExpression( nodeBuilder().trim( t, this ) );
	}

	@Override
	default SqmTextExpression trim(CriteriaBuilder.Trimspec ts, char t) {
		return wrapTextExpression( nodeBuilder().trim( ts, t, this ) );
	}

	@Override
	default SqmTextExpression lower() {
		return wrapTextExpression( nodeBuilder().lower( this ) );
	}

	@Override
	default SqmTextExpression upper() {
		return wrapTextExpression( nodeBuilder().upper( this ) );
	}

	@Override
	default SqmExpression<Integer> length() {
		return nodeBuilder().length( this );
	}

	@Override
	default SqmTextExpression left(int len) {
		return wrapTextExpression( nodeBuilder().left( this, len ) );
	}

	@Override
	default SqmTextExpression right(int len) {
		return wrapTextExpression( nodeBuilder().right( this, len ) );
	}

	@Override
	default SqmTextExpression left(Expression<Integer> len) {
		return wrapTextExpression( nodeBuilder().left( this, len ) );
	}

	@Override
	default SqmTextExpression right(Expression<Integer> len) {
		return wrapTextExpression( nodeBuilder().right( this, len ) );
	}

	@Override
	default SqmTextExpression replace(Expression<String> substring, Expression<String> replacement) {
		return wrapTextExpression( nodeBuilder().replace( this, substring, replacement ) );
	}

	@Override
	default SqmTextExpression replace(String substring, Expression<String> replacement) {
		return wrapTextExpression( nodeBuilder().replace( this, substring, replacement ) );
	}

	@Override
	default SqmTextExpression replace(Expression<String> substring, String replacement) {
		return wrapTextExpression( nodeBuilder().replace( this, substring, replacement ) );
	}

	@Override
	default SqmTextExpression replace(String substring, String replacement) {
		return wrapTextExpression( nodeBuilder().replace( this, substring, replacement ) );
	}

	@Override
	default SqmNumericExpression<Integer> locate(Expression<String> pattern) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().locate( this, pattern ) );
	}

	@Override
	default SqmNumericExpression<Integer> locate(String pattern) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().locate( this, pattern ) );
	}

	@Override
	default SqmNumericExpression<Integer> locate(Expression<String> pattern, Expression<Integer> from) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().locate( this, pattern, from ) );
	}

	@Override
	default SqmNumericExpression<Integer> locate(String pattern, int from) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().locate( this, pattern, from ) );
	}

	@Override
	default SqmTextExpression coalesce(Expression<? extends String> y) {
		return new SqmTextExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	default SqmTextExpression coalesce(String y) {
		return new SqmTextExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	default SqmTextExpression nullif(Expression<? extends String> y) {
		return new SqmTextExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

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
