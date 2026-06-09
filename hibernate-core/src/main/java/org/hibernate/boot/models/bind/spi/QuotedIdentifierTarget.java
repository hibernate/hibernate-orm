/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

/// Identifier categories affected by global quoting options.
///
/// Binding uses these targets to decide which logical names or SQL fragments should
/// be quoted before they are applied to the boot-time mapping model.
///
/// @since 9.0
/// @author Steve Ebersole
public enum QuotedIdentifierTarget {
	/// Catalog names.
	CATALOG_NAME,

	/// Schema names.
	SCHEMA_NAME,

	/// Table and view names.
	TABLE_NAME,

	/// Sequence names.
	SEQUENCE_NAME,

	/// Callable object names.
	CALLABLE_NAME,

	/// Foreign-key names.
	FOREIGN_KEY,

	/// Foreign-key SQL definitions.
	FOREIGN_DEFINITION,

	/// Index names.
	INDEX,

	/// Type names.
	TYPE_NAME,

	/// Column names.
	COLUMN_NAME,

	/// Column SQL definitions.
	COLUMN_DEFINITION
}
