/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identifier.spi;

import java.util.Collection;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Receives SQL keywords while a Dialect's immutable keyword profile is built.
///
/// Register words only during the contribution callback and do not retain this
/// boot-scoped registration target.
///
/// @author Steve Ebersole
/// @since 8.0
@FunctionalInterface
@SPI(USE)
public interface KeywordRegistration {
	/// Register one SQL keyword.
	void registerKeyword(String keyword);

	/// Register the given SQL keywords in encounter order.
	default void registerKeywords(Collection<String> keywords) {
		if ( keywords != null ) {
			keywords.forEach( this::registerKeyword );
		}
	}

	/// Register the given SQL keywords in declaration order.
	default void registerKeywords(String... keywords) {
		if ( keywords != null ) {
			for ( String keyword : keywords ) {
				registerKeyword( keyword );
			}
		}
	}
}
