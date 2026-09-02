/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identifier.spi;

import java.util.Set;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines the stable SQL keywords of a Dialect and filters driver observations.
///
/// Return an immutable lowercase set from [#getKeywords()]. Driver-reported
/// words belong to JDBC metadata and are admitted separately through
/// [#acceptsJdbcKeyword(String)].
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getKeywordSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface KeywordSupport {
	/// Return the immutable ANSI and Dialect-defined keyword set.
	Set<String> getKeywords();

	/// Decide whether a normalized driver-reported word is truly a keyword for
	/// this database.
	default boolean acceptsJdbcKeyword(String keyword) {
		return true;
	}
}
