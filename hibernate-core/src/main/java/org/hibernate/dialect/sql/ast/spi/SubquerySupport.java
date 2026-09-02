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

/// Immutable description of the subquery placements supported by a Dialect.
///
/// Providers supply a stable profile through
/// [org.hibernate.dialect.Dialect#getSubquerySupport]. Each feature answers one
/// independent placement question. In particular, [Feature#SELECT_LIST]
/// reports scalar-subquery placement and does not imply
/// [Feature#EXISTS_IN_SELECT], which reports the specialized placement of an
/// `exists` predicate in the select list.
///
/// [Feature#LATERAL] reports native or proprietary correlated-derived-table
/// support. The SQL AST translator remains responsible for spelling and any
/// fallback rendering.
///
/// @see org.hibernate.dialect.Dialect#getSubquerySupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class SubquerySupport {
	/// No supported subquery placement.
	public static final SubquerySupport NONE = new SubquerySupport( Set.of() );

	/// The base-Dialect profile. Offset in subqueries and lateral derived tables
	/// are disabled; all other placements are enabled.
	public static final SubquerySupport STANDARD = new SubquerySupport(
			Set.of(
					Feature.SELECT_LIST,
					Feature.EXISTS_IN_SELECT,
					Feature.ORDER_BY,
					Feature.NESTED_CORRELATION,
					Feature.MUTATION_TARGET_REFERENCE,
					Feature.MUTATION_JOIN,
					Feature.IN_PREDICATE_LHS
			)
	);

	private final Set<Feature> features;

	private SubquerySupport(Set<Feature> features) {
		this.features = Set.copyOf( features );
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized with the complete feature set from the given
	/// profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(SubquerySupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// The immutable set of supported subquery placements.
	public Set<Feature> getFeatures() {
		return features;
	}

	/// Whether the given subquery placement is supported.
	public boolean supports(Feature feature) {
		return features.contains( requireArgument( feature, "feature" ) );
	}

	/// An independently configurable subquery placement.
	public enum Feature {
		/// A scalar subquery may occur in the select list.
		SELECT_LIST,

		/// An `exists` predicate may occur in the select list.
		EXISTS_IN_SELECT,

		/// A subquery may contain an `order by` clause.
		ORDER_BY,

		/// Pagination may be pushed into a subquery using an offset.
		OFFSET,

		/// A subquery may correlate across more than one enclosing query level.
		NESTED_CORRELATION,

		/// A mutation statement's subquery may reference the mutation target.
		MUTATION_TARGET_REFERENCE,

		/// A mutation-statement subquery may contain joins.
		MUTATION_JOIN,

		/// A subquery may occur as the left operand of an `in` predicate.
		IN_PREDICATE_LHS,

		/// A correlated derived table may use native or proprietary lateral syntax.
		LATERAL
	}

	/// Build an immutable subquery-support profile.
	public static final class Builder {
		private final EnumSet<Feature> features;

		private Builder(SubquerySupport base) {
			features = base.features.isEmpty()
					? EnumSet.noneOf( Feature.class )
					: EnumSet.copyOf( base.features );
		}

		/// Enable the given features without changing other features.
		public Builder features(Feature... features) {
			requireArgument( features, "features" );
			for ( Feature feature : features ) {
				feature( feature, true );
			}
			return this;
		}

		/// Enable or disable exactly one feature.
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

		/// Build an immutable snapshot of this builder.
		public SubquerySupport build() {
			return new SubquerySupport( features );
		}
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
