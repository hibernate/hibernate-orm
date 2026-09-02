/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.USE;

/// Describes whether qualified database object names admit catalogs, schemas,
/// both namespace kinds, or neither.
///
/// This value does not define the catalog separator. A Dialect may report the
/// same qualifier support as another database while using a different
/// separator.
///
/// @author Steve Ebersole
/// @see Dialect#getNameQualifierSupport()
@SPI(USE)
public enum NameQualifierSupport {
	/// Only catalogs are supported.
	CATALOG,
	/// Only schemas are supported.
	SCHEMA,
	/// Both catalogs and schemas are supported.
	BOTH,
	/// Neither catalogs nor schemas are supported.
	NONE;

	/// Report whether catalog qualification is supported.
	public boolean supportsCatalogs() {
		return this == CATALOG || this == BOTH;
	}

	/// Report whether schema qualification is supported.
	public boolean supportsSchemas() {
		return this == SCHEMA || this == BOTH;
	}
}
