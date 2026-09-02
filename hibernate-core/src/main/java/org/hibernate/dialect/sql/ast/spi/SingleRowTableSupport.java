/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable description of the SQL used by a Dialect for a reusable
/// single-row table and for an otherwise table-less select.
///
/// Providers supply a stable profile through
/// [org.hibernate.dialect.Dialect#getSingleRowTableSupport]. The table
/// expression is used where SQL requires a reusable one-row table expression.
/// The select-only `from` clause is a complete fragment, including any alias or
/// restriction required by the database; an empty fragment means that an
/// otherwise table-less select requires no `from` clause.
///
/// Consumers must select the accessor matching the rendering context and must
/// not derive one value from the other. Providers refining a Dialect family
/// profile should copy the profile returned by the superclass and change only
/// the values which differ.
///
/// @see org.hibernate.dialect.Dialect#getSingleRowTableSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class SingleRowTableSupport {
	/// The base-Dialect profile: a standard values-table expression and no
	/// select-only `from` clause.
	public static final SingleRowTableSupport STANDARD = new SingleRowTableSupport(
			"(values(0))",
			""
	);

	private final String tableExpression;
	private final String selectOnlyFromClause;

	private SingleRowTableSupport(String tableExpression, String selectOnlyFromClause) {
		this.tableExpression = requireTableExpression( tableExpression );
		this.selectOnlyFromClause = requireArgument( selectOnlyFromClause, "selectOnlyFromClause" );
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized with both rendering values of the given
	/// profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(SingleRowTableSupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// The nonblank table expression representing exactly one row.
	public String getTableExpression() {
		return tableExpression;
	}

	/// The complete `from` fragment for an otherwise table-less select, or the
	/// empty string when no fragment is required.
	public String getSelectOnlyFromClause() {
		return selectOnlyFromClause;
	}

	/// Build an immutable single-row-table-support profile.
	public static final class Builder {
		private String tableExpression;
		private String selectOnlyFromClause;

		private Builder(SingleRowTableSupport base) {
			tableExpression = base.tableExpression;
			selectOnlyFromClause = base.selectOnlyFromClause;
		}

		/// Set the reusable nonblank one-row table expression exactly as it should
		/// be rendered.
		public Builder tableExpression(String expression) {
			tableExpression = requireTableExpression( expression );
			return this;
		}

		/// Set the complete select-only `from` fragment exactly as it should be
		/// rendered. An empty fragment means no `from` clause is required.
		public Builder selectOnlyFromClause(String fragment) {
			selectOnlyFromClause = requireArgument( fragment, "fragment" );
			return this;
		}

		/// Build an immutable snapshot of this builder.
		public SingleRowTableSupport build() {
			return new SingleRowTableSupport( tableExpression, selectOnlyFromClause );
		}
	}

	private static String requireTableExpression(String expression) {
		requireArgument( expression, "expression" );
		if ( expression.isBlank() ) {
			throw new IllegalArgumentException( "expression must not be blank" );
		}
		return expression;
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
