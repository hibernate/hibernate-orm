/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.array.spi;

import java.util.EnumSet;
import java.util.Set;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.dialect.array.spi.ArraySupport.Capability.ARRAY_CONSTRUCTOR;
import static org.hibernate.dialect.array.spi.ArraySupport.Capability.STANDARD_ARRAY;
import static org.hibernate.dialect.array.spi.ArraySupport.MultiValuedParameterStrategy.ARRAY;
import static org.hibernate.dialect.array.spi.ArraySupport.MultiValuedParameterStrategy.EXPANDED;

/// Immutable description of a Dialect's array syntax and multi-valued
/// parameter-binding behavior.
///
/// Providers supply a stable profile through `Dialect#getArraySupport()`.
/// Standard element-type array syntax, SQL array-constructor syntax, and the
/// multi-valued parameter-binding strategy are independent dimensions. Do not
/// infer support for one dimension from another: a database might bind a named
/// or nonstandard array type without either standard syntax capability, or it
/// might deliberately expand parameters despite supporting both capabilities.
///
/// @see org.hibernate.dialect.Dialect#getArraySupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class ArraySupport {
	/// No standard array syntax, no array-constructor syntax, and expanded
	/// multi-valued parameters.
	public static final ArraySupport NONE = new ArraySupport( Set.of(), EXPANDED );

	/// Both standard array syntax capabilities and array-bound multi-valued
	/// parameters.
	public static final ArraySupport STANDARD = new ArraySupport(
			Set.of( STANDARD_ARRAY, ARRAY_CONSTRUCTOR ),
			ARRAY
	);

	private final Set<Capability> capabilities;
	private final MultiValuedParameterStrategy multiValuedParameterStrategy;

	private ArraySupport(
			Set<Capability> capabilities,
			MultiValuedParameterStrategy multiValuedParameterStrategy) {
		this.capabilities = capabilities;
		this.multiValuedParameterStrategy = multiValuedParameterStrategy;
	}

	/// Create a builder initialized from [#NONE].
	public static Builder builder() {
		return new Builder( NONE );
	}

	/// Create a builder initialized with every value from the given profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(ArraySupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// The immutable set of supported array-syntax capabilities.
	public Set<Capability> getCapabilities() {
		return capabilities;
	}

	/// Whether the given array-syntax capability is supported.
	public boolean supports(Capability capability) {
		return capabilities.contains( requireArgument( capability, "capability" ) );
	}

	/// The strategy for passing a multi-valued parameter to the database.
	public MultiValuedParameterStrategy getMultiValuedParameterStrategy() {
		return multiValuedParameterStrategy;
	}

	/// An independently configurable array-syntax capability.
	public enum Capability {
		/// ANSI element-type array declarations such as `integer array` and
		/// standard array literals.
		STANDARD_ARRAY,

		/// SQL array-constructor syntax used by recursive-CTE emulation.
		ARRAY_CONSTRUCTOR
	}

	/// How multi-valued parameters are passed to the database.
	public enum MultiValuedParameterStrategy {
		/// Bind the values as one SQL array parameter.
		ARRAY,

		/// Expand the values into individual JDBC parameters.
		EXPANDED
	}

	/// Build an immutable array-support profile.
	public static final class Builder {
		private final EnumSet<Capability> capabilities;
		private MultiValuedParameterStrategy multiValuedParameterStrategy;

		private Builder(ArraySupport base) {
			capabilities = base.capabilities.isEmpty()
					? EnumSet.noneOf( Capability.class )
					: EnumSet.copyOf( base.capabilities );
			multiValuedParameterStrategy = base.multiValuedParameterStrategy;
		}

		/// Enable the given array-syntax capabilities.
		public Builder capabilities(Capability... capabilities) {
			requireArgument( capabilities, "capabilities" );
			for ( Capability capability : capabilities ) {
				capability( capability, true );
			}
			return this;
		}

		/// Enable or disable an array-syntax capability.
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

		/// Select how multi-valued parameters are passed to the database.
		public Builder multiValuedParameterStrategy(MultiValuedParameterStrategy strategy) {
			multiValuedParameterStrategy = requireArgument( strategy, "strategy" );
			return this;
		}

		/// Build an immutable snapshot of this builder.
		public ArraySupport build() {
			return new ArraySupport( Set.copyOf( capabilities ), multiValuedParameterStrategy );
		}
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
