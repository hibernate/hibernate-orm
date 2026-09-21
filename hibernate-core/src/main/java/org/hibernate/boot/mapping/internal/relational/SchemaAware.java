/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.relational;

import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

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
	PhysicalName getPhysicalSchemaName();

	/// Logical schema name requested by the mapping source or defaults.
	LogicalName getLogicalSchemaName();

	/// Physical catalog name used in the database model.
	PhysicalName getPhysicalCatalogName();

	/// Logical catalog name requested by the mapping source or defaults.
	LogicalName getLogicalCatalogName();
}
