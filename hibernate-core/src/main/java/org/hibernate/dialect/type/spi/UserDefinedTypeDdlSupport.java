/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;

import static org.hibernate.SPI.Role.USE;

/// Configures standard user-defined-type create and drop command rendering.
/// String fragments are preserved verbatim, including provider-owned spacing.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record UserDefinedTypeDdlSupport(
		String createTypeKind,
		String createTypeExtensions,
		ExistenceCheckPlacement dropIfExistsPlacement) {
	/// Standard UDT grammar without a kind, extensions, or existence check.
	public static final UserDefinedTypeDdlSupport STANDARD =
			new UserDefinedTypeDdlSupport( "", "", ExistenceCheckPlacement.NONE );

	/// Validate the immutable grammar components.
	public UserDefinedTypeDdlSupport {
		if ( createTypeKind == null || createTypeExtensions == null || dropIfExistsPlacement == null ) {
			throw new IllegalArgumentException( "User-defined-type DDL components must not be null" );
		}
	}
}
