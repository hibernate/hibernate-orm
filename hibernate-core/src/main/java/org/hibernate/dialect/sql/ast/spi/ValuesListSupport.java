/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;


import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable description of the contexts in which a Dialect supports native
/// `values` syntax.
///
/// Providers supply a profile through `Dialect#getValuesListSupport()`.
/// Hibernate may emulate an unsupported context with a semantically equivalent
/// query form.
///
/// @see org.hibernate.dialect.Dialect#getValuesListSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class ValuesListSupport {
	/// No native `values` syntax.
	public static final ValuesListSupport NONE = new ValuesListSupport( Set.of() );

	/// Native `values` syntax for inserts only.
	public static final ValuesListSupport INSERT_ONLY = of( Context.INSERT );

	/// Native `values` syntax in every supported context.
	public static final ValuesListSupport STANDARD = of( Context.QUERY, Context.INSERT );

	private final Set<Context> contexts;

	private ValuesListSupport(Set<Context> contexts) {
		this.contexts = contexts;
	}

	/// Create a profile supporting the given contexts.
	public static ValuesListSupport of(Context... contexts) {
		Objects.requireNonNull( contexts, "contexts" );
		if ( contexts.length == 0 ) {
			return NONE;
		}
		final EnumSet<Context> supported = EnumSet.noneOf( Context.class );
		for ( Context context : contexts ) {
			supported.add( Objects.requireNonNull( context, "context" ) );
		}
		return new ValuesListSupport( Set.copyOf( supported ) );
	}

	/// The immutable set of contexts supporting native `values`.
	public Set<Context> getContexts() {
		return contexts;
	}

	/// Whether native `values` is supported in the given context, independently
	/// of list cardinality.
	public boolean supports(Context context) {
		return contexts.contains( Objects.requireNonNull( context, "context" ) );
	}

	/// A SQL context in which a values list may occur.
	public enum Context {
		/// A values query or table expression.
		QUERY,

		/// The source of an insert statement.
		INSERT
	}
}
