/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowid.internal;

import jakarta.annotation.Nullable;

import org.hibernate.dialect.rowid.spi.RowIdSupport;

/// Built-in immutable row-id profiles.
///
/// @author Steve Ebersole
/// @since 8.0
public final class StandardRowIdSupport implements RowIdSupport {
	private enum Kind { NONE, FIXED, REQUESTED }

	private static final RowIdSupport NONE = new StandardRowIdSupport( Kind.NONE, null, 0, null );

	private final Kind kind;
	private final String expression;
	private final int sqlTypeCode;
	private final String columnDefinitionSuffix;

	private StandardRowIdSupport(
			Kind kind,
			@Nullable String expression,
			int sqlTypeCode,
			@Nullable String columnDefinitionSuffix) {
		this.kind = kind;
		this.expression = expression;
		this.sqlTypeCode = sqlTypeCode;
		this.columnDefinitionSuffix = columnDefinitionSuffix;
	}

	public static RowIdSupport none() {
		return NONE;
	}

	public static RowIdSupport fixed(String expression, int sqlTypeCode) {
		if ( expression == null || expression.isEmpty() ) {
			throw new IllegalArgumentException( "Fixed row-id expression must not be null or empty" );
		}
		return new StandardRowIdSupport( Kind.FIXED, expression, sqlTypeCode, null );
	}

	public static RowIdSupport requestedName(
			@Nullable String defaultExpression,
			int sqlTypeCode,
			@Nullable String columnDefinitionSuffix) {
		return new StandardRowIdSupport( Kind.REQUESTED, emptyToNull( defaultExpression ), sqlTypeCode, columnDefinitionSuffix );
	}

	@Override
	public boolean isSupported() {
		return kind != Kind.NONE;
	}

	@Override
	public @Nullable String resolveExpression(@Nullable String requestedName) {
		return switch ( kind ) {
			case NONE -> null;
			case FIXED -> expression;
			case REQUESTED -> requestedName == null || requestedName.isEmpty() ? expression : requestedName;
		};
	}

	@Override
	public int sqlTypeCode() {
		if ( kind == Kind.NONE ) {
			throw new UnsupportedOperationException( "Row-id is not supported" );
		}
		return sqlTypeCode;
	}

	@Override
	public @Nullable String columnDefinition(@Nullable String requestedName) {
		if ( kind != Kind.REQUESTED || columnDefinitionSuffix == null ) {
			return null;
		}
		final String resolved = resolveExpression( requestedName );
		return resolved == null ? null : resolved + columnDefinitionSuffix;
	}

	private static @Nullable String emptyToNull(@Nullable String value) {
		return value == null || value.isEmpty() ? null : value;
	}
}
