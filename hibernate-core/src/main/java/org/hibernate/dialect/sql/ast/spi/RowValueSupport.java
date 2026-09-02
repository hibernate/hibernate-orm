/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.EnumSet;
import java.util.Set;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.DISTINCTNESS_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.EQUALITY_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.IN_LIST;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.IN_SUBQUERY;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.ORDERING_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.QUANTIFIED_COMPARISON;

/// Immutable description of a Dialect's native row-value syntax.
///
/// Providers supply a stable profile through
/// [org.hibernate.dialect.Dialect#getRowValueSupport]. Each [Feature] reports
/// one exact syntax form or predicate context. Do not infer one context from
/// another: a database may, for example, support row values in an `IN`
/// subquery while rejecting an `IN` list. Explicit `row(a, b)` construction is
/// likewise independent from parenthesized row-value predicates.
///
/// Ordering and distinctness comparisons refine equality comparison and are
/// validated against that prerequisite. Native row distinctness additionally
/// requires scalar
/// [PredicateSupport.Capability#DISTINCT_FROM] at the rendering site.
///
/// @see org.hibernate.dialect.Dialect#getRowValueSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class RowValueSupport {
	/// No native row-value syntax.
	public static final RowValueSupport NONE = new RowValueSupport( Set.of() );

	/// The base-Dialect profile: every predicate feature, without explicit
	/// `row(a, b)` constructor syntax.
	public static final RowValueSupport STANDARD = new RowValueSupport(
			Set.of(
					EQUALITY_COMPARISON,
					ORDERING_COMPARISON,
					DISTINCTNESS_COMPARISON,
					IN_LIST,
					IN_SUBQUERY,
					QUANTIFIED_COMPARISON
			)
	);

	private final Set<Feature> features;

	private RowValueSupport(Set<Feature> features) {
		this.features = Set.copyOf( features );
		validate();
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized with every feature from the given profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(RowValueSupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// The immutable set of supported row-value features.
	public Set<Feature> getFeatures() {
		return features;
	}

	/// Whether the given row-value feature is supported natively.
	public boolean supports(Feature feature) {
		return features.contains( requireArgument( feature, "feature" ) );
	}

	private void validate() {
		if ( features.contains( ORDERING_COMPARISON ) && !features.contains( EQUALITY_COMPARISON ) ) {
			throw new IllegalArgumentException( "ORDERING_COMPARISON requires EQUALITY_COMPARISON" );
		}
		if ( features.contains( DISTINCTNESS_COMPARISON ) && !features.contains( EQUALITY_COMPARISON ) ) {
			throw new IllegalArgumentException( "DISTINCTNESS_COMPARISON requires EQUALITY_COMPARISON" );
		}
	}

	/// An independently configurable row-value syntax feature.
	public enum Feature {
		/// Explicit SQL row construction such as `row(a, b)`, independently of
		/// parenthesized row values such as `(a, b)`.
		ROW_CONSTRUCTOR,

		/// Row equality and inequality such as `(a, b) = (c, d)` and
		/// `(a, b) <> (c, d)`.
		EQUALITY_COMPARISON,

		/// Lexicographic row ordering with `<`, `<=`, `>`, or `>=`.
		ORDERING_COMPARISON,

		/// Null-safe row comparison with `is distinct from` and
		/// `is not distinct from`.
		DISTINCTNESS_COMPARISON,

		/// A row-valued test expression and row-valued entries in an `IN` list,
		/// for example `(a, b) in ((1, 2), (3, 4))`.
		IN_LIST,

		/// A row-valued test expression with a matching multi-column subquery,
		/// for example `(a, b) in (select x, y ...)`.
		IN_SUBQUERY,

		/// A row comparison against an `ALL`, `ANY`, or `SOME` subquery, for
		/// example `(a, b) = any (select x, y ...)`.
		QUANTIFIED_COMPARISON
	}

	/// Build an immutable row-value-support profile.
	public static final class Builder {
		private final EnumSet<Feature> features;

		private Builder(RowValueSupport base) {
			features = base.features.isEmpty()
					? EnumSet.noneOf( Feature.class )
					: EnumSet.copyOf( base.features );
		}

		/// Enable the given row-value features without changing other features.
		public Builder features(Feature... features) {
			requireArgument( features, "features" );
			for ( Feature feature : features ) {
				feature( feature, true );
			}
			return this;
		}

		/// Enable or disable one row-value feature.
		public Builder feature(Feature feature, boolean supported) {
			requireArgument( feature, "feature" );
			if ( supported ) {
				features.add( feature );
			}
			else {
				features.remove( feature );
			}
			return this;
		}

		/// Build and validate an immutable snapshot of this builder.
		public RowValueSupport build() {
			return new RowValueSupport( features );
		}
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
