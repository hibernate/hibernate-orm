/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing.spi;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// A database-free behavior exercised by the Dialect contract test kit.
///
/// Required contracts must be applicable to every profile. Optional contracts
/// may be marked inapplicable by [DialectContractProfile#applicability].
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(USE)
public enum DialectContract {
	BOOTSTRAP(true),
	BASIC_QUERY_TRANSLATION(true),
	IDENTIFIER_AND_LITERAL_RENDERING(true),
	PARAMETER_MARKER_ORDER(true),
	TABLELESS_AND_SYNTHETIC_ROOTS(true),
	FETCH_AND_PAGINATION(false),
	LOCKING(false),
	SCHEMA_DDL(true),
	TEMPORARY_TABLES(false),
	MULTI_TABLE_MUTATION(false);

	private final boolean required;

	DialectContract(boolean required) {
		this.required = required;
	}

	/// Whether every profile must exercise this contract.
	public boolean isRequired() {
		return required;
	}
}
