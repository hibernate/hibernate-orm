/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

import org.hibernate.boot.model.naming.Identifier;

/// Contract for table references that carry catalog and schema names.
///
/// Logical names are the names requested by mapping sources after defaults are
/// applied.  Physical names are the database names after physical naming and quoting
/// rules have been applied.
///
/// @since 9.0
/// @author Steve Ebersole
public interface SchemaAware {
	/// Physical schema name used in the database model.
	Identifier getPhysicalSchemaName();

	/// Logical schema name requested by the mapping source or defaults.
	Identifier getLogicalSchemaName();

	/// Physical catalog name used in the database model.
	Identifier getPhysicalCatalogName();

	/// Logical catalog name requested by the mapping source or defaults.
	Identifier getLogicalCatalogName();
}
