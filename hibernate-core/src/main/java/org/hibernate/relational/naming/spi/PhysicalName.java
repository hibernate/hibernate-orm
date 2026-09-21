/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational.naming.spi;

import org.hibernate.Internal;
import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// An immutable physical name of a database object (table, column, index, etc.),
/// representing its database spelling and quoting state.
/// A [physical naming strategy][org.hibernate.boot.model.naming.PhysicalNamingStrategy]
/// transforms a [LogicalName] into a physical name using the supplied [Factory].
/// For example, a strategy might transform the logical table name `Person` into
/// the physical table name `p_person`.
///
/// The original physical spelling is preserved by [#getText()]. Quoting is represented
/// separately by [#isQuoted()], not by delimiters in the text. Hibernate preserves
/// quoting requested by the logical name and applies configured global and automatic
/// quoting when finalizing the strategy's result. A strategy result is therefore not
/// necessarily the final quoting state of the mapped identifier.
///
/// Equality and hashing use a comparison key computed at construction by the system's
/// shared [IdentifierComparisonPolicy]. Natural ordering compares those keys
/// lexicographically and is consistent with equality. Unlike [LogicalName], quoting
/// is not compared separately: quoted and unquoted names may be equal if the policy
/// produces the same key. Differently spelled names may likewise be equal while
/// retaining their original text for rendering.
///
/// Comparisons assume names belong to the same configured system. Naming strategies
/// must use the supplied factory so that names share its comparison policy;
/// equality does not check policy identity or system membership. This type does not
/// retain the logical name's explicit/implicit provenance.
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public final class PhysicalName implements Comparable<PhysicalName> {
	private final String text;
	private final boolean quoted;
	private final String comparisonKey;

	private PhysicalName(IdentifierComparisonPolicy policy, String text, boolean quoted) {
		NameText.validate( text );
		this.text = text;
		this.quoted = quoted;
		comparisonKey = requireNonNull( policy.comparisonKey( text, quoted ), "comparisonKey" );
	}

	/// The original physical spelling, without quote delimiters or comparison-key normalization.
	public String getText() { return text; }

	/// Whether this physical identifier is quoted.
	public boolean isQuoted() { return quoted; }

	@Override
	public boolean equals(Object other) {
		return other instanceof PhysicalName that
				&& comparisonKey.equals( that.comparisonKey );
	}

	@Override
	public int hashCode() {
		return comparisonKey.hashCode();
	}

	@Override
	public int compareTo(PhysicalName other) {
		return comparisonKey.compareTo( other.comparisonKey );
	}

	/// A diagnostic representation, using backticks for quoted names; not dialect-specific SQL.
	@Override
	public String toString() {
		return quoted ? '`' + text + '`' : text;
	}

	/// Constructs physical names using the configured system's shared comparison policy.
	/// Naming strategies obtain this factory from their supplied context.
	///
	/// The factory accepts physical spelling and quoting directly. It does not apply
	/// a naming strategy, mapping defaults, global quoting, or automatic quoting, and
	/// it has no logical name from which to preserve requested quoting. Those rules
	/// are applied by Hibernate when finalizing a naming strategy's result.
	///
	/// @author Steve Ebersole
	@SPI(SPI.Role.USE)
	public static final class Factory {
		private final IdentifierComparisonPolicy policy;

		/// Internal bootstrap entry point, not a naming-strategy extension point.
		///
		/// @param policy the immutable comparison policy shared by the configured system
		/// @throws NullPointerException if the policy is null
		@Internal
		public Factory(IdentifierComparisonPolicy policy) {
			this.policy = requireNonNull( policy, "policy" );
		}

		/// The shared policy used to compute comparison keys for names created by this factory.
		@Internal
		public IdentifierComparisonPolicy getComparisonPolicy() { return policy; }

		/// Create a physical name and compute its comparison key using this factory's policy.
		///
		/// @param text the physical spelling, without quote delimiters
		/// @param quoted whether the physical identifier is quoted
		/// @return a new physical name preserving the supplied spelling and quoting
		/// @throws IllegalArgumentException if the text fails name validation
		/// @throws NullPointerException if the policy returns a null comparison key
		public PhysicalName create(String text, boolean quoted) {
			return new PhysicalName( policy, text, quoted );
		}

	}
}
