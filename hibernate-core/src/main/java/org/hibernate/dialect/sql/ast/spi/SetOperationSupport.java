/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.EnumSet;
import java.util.Set;

import org.hibernate.SPI;
import org.hibernate.query.sqm.SetOperator;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable description of a Dialect's set-operation grammar.
///
/// Providers supply a stable profile through
/// [org.hibernate.dialect.Dialect#getSetOperationSupport]. The operator set
/// reports which of the six SQL set operators may be emitted. The capability
/// set separately reports structural rules for subquery placement, duplicate
/// select items, and simple parenthesized query grouping.
///
/// Treat every operator as independent. In particular, support for a distinct
/// operator does not imply support for its `ALL` form, and support for `UNION`
/// does not imply [Capability#UNION_IN_SUBQUERY].
///
/// @see org.hibernate.dialect.Dialect#getSetOperationSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class SetOperationSupport {
	/// No native set operator or structural set-operation capability.
	public static final SetOperationSupport NONE = new SetOperationSupport( Set.of(), Set.of() );

	/// The base-Dialect profile: all six operators and all structural
	/// capabilities.
	public static final SetOperationSupport STANDARD = new SetOperationSupport(
			Set.of( SetOperator.values() ),
			Set.of( Capability.values() )
	);

	private final Set<SetOperator> supportedOperators;
	private final Set<Capability> capabilities;

	private SetOperationSupport(Set<SetOperator> supportedOperators, Set<Capability> capabilities) {
		this.supportedOperators = Set.copyOf( supportedOperators );
		this.capabilities = Set.copyOf( capabilities );
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized with both complete sets from the given
	/// profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(SetOperationSupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// The immutable set of supported SQL set operators.
	public Set<SetOperator> getSupportedOperators() {
		return supportedOperators;
	}

	/// The immutable set of supported structural capabilities.
	public Set<Capability> getCapabilities() {
		return capabilities;
	}

	/// Whether the given SQL set operator is supported natively.
	public boolean supports(SetOperator operator) {
		return supportedOperators.contains( requireArgument( operator, "operator" ) );
	}

	/// Whether the given structural set-operation capability is supported.
	public boolean supports(Capability capability) {
		return capabilities.contains( requireArgument( capability, "capability" ) );
	}

	/// An independently configurable structural set-operation capability.
	public enum Capability {
		/// A `UNION` query group may occur inside a subquery.
		UNION_IN_SUBQUERY,

		/// Query-group branches may project the same expression more than once.
		DUPLICATE_SELECT_ITEMS,

		/// A query-group branch may be grouped directly with parentheses instead
		/// of requiring an outer select wrapper.
		SIMPLE_QUERY_GROUPING
	}

	/// Build an immutable set-operation-support profile.
	public static final class Builder {
		private final EnumSet<SetOperator> operators;
		private final EnumSet<Capability> capabilities;

		private Builder(SetOperationSupport base) {
			operators = copyOf( base.supportedOperators, SetOperator.class );
			capabilities = copyOf( base.capabilities, Capability.class );
		}

		/// Enable the given operators without changing other operators.
		public Builder operators(SetOperator... operators) {
			requireArgument( operators, "operators" );
			for ( SetOperator operator : operators ) {
				operator( operator, true );
			}
			return this;
		}

		/// Enable or disable exactly one operator.
		public Builder operator(SetOperator operator, boolean supported) {
			requireArgument( operator, "operator" );
			if ( supported ) {
				operators.add( operator );
			}
			else {
				operators.remove( operator );
			}
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
		public SetOperationSupport build() {
			return new SetOperationSupport( operators, capabilities );
		}
	}

	private static <E extends Enum<E>> EnumSet<E> copyOf(Set<E> values, Class<E> type) {
		return values.isEmpty() ? EnumSet.noneOf( type ) : EnumSet.copyOf( values );
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
