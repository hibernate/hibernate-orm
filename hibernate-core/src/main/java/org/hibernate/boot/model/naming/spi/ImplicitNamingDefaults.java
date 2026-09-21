/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

/// Effective defaults consumed by implicit naming strategies.
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public interface ImplicitNamingDefaults {
	String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
	String DEFAULT_TENANT_IDENTIFIER_COLUMN_NAME = "tenant_id";
	String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";

	/// Whether database identifiers are quoted by default.
	boolean isDefaultQuoteIdentifiers();

	/// The default identifier column name, falling back to {@value #DEFAULT_IDENTIFIER_COLUMN_NAME}.
	String getDefaultIdColumnName();

	/// The default discriminator column name, falling back to {@value #DEFAULT_DISCRIMINATOR_COLUMN_NAME}.
	String getDefaultDiscriminatorColumnName();

	/// The configured tenant identifier column name, falling back to {@value #DEFAULT_TENANT_IDENTIFIER_COLUMN_NAME}.
	/// Supplied implicit naming strategies use the tenant attribute name instead;
	/// custom strategies may choose to use this configured value.
	String getDefaultTenantIdColumnName();
}
