/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// One ordered index term, either a resolved column or an opaque SQL expression.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public sealed interface IndexTermNamingInput {
	/// Source spelling after trimming and removing the trailing ordering keyword.
	/// Retained for default-name compatibility; column references may have resolved
	/// using their logical or physical spelling, an entity attribute path, or an
	/// explicit collection role such as `{element}.city`.
	@Nonnull String sourceText();

	/// Requested direction, including the distinction between absent and explicit ascending.
	@Nonnull Order order();

	/// Ordering specified on an index term.
	@SPI(SPI.Role.USE)
	enum Order { UNSPECIFIED, ASC, DESC }

	/// A reference resolved to an actual table column.
	///
	/// @param names Selected logical declaration/reference and finalized physical column name
	/// @param sourceText Original reference spelling without ordering, including column, attribute, or collection-role syntax
	/// @param order Requested direction
	/// @author Steve Ebersole
	@SPI(SPI.Role.USE)
	record ColumnTerm(@Nonnull NamingNamePair names, @Nonnull String sourceText, @Nonnull Order order)
			implements IndexTermNamingInput {
		public ColumnTerm {
			requireNonNull( names, "names" );
			requireNonNull( sourceText, "sourceText" );
			requireNonNull( order, "order" );
		}
	}

	/// An SQL expression, not an identifier and not subject to physical column naming.
	///
	/// @param sourceText Opaque expression text, including its enclosing parentheses
	/// @param order Requested direction
	/// @author Steve Ebersole
	@SPI(SPI.Role.USE)
	record ExpressionTerm(@Nonnull String sourceText, @Nonnull Order order) implements IndexTermNamingInput {
		public ExpressionTerm {
			requireNonNull( sourceText, "sourceText" );
			requireNonNull( order, "order" );
		}
	}
}
