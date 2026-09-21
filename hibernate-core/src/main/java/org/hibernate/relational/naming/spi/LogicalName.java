/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational.naming.spi;

import java.io.Serializable;
import java.util.Locale;

import org.hibernate.SPI;

/// An immutable name of a database object (table, column, index, etc.) before
/// [physical naming][org.hibernate.boot.model.naming.PhysicalNamingStrategy] is applied to that object.
///
/// The logical name tracks whether the name was explicitly supplied in mapping metadata
/// (for example, annotations or XML) or implicitly determined. This distinction is
/// available through [#isExplicit()] for naming strategies to inspect, but does not
/// affect equality, hashing, or ordering. Explicit names still undergo physical naming.
///
/// For example:
/// ````java
/// @Entity
/// class Person { ... }
/// ````
/// Here we have an implicit table name `Person` for the `Person` entity, though
/// [org.hibernate.boot.model.naming.ImplicitNamingStrategy] can affect that.
///
/// Compare that to:
/// ````java
/// @Entity
/// @Table(name="t_person")
/// class Person { ... }
/// ````
/// Now we have an explicit logical name `t_person` for the `Person` entity.
///
/// The original spelling is preserved by [#getText()]. Quoting is represented separately
/// by [#isQuoted()], not by delimiters in the text. Requested quoting must be preserved
/// during physical naming; physical naming and identifier finalization may add quoting.
///
/// Unquoted names compare using locale-independent lowercase keys. Quoted names compare
/// exactly and never equal unquoted names, even when their text is identical. Natural
/// ordering is consistent with equality: unquoted names sort before quoted names, and
/// names within each group sort lexicographically by their comparison keys. These rules
/// are independent of the dialect's physical identifier comparison policy.
///
/// @see org.hibernate.boot.model.naming.ImplicitNamingStrategy
/// @see org.hibernate.boot.model.naming.PhysicalNamingStrategy
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public final class LogicalName implements Comparable<LogicalName>, Serializable {
	private final String text;
	private final boolean quoted;
	private final boolean explicit;
	private final String comparisonKey;

	/// Construct a name from nonempty text without surrounding quote delimiters.
	///
	/// @param text the original spelling, without quote delimiters
	/// @param quoted whether the physical identifier must be quoted
	/// @param explicit whether the name was explicitly supplied in mapping metadata
	/// @throws IllegalArgumentException if the text fails name validation
	public LogicalName(String text, boolean quoted, boolean explicit) {
		NameText.validate( text );
		this.text = text;
		this.quoted = quoted;
		this.explicit = explicit;
		comparisonKey = quoted ? text : text.toLowerCase( Locale.ROOT );
	}

	/// The original spelling, without quote delimiters or case normalization.
	public String getText() { return text; }

	/// Whether this name requires quoting when converted to a physical identifier.
	public boolean isQuoted() { return quoted; }

	/// Whether the name was explicitly supplied rather than implicitly determined.
	public boolean isExplicit() { return explicit; }

	@Override
	public boolean equals(Object other) {
		return other instanceof LogicalName that
				&& quoted == that.quoted && comparisonKey.equals( that.comparisonKey );
	}

	@Override
	public int hashCode() {
		return 31 * comparisonKey.hashCode() + Boolean.hashCode( quoted );
	}

	@Override
	public int compareTo(LogicalName other) {
		final int quoting = Boolean.compare( quoted, other.quoted );
		return quoting == 0 ? comparisonKey.compareTo( other.comparisonKey ) : quoting;
	}

	/// A diagnostic representation, using backticks for quoted names; not rendered SQL.
	@Override
	public String toString() {
		return quoted ? '`' + text + '`' : text;
	}
}
