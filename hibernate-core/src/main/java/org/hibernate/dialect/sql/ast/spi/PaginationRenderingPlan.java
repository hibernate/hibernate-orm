/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// Immutable semantic plan for SQL AST pagination rendering.
///
/// The plan describes syntax and placement. It does not own or receive mutable
/// translator state. Providers select a plan; [AbstractSqlAstTranslator]
/// performs the actual traversal and rendering.
///
/// @since 8.0
/// @author Steve Ebersole
public sealed interface PaginationRenderingPlan {
	/// ANSI offset/fetch syntax rendered at the end of the query part.
	///
	/// For offset 5 and fetch 10, the usual shape is:
	///
	/// ```sql
	/// select ... order by ... offset 5 rows fetch first 10 rows only
	/// ```
	///
	/// @param renderOffsetRowsKeyword whether an existing offset expression is
	/// followed by the `rows` keyword; this flag does not synthesize an offset
	record OffsetFetch(boolean renderOffsetRowsKeyword) implements PaginationRenderingPlan {
	}

	/// Separate trailing `limit` and `offset` clauses, for example
	/// `select ... limit 10 offset 5`.
	record LimitOffset() implements PaginationRenderingPlan {
	}

	/// A combined trailing limit clause whose first operand is the offset and
	/// second operand is the row count, for example `select ... limit 5, 10`.
	record CombinedLimit() implements PaginationRenderingPlan {
	}

	/// A `top` clause rendered immediately after `select`.
	///
	/// Examples are `select top (10) ...` and, when `addOffset` is true,
	/// `select top (15) ...` for offset 5 and fetch 10. Since `top` cannot express
	/// the offset itself, Hibernate also skips the first five JDBC result rows in
	/// the latter case.
	///
	/// @param addOffset whether the bound value represents fetch plus offset
	/// @param parenthesize whether the bound value must be parenthesized
	record Top(boolean addOffset, boolean parenthesize) implements PaginationRenderingPlan {
	}

	/// A select-clause `top ... start at ...` form, for example
	/// `select top 10 start at 5 ...`.
	record TopStartAt() implements PaginationRenderingPlan {
	}

	/// A select-clause `first ... skip ...` form, for example
	/// `select first 10 skip 5 ...`.
	record FirstSkip() implements PaginationRenderingPlan {
	}

	/// A select-clause `skip ... first ...` form, for example
	/// `select skip 5 first 10 ...`.
	record SkipFirst() implements PaginationRenderingPlan {
	}

	/// A select-clause `first` form without native offset syntax.
	///
	/// For offset 5 and fetch 10, Hibernate renders `select first 15 ...` and
	/// skips the first five JDBC result rows.
	record First() implements PaginationRenderingPlan {
	}

	/// A trailing inclusive row range, for example `select ... rows 5 to 14` for
	/// a ten-row range beginning at the database-specific first-row value.
	record RowsTo() implements PaginationRenderingPlan {
	}

	/// A trailing fetch clause whose count is increased by the offset.
	///
	/// For offset 5 and fetch 10, Hibernate renders
	/// `select ... fetch first 15 rows only` and skips the first five JDBC result
	/// rows. This plan is useful when fetch is supported but offset is not.
	record FetchPlusOffset() implements PaginationRenderingPlan {
	}

	/// Emulate pagination by wrapping the query part and filtering a derived row
	/// number.
	///
	/// The precise projection and window function vary with the query, but the
	/// general shape for offset 5 and fetch 10 is:
	///
	/// ```sql
	/// select ...
	/// from (
	///     select ..., row_number() over (order by ...) as rn
	///     from ...
	/// ) page
	/// where page.rn > 5 and page.rn <= 15
	/// ```
	///
	/// @param emulateFetchClause whether the fetch clause itself requires window
	/// emulation, including semantics such as `with ties` or percentages, instead
	/// of emulating only its offset
	record Window(boolean emulateFetchClause) implements PaginationRenderingPlan {
	}

	/// No SQL AST pagination syntax. Execution-time JDBC instructions may still
	/// enforce a requested limit or offset using `PreparedStatement.setMaxRows()`
	/// or result-set row skipping. This plan does not mean that a requested limit
	/// may be silently ignored.
	record None() implements PaginationRenderingPlan {
	}
}
