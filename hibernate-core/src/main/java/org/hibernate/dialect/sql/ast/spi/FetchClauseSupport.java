/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.EnumSet;
import java.util.Set;

import org.hibernate.SPI;
import org.hibernate.query.common.FetchClauseType;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable description of the `fetch` clause forms accepted by a database.
///
/// A Dialect supplies one stable profile through
/// [org.hibernate.dialect.Dialect#getFetchClauseSupport()]. Each
/// [FetchClauseType] is independent: do not infer percent or ties support from
/// ordinary row-count support.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see org.hibernate.dialect.Dialect#getFetchClauseSupport()
@SPI({ USE, SUPPLY })
public final class FetchClauseSupport {
	/// No `fetch` clause form is supported.
	public static final FetchClauseSupport NONE = new FetchClauseSupport( Set.of() );

	/// Only `fetch first n rows only` is supported.
	public static final FetchClauseSupport ROWS_ONLY = new FetchClauseSupport(
			Set.of( FetchClauseType.ROWS_ONLY )
	);

	/// Both ordinary row-count and row-count-with-ties forms are supported.
	public static final FetchClauseSupport ROWS = new FetchClauseSupport(
			Set.of( FetchClauseType.ROWS_ONLY, FetchClauseType.ROWS_WITH_TIES )
	);

	/// Every standard row-count, percent, and ties form is supported.
	public static final FetchClauseSupport ALL = new FetchClauseSupport(
			Set.of( FetchClauseType.values() )
	);

	private final Set<FetchClauseType> supportedTypes;

	private FetchClauseSupport(Set<FetchClauseType> supportedTypes) {
		this.supportedTypes = supportedTypes;
	}

	/// Create an immutable profile containing exactly the supplied forms.
	///
	/// Duplicate forms are ignored. The four stock sets are canonicalized to
	/// [#NONE], [#ROWS_ONLY], [#ROWS], or [#ALL].
	public static FetchClauseSupport of(FetchClauseType... supportedTypes) {
		requireArgument( supportedTypes, "supportedTypes" );
		final EnumSet<FetchClauseType> types = EnumSet.noneOf( FetchClauseType.class );
		for ( FetchClauseType type : supportedTypes ) {
			types.add( requireArgument( type, "supportedTypes element" ) );
		}
		if ( types.isEmpty() ) {
			return NONE;
		}
		if ( types.equals( ROWS_ONLY.supportedTypes ) ) {
			return ROWS_ONLY;
		}
		if ( types.equals( ROWS.supportedTypes ) ) {
			return ROWS;
		}
		if ( types.size() == FetchClauseType.values().length ) {
			return ALL;
		}
		return new FetchClauseSupport( Set.copyOf( types ) );
	}

	/// The immutable set of supported `fetch` clause forms.
	public Set<FetchClauseType> getSupportedTypes() {
		return supportedTypes;
	}

	/// Whether the given `fetch` clause form is supported.
	public boolean supports(FetchClauseType type) {
		return supportedTypes.contains( requireArgument( type, "type" ) );
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
