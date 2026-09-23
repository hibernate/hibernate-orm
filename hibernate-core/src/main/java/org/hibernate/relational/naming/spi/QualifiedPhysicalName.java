/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// A finalized physical object name with optional catalog and schema qualifiers.
/// SQL rendering belongs to the configured qualified-name formatter.
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record QualifiedPhysicalName(PhysicalName catalogName, PhysicalName schemaName, PhysicalName objectName) {
	public QualifiedPhysicalName {
		requireNonNull( objectName, "objectName" );
	}

	public PhysicalName getCatalogName() { return catalogName; }
	public PhysicalName getSchemaName() { return schemaName; }
	public PhysicalName getObjectName() { return objectName; }

	/// Diagnostic/export identity, not dialect-specific SQL rendering.
	public String render() {
		return (catalogName == null ? "" : catalogName + ".")
				+ (schemaName == null ? "" : schemaName + ".") + objectName;
	}

	@Override
	public String toString() { return render(); }
}
