/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.translation.Clause;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Determines whether resolving a grouping or ordering expression requires a
/// synthetic table root in the current SQL AST query specification.
///
/// Implementations must be immutable or thread-safe and should return a stable
/// descriptor for all triggering expressions in one query specification. The
/// standard SQM converter adds at most one root per query specification and
/// rejects incompatible descriptors returned for that specification.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getSyntheticTableGroupSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
@FunctionalInterface
public interface SyntheticTableGroupSupport {
	/// A strategy which never requests a synthetic table root.
	SyntheticTableGroupSupport NONE = (clause, expression) -> {
		Objects.requireNonNull( clause, "clause" );
		return null;
	};

	/// The standard derived-table root used by databases which require a `from`
	/// clause for literal grouping and ordering expressions.
	SyntheticTableGroupSupport SELECT_ONE_FOR_LITERALS =
			forLiteralExpressions( "(select 1)", "dummy_(x)" );

	/// Create a strategy which requests the given table root whenever a resolved
	/// grouping or ordering expression is a SQL AST literal.
	static SyntheticTableGroupSupport forLiteralExpressions(
			String tableExpression,
			@Nullable String identificationVariable) {
		final var descriptor = new SyntheticTableGroupDescriptor( tableExpression, identificationVariable );
		return (clause, expression) -> {
			Objects.requireNonNull( clause, "clause" );
			return expression instanceof Literal ? descriptor : null;
		};
	}

	/// Return the synthetic table root required for the resolved expression, or
	/// `null` when the current query specification requires no synthetic root.
	///
	/// The clause is the converter context in which the grouping or ordering
	/// expression was resolved. The expression may be `null` when a positional
	/// reference resolves to multiple selections; this does not require a root.
	/// Implementations may use the clause to distinguish normal grouping and
	/// ordering from ordering within a window.
	@Nullable
	SyntheticTableGroupDescriptor resolveSyntheticTableGroup(Clause clause, @Nullable Expression expression);
}
