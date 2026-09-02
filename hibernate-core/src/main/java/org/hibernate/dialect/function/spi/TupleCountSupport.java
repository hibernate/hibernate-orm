/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.dialect.function.spi.TupleCountSupport.Syntax.ARGUMENT_LIST;
import static org.hibernate.dialect.function.spi.TupleCountSupport.Syntax.UNSUPPORTED;

/// Immutable description of a Dialect's aggregate tuple-count syntax.
///
/// Providers supply a stable profile through
/// [org.hibernate.dialect.Dialect#getTupleCountSupport]. Ordinary tuple counts
/// and distinct tuple counts are independent syntax choices. Select
/// [Syntax#UNSUPPORTED] to request Hibernate's existing emulation,
/// [Syntax#ARGUMENT_LIST] for forms such as `count(a, b)`, or
/// [Syntax#PARENTHESIZED_TUPLE] for forms such as `count((a, b))`.
///
/// Do not infer the distinct form from the ordinary form. A database may, for
/// example, accept `count((a, b))` while requiring
/// `count(distinct a, b)` for the distinct form.
///
/// @see org.hibernate.dialect.Dialect#getTupleCountSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class TupleCountSupport {
	/// Neither ordinary nor distinct tuple counts have native syntax.
	public static final TupleCountSupport NONE = new TupleCountSupport( UNSUPPORTED, UNSUPPORTED );

	/// The base-Dialect profile: emulate ordinary tuple counts and render
	/// distinct tuples as an argument list.
	public static final TupleCountSupport STANDARD = new TupleCountSupport( UNSUPPORTED, ARGUMENT_LIST );

	private final Syntax nonDistinctSyntax;
	private final Syntax distinctSyntax;

	private TupleCountSupport(Syntax nonDistinctSyntax, Syntax distinctSyntax) {
		this.nonDistinctSyntax = requireArgument( nonDistinctSyntax, "nonDistinctSyntax" );
		this.distinctSyntax = requireArgument( distinctSyntax, "distinctSyntax" );
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized with both syntax choices from the given
	/// profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(TupleCountSupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// The syntax for ordinary multi-expression counts.
	public Syntax getNonDistinctSyntax() {
		return nonDistinctSyntax;
	}

	/// The syntax for distinct multi-expression counts.
	public Syntax getDistinctSyntax() {
		return distinctSyntax;
	}

	/// A complete native syntax choice for one tuple-count form.
	public enum Syntax {
		/// No native syntax; select Hibernate's tuple-count emulation.
		UNSUPPORTED,

		/// Render the tuple expressions as separate aggregate arguments, for
		/// example `count(a, b)` or `count(distinct a, b)`.
		ARGUMENT_LIST,

		/// Render the expressions as one parenthesized tuple, for example
		/// `count((a, b))` or `count(distinct (a, b))`.
		PARENTHESIZED_TUPLE
	}

	/// Build an immutable tuple-count-support profile.
	public static final class Builder {
		private Syntax nonDistinctSyntax;
		private Syntax distinctSyntax;

		private Builder(TupleCountSupport base) {
			nonDistinctSyntax = base.nonDistinctSyntax;
			distinctSyntax = base.distinctSyntax;
		}

		/// Select the complete syntax for ordinary multi-expression counts.
		public Builder nonDistinctSyntax(Syntax syntax) {
			nonDistinctSyntax = requireArgument( syntax, "syntax" );
			return this;
		}

		/// Select the complete syntax for distinct multi-expression counts.
		public Builder distinctSyntax(Syntax syntax) {
			distinctSyntax = requireArgument( syntax, "syntax" );
			return this;
		}

		/// Build an immutable snapshot of this builder.
		public TupleCountSupport build() {
			return new TupleCountSupport( nonDistinctSyntax, distinctSyntax );
		}
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
