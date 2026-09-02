/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import jakarta.annotation.Nullable;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Selects the JDBC metadata calls used for per-table foreign-key discovery.
///
/// Use [#importedKeysOnly()] for the portable JDBC path. Use
/// [#importedKeysAndCrossReference(String)] only when the driver requires
/// cross-reference rows to complete imported-key metadata, preserving the
/// parent-table filter exactly as required by that driver.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record ForeignKeyMetadataPolicy(
		Mode mode,
		@Nullable String crossReferenceParentTableFilter) {
	private static final ForeignKeyMetadataPolicy IMPORTED_KEYS_ONLY =
			new ForeignKeyMetadataPolicy( Mode.IMPORTED_KEYS_ONLY, null );

	public ForeignKeyMetadataPolicy {
		if ( mode == null ) {
			throw new IllegalArgumentException( "mode must not be null" );
		}
		if ( mode == Mode.IMPORTED_KEYS_ONLY && crossReferenceParentTableFilter != null ) {
			throw new IllegalArgumentException(
					"crossReferenceParentTableFilter requires IMPORTED_KEYS_AND_CROSS_REFERENCE"
			);
		}
	}

	/// Return the stable portable imported-keys-only policy.
	public static ForeignKeyMetadataPolicy importedKeysOnly() {
		return IMPORTED_KEYS_ONLY;
	}

	/// Return a policy which supplements imported keys with cross-reference rows.
	public static ForeignKeyMetadataPolicy importedKeysAndCrossReference(
			@Nullable String parentTableFilter) {
		return new ForeignKeyMetadataPolicy( Mode.IMPORTED_KEYS_AND_CROSS_REFERENCE, parentTableFilter );
	}

	/// JDBC foreign-key metadata modes.
	public enum Mode {
		IMPORTED_KEYS_ONLY,
		IMPORTED_KEYS_AND_CROSS_REFERENCE
	}
}
