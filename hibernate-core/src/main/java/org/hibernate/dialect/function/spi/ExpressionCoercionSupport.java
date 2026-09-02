/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.spi;

import java.util.EnumSet;
import java.util.Set;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable description of the expression-coercion adaptations required by a
/// Dialect's SQL rendering.
///
/// Providers supply a stable profile through
/// [org.hibernate.dialect.Dialect#getExpressionCoercionSupport]. Each
/// requirement directs a focused rendering adaptation. It does not report
/// general database cast support and must not be used as a database-wide
/// implicit-coercion policy.
///
/// @see org.hibernate.dialect.Dialect#getExpressionCoercionSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class ExpressionCoercionSupport {
	/// No required expression-coercion adaptations.
	public static final ExpressionCoercionSupport NONE = new ExpressionCoercionSupport( Set.of() );

	/// The base-Dialect profile, which requires no expression-coercion
	/// adaptations.
	public static final ExpressionCoercionSupport STANDARD = new ExpressionCoercionSupport( Set.of() );

	private final Set<Requirement> requirements;

	private ExpressionCoercionSupport(Set<Requirement> requirements) {
		this.requirements = Set.copyOf( requirements );
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized with the complete requirement set from the
	/// given profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(ExpressionCoercionSupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// The immutable set of required expression-coercion adaptations.
	public Set<Requirement> getRequirements() {
		return requirements;
	}

	/// Whether the given rendering adaptation is required.
	public boolean requires(Requirement requirement) {
		return requirements.contains( requireArgument( requirement, "requirement" ) );
	}

	/// An independently configurable expression-coercion adaptation.
	public enum Requirement {
		/// Cast non-string operands before using them in concatenation rendering.
		CAST_NON_STRING_CONCATENATION_ARGUMENTS,

		/// Cast integer division to a floating type before applying division
		/// semantics which require a non-integral result.
		CAST_INTEGER_DIVISION_TO_FLOAT
	}

	/// Build an immutable expression-coercion-support profile.
	public static final class Builder {
		private final EnumSet<Requirement> requirements;

		private Builder(ExpressionCoercionSupport base) {
			requirements = base.requirements.isEmpty()
					? EnumSet.noneOf( Requirement.class )
					: EnumSet.copyOf( base.requirements );
		}

		/// Add the given requirements without changing other requirements.
		public Builder requirements(Requirement... requirements) {
			requireArgument( requirements, "requirements" );
			for ( Requirement requirement : requirements ) {
				requirement( requirement, true );
			}
			return this;
		}

		/// Add or remove exactly one rendering requirement.
		public Builder requirement(Requirement requirement, boolean required) {
			requireArgument( requirement, "requirement" );
			if ( required ) {
				requirements.add( requirement );
			}
			else {
				requirements.remove( requirement );
			}
			return this;
		}

		/// Build an immutable snapshot of this builder.
		public ExpressionCoercionSupport build() {
			return new ExpressionCoercionSupport( requirements );
		}
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
