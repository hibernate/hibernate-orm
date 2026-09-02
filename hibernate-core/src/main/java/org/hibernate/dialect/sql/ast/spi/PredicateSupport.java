/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.dialect.sql.ast.spi.PredicateSupport.Capability.EXPRESSION_PLACEMENT;

/// Immutable description of a Dialect's native predicate syntax and predicate
/// placement support.
///
/// Providers supply a stable profile through `Dialect#getPredicateSupport()`.
/// A present case-insensitive-`like` operator selects native rendering; an
/// absent operator selects lowercase-expression emulation. The remaining
/// capabilities independently select scalar `distinct from`, truthness
/// predicates, and use of predicates in value-expression contexts.
///
/// @see org.hibernate.dialect.Dialect#getPredicateSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class PredicateSupport {
	/// No native predicate syntax or predicate expression placement.
	public static final PredicateSupport NONE = new PredicateSupport( null, Set.of() );

	/// The standard base-Dialect profile, supporting predicate expression
	/// placement but no native case-insensitive-`like`, scalar `distinct from`,
	/// or truthness syntax.
	public static final PredicateSupport STANDARD = new PredicateSupport(
			null,
			Set.of( EXPRESSION_PLACEMENT )
	);

	private final String caseInsensitiveLikeOperator;
	private final Set<Capability> capabilities;

	private PredicateSupport(String caseInsensitiveLikeOperator, Set<Capability> capabilities) {
		this.caseInsensitiveLikeOperator = caseInsensitiveLikeOperator;
		this.capabilities = capabilities;
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized with every value from the given profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(PredicateSupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// The native case-insensitive-`like` operator, or an empty value when
	/// lowercase-expression emulation is required.
	public Optional<String> getCaseInsensitiveLikeOperator() {
		return Optional.ofNullable( caseInsensitiveLikeOperator );
	}

	/// The immutable set of supported predicate capabilities.
	public Set<Capability> getCapabilities() {
		return capabilities;
	}

	/// Whether the given predicate capability is supported.
	public boolean supports(Capability capability) {
		return capabilities.contains( requireArgument( capability, "capability" ) );
	}

	/// An independently configurable predicate capability.
	public enum Capability {
		/// Native scalar `is distinct from` and `is not distinct from` syntax.
		DISTINCT_FROM,

		/// Native `is true` and `is false` syntax.
		TRUTHNESS,

		/// Predicates may occur where the SQL grammar expects a value expression.
		EXPRESSION_PLACEMENT
	}

	/// Build an immutable predicate-support profile.
	public static final class Builder {
		private String caseInsensitiveLikeOperator;
		private final EnumSet<Capability> capabilities;

		private Builder(PredicateSupport base) {
			caseInsensitiveLikeOperator = base.caseInsensitiveLikeOperator;
			capabilities = base.capabilities.isEmpty()
					? EnumSet.noneOf( Capability.class )
					: EnumSet.copyOf( base.capabilities );
		}

		/// Select native case-insensitive-`like` rendering with the given SQL
		/// operator.
		///
		/// @param operator a non-null, non-blank SQL operator
		public Builder caseInsensitiveLikeOperator(String operator) {
			requireArgument( operator, "operator" );
			if ( operator.isBlank() ) {
				throw new IllegalArgumentException( "operator must not be blank" );
			}
			caseInsensitiveLikeOperator = operator;
			return this;
		}

		/// Remove the native case-insensitive-`like` operator so consumers use
		/// lowercase-expression emulation.
		public Builder noCaseInsensitiveLikeOperator() {
			caseInsensitiveLikeOperator = null;
			return this;
		}

		/// Enable the given predicate capabilities.
		public Builder capabilities(Capability... capabilities) {
			requireArgument( capabilities, "capabilities" );
			for ( Capability capability : capabilities ) {
				capability( capability, true );
			}
			return this;
		}

		/// Enable or disable a predicate capability.
		public Builder capability(Capability capability, boolean supported) {
			requireArgument( capability, "capability" );
			if ( supported ) {
				capabilities.add( capability );
			}
			else {
				capabilities.remove( capability );
			}
			return this;
		}

		/// Build an immutable snapshot of this builder.
		public PredicateSupport build() {
			return new PredicateSupport( caseInsensitiveLikeOperator, Set.copyOf( capabilities ) );
		}
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
