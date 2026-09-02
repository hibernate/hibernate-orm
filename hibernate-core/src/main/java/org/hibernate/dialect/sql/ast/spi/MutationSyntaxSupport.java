/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;


import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable description of a Dialect's native SQL mutation syntax.
///
/// Providers supply a profile through `Dialect#getMutationSyntaxSupport()`.
/// Returned capability sets are immutable, and the profile must remain stable
/// for the lifetime of the Dialect.
///
/// @see org.hibernate.dialect.Dialect#getMutationSyntaxSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public interface MutationSyntaxSupport {
	/// A profile which reports no native mutation-syntax capabilities.
	MutationSyntaxSupport NONE = mutationKind -> Set.of();

	/// The immutable capabilities for the given mutation kind.
	Set<MutationSyntaxCapability> capabilities(MutationKind mutationKind);

	/// Whether the given capability is supported for the mutation kind.
	default boolean supports(MutationKind mutationKind, MutationSyntaxCapability capability) {
		return capabilities( mutationKind ).contains( capability );
	}

	/// Create an empty profile builder.
	static Builder builder() {
		return new Builder();
	}

	/// Create a profile containing capabilities for one mutation kind.
	static MutationSyntaxSupport of(
			MutationKind mutationKind,
			MutationSyntaxCapability... capabilities) {
		return builder().capabilities( mutationKind, capabilities ).build();
	}

	/// Build an immutable mutation-syntax profile.
	final class Builder {
		private final EnumMap<MutationKind, EnumSet<MutationSyntaxCapability>> capabilities =
				new EnumMap<>( MutationKind.class );

		private Builder() {
		}

		/// Enable a capability for a mutation kind.
		public Builder capability(MutationKind mutationKind, MutationSyntaxCapability capability) {
			return capability( mutationKind, capability, true );
		}

		/// Enable or disable a capability for a mutation kind.
		public Builder capability(
				MutationKind mutationKind,
				MutationSyntaxCapability capability,
				boolean supported) {
			Objects.requireNonNull( mutationKind, "mutationKind" );
			Objects.requireNonNull( capability, "capability" );
			if ( supported ) {
				capabilities.computeIfAbsent(
						mutationKind,
						ignored -> EnumSet.noneOf( MutationSyntaxCapability.class )
				).add( capability );
			}
			else {
				final Set<MutationSyntaxCapability> mutationCapabilities = capabilities.get( mutationKind );
				if ( mutationCapabilities != null ) {
					mutationCapabilities.remove( capability );
				}
			}
			return this;
		}

		/// Enable capabilities for a mutation kind.
		public Builder capabilities(
				MutationKind mutationKind,
				MutationSyntaxCapability... mutationCapabilities) {
			Objects.requireNonNull( mutationCapabilities, "mutationCapabilities" );
			for ( MutationSyntaxCapability capability : mutationCapabilities ) {
				capability( mutationKind, capability );
			}
			return this;
		}

		/// Build the immutable profile.
		public MutationSyntaxSupport build() {
			if ( capabilities.isEmpty() ) {
				return NONE;
			}
			final EnumMap<MutationKind, Set<MutationSyntaxCapability>> snapshot =
					new EnumMap<>( MutationKind.class );
			capabilities.forEach( (mutationKind, values) -> snapshot.put( mutationKind, Set.copyOf( values ) ) );
			final Map<MutationKind, Set<MutationSyntaxCapability>> immutableSnapshot = Map.copyOf( snapshot );
			return mutationKind -> immutableSnapshot.getOrDefault(
					Objects.requireNonNull( mutationKind, "mutationKind" ),
					Set.of()
			);
		}
	}
}
