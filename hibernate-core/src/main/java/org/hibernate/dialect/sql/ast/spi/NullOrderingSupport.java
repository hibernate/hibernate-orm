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

/// Immutable description of a Dialect's default null ordering and native
/// explicit null-precedence syntax.
///
/// Providers supply a stable profile through
/// [org.hibernate.dialect.Dialect#getNullOrderingSupport]. The default ordering
/// describes where the database places null values when an `order by` item does
/// not specify `nulls first` or `nulls last`. It is independent of
/// [Capability#NULLS_FIRST_LAST], which reports whether that explicit syntax may
/// be rendered natively.
///
/// Consumers should use both parts of this profile when deciding whether an
/// explicit precedence may be elided, rendered natively, or emulated. Providers
/// refining a Dialect family profile should copy the profile returned by the
/// superclass and change only the values which differ.
///
/// @see org.hibernate.dialect.Dialect#getNullOrderingSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class NullOrderingSupport {
	/// The base-Dialect profile: null values sort as the greatest values and
	/// explicit `nulls first` and `nulls last` syntax is supported.
	public static final NullOrderingSupport STANDARD = new NullOrderingSupport(
			NullOrdering.GREATEST,
			Set.of( Capability.NULLS_FIRST_LAST )
	);

	private final NullOrdering defaultOrdering;
	private final Set<Capability> capabilities;

	private NullOrderingSupport(NullOrdering defaultOrdering, Set<Capability> capabilities) {
		this.defaultOrdering = requireArgument( defaultOrdering, "defaultOrdering" );
		this.capabilities = Set.copyOf( capabilities );
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized with the complete state of the given profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(NullOrderingSupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// The database's default ordering of null values.
	public NullOrdering getDefaultOrdering() {
		return defaultOrdering;
	}

	/// The immutable set of supported null-ordering syntax capabilities.
	public Set<Capability> getCapabilities() {
		return capabilities;
	}

	/// Whether the given null-ordering syntax capability is supported natively.
	public boolean supports(Capability capability) {
		return capabilities.contains( requireArgument( capability, "capability" ) );
	}

	/// An independently configurable null-ordering syntax capability.
	public enum Capability {
		/// Explicit `nulls first` and `nulls last` syntax.
		NULLS_FIRST_LAST
	}

	/// Build an immutable null-ordering-support profile.
	public static final class Builder {
		private NullOrdering defaultOrdering;
		private final EnumSet<Capability> capabilities;

		private Builder(NullOrderingSupport base) {
			defaultOrdering = base.defaultOrdering;
			capabilities = base.capabilities.isEmpty()
					? EnumSet.noneOf( Capability.class )
					: EnumSet.copyOf( base.capabilities );
		}

		/// Set the database's mandatory default null ordering.
		public Builder defaultOrdering(NullOrdering ordering) {
			defaultOrdering = requireArgument( ordering, "ordering" );
			return this;
		}

		/// Enable the given capabilities without changing other capabilities.
		public Builder capabilities(Capability... capabilities) {
			requireArgument( capabilities, "capabilities" );
			for ( Capability capability : capabilities ) {
				capability( capability, true );
			}
			return this;
		}

		/// Enable or disable exactly one capability.
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
		public NullOrderingSupport build() {
			return new NullOrderingSupport( defaultOrdering, capabilities );
		}
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
