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

/// Immutable description of a Dialect's common-table-expression (CTE) capabilities.
///
/// Placement levels are hierarchical. Recursive and mutation refinements are
/// validated against their prerequisites when a profile is built.
///
/// Providers supply a profile by overriding
/// [org.hibernate.dialect.Dialect#getCteSupport]. The
/// profile is immutable and thread-safe, has no initialization or shutdown
/// lifecycle, and may be cached and reused by Hibernate. A Dialect may return
/// the same instance or equivalent instances, but its capabilities must remain
/// stable for the lifetime of the Dialect. Invalid combinations fail during
/// construction with an [IllegalArgumentException].
///
/// @see org.hibernate.dialect.Dialect#getCteSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class CteSupport {
	/// A profile for a database with no WITH-clause support.
	public static final CteSupport NONE = builder().placement( Placement.NONE ).build();
	/// The base Dialect profile: nested WITH clauses, without recursive or mutation features.
	public static final CteSupport STANDARD = builder().build();

	private final Placement placement;
	private final Set<RecursiveFeature> recursiveFeatures;
	private final Set<MutationFeature> mutationFeatures;
	private final boolean headerColumnList;
	private final boolean recursiveKeyword;
	private final boolean recursiveClauseArrayAndRowEmulation;

	private CteSupport(Builder builder) {
		placement = builder.placement;
		recursiveFeatures = Set.copyOf( builder.recursiveFeatures );
		mutationFeatures = Set.copyOf( builder.mutationFeatures );
		headerColumnList = builder.headerColumnList;
		recursiveKeyword = builder.recursiveKeyword;
		recursiveClauseArrayAndRowEmulation = builder.recursiveClauseArrayAndRowEmulation;
		validate();
	}

	/// Create a builder initialized with the base Dialect profile.
	public static Builder builder() {
		return new Builder();
	}

	/// Create a builder initialized as a copy of the given profile.
	public static Builder builder(CteSupport base) {
		return new Builder( base );
	}

	/// The strongest supported WITH-clause placement.
	public Placement getPlacement() {
		return placement;
	}

	/// The immutable set of recursive features.
	public Set<RecursiveFeature> getRecursiveFeatures() {
		return recursiveFeatures;
	}

	/// Whether the given recursive feature is supported.
	public boolean supports(RecursiveFeature feature) {
		return recursiveFeatures.contains( feature );
	}

	/// The immutable set of mutation features.
	public Set<MutationFeature> getMutationFeatures() {
		return mutationFeatures;
	}

	/// Whether the given mutation feature is supported.
	public boolean supports(MutationFeature feature) {
		return mutationFeatures.contains( feature );
	}

	/// Whether a WITH clause is supported in any placement.
	public boolean supportsWithClause() {
		return placement != Placement.NONE;
	}

	/// Whether a WITH clause may be used in a subquery.
	public boolean supportsWithClauseInSubquery() {
		return placement.includes( Placement.SUBQUERY );
	}

	/// Whether a WITH clause may be nested inside another CTE.
	public boolean supportsNestedWithClause() {
		return placement.includes( Placement.NESTED );
	}

	/// Whether CTE column names may be declared in the CTE header.
	public boolean supportsCteHeaderColumnList() {
		return headerColumnList;
	}

	/// Whether recursive CTE syntax requires a `recursive` keyword.
	public boolean requiresRecursiveKeyword() {
		return recursiveKeyword;
	}

	/// Whether SEARCH/CYCLE emulation may use array and row constructors.
	public boolean supportsRecursiveClauseArrayAndRowEmulation() {
		return recursiveClauseArrayAndRowEmulation;
	}

	private void validate() {
		if ( placement == Placement.NONE && ( !recursiveFeatures.isEmpty() || !mutationFeatures.isEmpty() ) ) {
			throw new IllegalArgumentException( "CTE features require WITH-clause support" );
		}
		if ( !recursiveFeatures.contains( RecursiveFeature.RECURSIVE )
				&& recursiveFeatures.size() > 0 ) {
			throw new IllegalArgumentException( "Recursive CTE refinements require RECURSIVE support" );
		}
		if ( recursiveFeatures.contains( RecursiveFeature.CYCLE_USING )
				&& !recursiveFeatures.contains( RecursiveFeature.CYCLE ) ) {
			throw new IllegalArgumentException( "CYCLE_USING requires CYCLE support" );
		}
		if ( mutationFeatures.contains( MutationFeature.INSERT_CONFLICT )
				&& !mutationFeatures.contains( MutationFeature.NON_QUERY ) ) {
			throw new IllegalArgumentException( "INSERT_CONFLICT requires NON_QUERY CTE support" );
		}
	}

	/// The strongest location in which a WITH clause may occur.
	public enum Placement {
		NONE,
		TOP_LEVEL,
		SUBQUERY,
		NESTED;

		/// Whether this placement includes the required placement.
		public boolean includes(Placement required) {
			return ordinal() >= required.ordinal();
		}
	}

	/// Native recursive-CTE syntax capabilities.
	public enum RecursiveFeature {
		RECURSIVE,
		SEARCH,
		CYCLE,
		CYCLE_USING
	}

	/// Mutation-specific CTE capabilities.
	public enum MutationFeature {
		NON_QUERY,
		INSERT_CONFLICT
	}

	/// Builder for immutable [CteSupport] profiles.
	public static final class Builder {
		private Placement placement = Placement.NESTED;
		private final EnumSet<RecursiveFeature> recursiveFeatures = EnumSet.noneOf( RecursiveFeature.class );
		private final EnumSet<MutationFeature> mutationFeatures = EnumSet.noneOf( MutationFeature.class );
		private boolean headerColumnList = true;
		private boolean recursiveKeyword = true;
		private boolean recursiveClauseArrayAndRowEmulation = true;

		private Builder() {
		}

		private Builder(CteSupport base) {
		if ( base == null ) {
			throw new IllegalArgumentException( "Base CTE support profile must not be null" );
		}
		placement = base.placement;
			recursiveFeatures.addAll( base.recursiveFeatures );
			mutationFeatures.addAll( base.mutationFeatures );
			headerColumnList = base.headerColumnList;
			recursiveKeyword = base.recursiveKeyword;
			recursiveClauseArrayAndRowEmulation = base.recursiveClauseArrayAndRowEmulation;
		}

		/// Set the strongest supported WITH-clause placement.
		public Builder placement(Placement placement) {
			if ( placement == null ) {
				throw new IllegalArgumentException( "CTE placement must not be null" );
			}
			this.placement = placement;
			return this;
		}

		/// Replace the recursive-feature set.
		public Builder recursiveFeatures(RecursiveFeature... features) {
			recursiveFeatures.clear();
			recursiveFeatures.addAll( Set.of( features ) );
			return this;
		}

		/// Add or remove one recursive feature according to a condition.
		public Builder recursiveFeature(RecursiveFeature feature, boolean supported) {
			if ( feature == null ) {
				throw new IllegalArgumentException( "Recursive CTE feature must not be null" );
			}
			if ( supported ) {
				recursiveFeatures.add( feature );
			}
			else {
				recursiveFeatures.remove( feature );
			}
			return this;
		}

		/// Replace the mutation-feature set.
		public Builder mutationFeatures(MutationFeature... features) {
			mutationFeatures.clear();
			mutationFeatures.addAll( Set.of( features ) );
			return this;
		}

		/// Configure CTE-header column-list support.
		public Builder supportsCteHeaderColumnList(boolean supported) {
			headerColumnList = supported;
			return this;
		}

		/// Configure whether recursive syntax requires its keyword.
		public Builder requiresRecursiveKeyword(boolean required) {
			recursiveKeyword = required;
			return this;
		}

		/// Configure whether array/row recursive-clause emulation is allowed.
		public Builder supportsRecursiveClauseArrayAndRowEmulation(boolean supported) {
			recursiveClauseArrayAndRowEmulation = supported;
			return this;
		}

		/// Build and validate the immutable profile.
		public CteSupport build() {
			return new CteSupport( this );
		}
	}
}
