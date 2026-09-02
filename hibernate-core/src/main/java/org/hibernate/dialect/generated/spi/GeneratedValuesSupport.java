/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.generated.spi;

import java.util.EnumSet;
import java.util.Set;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable description of the mechanisms a Dialect supports for retrieving
/// arbitrary database-generated values during mutation execution.
///
/// Providers supply a stable profile through
/// [org.hibernate.dialect.Dialect#getGeneratedValuesSupport()]. Native mutation returning and JDBC
/// generated keys are independent mechanisms. Identity-only generated-key
/// behavior belongs to
/// [org.hibernate.dialect.identity.spi.IdentityColumnSupport], while an
/// ordinary select after a mutation is a Hibernate fallback determined from
/// mapping and execution state rather than a Dialect capability.
///
/// @see org.hibernate.dialect.Dialect#getGeneratedValuesSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class GeneratedValuesSupport {
	/// Independently variable generated-value retrieval capabilities.
	///
	/// @since 8.0
	public enum Capability {
		/// Native SQL can return arbitrary generated values from an insert.
		INSERT_RETURNING,
		/// Native SQL can return arbitrary generated values from an update.
		UPDATE_RETURNING,
		/// Native insert returning can include the database row identifier.
		INSERT_RETURNING_ROW_ID,
		/// JDBC generated keys can return arbitrary generated columns, not only an
		/// identity identifier.
		ARBITRARY_GENERATED_KEYS
	}

	/// The base-Dialect profile, which advertises no immediate generated-value
	/// retrieval mechanism.
	public static final GeneratedValuesSupport STANDARD = new GeneratedValuesSupport(
			EnumSet.noneOf( Capability.class ),
			false
	);

	private final Set<Capability> capabilities;
	private final boolean unquoteGeneratedKeyColumnNames;

	private GeneratedValuesSupport(
			EnumSet<Capability> capabilities,
			boolean unquoteGeneratedKeyColumnNames) {
		validate( capabilities, unquoteGeneratedKeyColumnNames );
		this.capabilities = Set.copyOf( capabilities );
		this.unquoteGeneratedKeyColumnNames = unquoteGeneratedKeyColumnNames;
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized with every value from the given profile.
	public static Builder builder(GeneratedValuesSupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// Whether the given immediate retrieval capability is supported.
	public boolean supports(Capability capability) {
		return capabilities.contains( requireArgument( capability, "capability" ) );
	}

	/// The immutable set of supported immediate retrieval capabilities.
	public Set<Capability> getCapabilities() {
		return capabilities;
	}

	/// Whether generated column names must be unquoted before passing them to
	/// JDBC statement preparation.
	public boolean unquoteGeneratedKeyColumnNames() {
		return unquoteGeneratedKeyColumnNames;
	}

	/// Build an immutable generated-values profile.
	///
	/// @since 8.0
	public static final class Builder {
		private final EnumSet<Capability> capabilities;
		private boolean unquoteGeneratedKeyColumnNames;

		private Builder(GeneratedValuesSupport base) {
			capabilities = base.capabilities.isEmpty()
					? EnumSet.noneOf( Capability.class )
					: EnumSet.copyOf( base.capabilities );
			unquoteGeneratedKeyColumnNames = base.unquoteGeneratedKeyColumnNames;
		}

		/// Enable exactly the specified capabilities without implying any other
		/// capability.
		public Builder enable(Capability... capabilities) {
			addCapabilities( this.capabilities, capabilities );
			return this;
		}

		/// Disable exactly the specified capabilities without cascading removal of
		/// dependent capabilities.
		public Builder disable(Capability... capabilities) {
			removeCapabilities( this.capabilities, capabilities );
			return this;
		}

		/// Require generated-key column names to be unquoted before JDBC statement
		/// preparation.
		public Builder unquoteGeneratedKeyColumnNames(boolean unquote) {
			unquoteGeneratedKeyColumnNames = unquote;
			return this;
		}

		/// Build an immutable snapshot and reject inconsistent capability and
		/// option combinations.
		public GeneratedValuesSupport build() {
			return new GeneratedValuesSupport( capabilities, unquoteGeneratedKeyColumnNames );
		}
	}

	private static void validate(
			Set<Capability> capabilities,
			boolean unquoteGeneratedKeyColumnNames) {
		if ( capabilities.contains( Capability.INSERT_RETURNING_ROW_ID )
				&& !capabilities.contains( Capability.INSERT_RETURNING ) ) {
			throw new IllegalArgumentException( "INSERT_RETURNING_ROW_ID requires INSERT_RETURNING" );
		}
		if ( unquoteGeneratedKeyColumnNames
				&& !capabilities.contains( Capability.ARBITRARY_GENERATED_KEYS ) ) {
			throw new IllegalArgumentException(
					"unquoteGeneratedKeyColumnNames requires ARBITRARY_GENERATED_KEYS"
			);
		}
	}

	private static void addCapabilities(
			EnumSet<Capability> target,
			Capability[] capabilities) {
		for ( Capability capability : requireCapabilities( capabilities ) ) {
			target.add( capability );
		}
	}

	private static void removeCapabilities(
			EnumSet<Capability> target,
			Capability[] capabilities) {
		for ( Capability capability : requireCapabilities( capabilities ) ) {
			target.remove( capability );
		}
	}

	private static Capability[] requireCapabilities(Capability[] capabilities) {
		requireArgument( capabilities, "capabilities" );
		for ( Capability capability : capabilities ) {
			requireArgument( capability, "capability" );
		}
		return capabilities;
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
