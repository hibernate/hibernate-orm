/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.namespace.spi.NamespaceSupport;

/// Provider-owned catalog and schema lifecycle strategy used by the standalone
/// Dialect fixture.
///
/// @author Steve Ebersole
/// @since 8.0
public final class ExampleNamespaceSupport implements NamespaceSupport {
	public static final ExampleNamespaceSupport INSTANCE = new ExampleNamespaceSupport();

	private ExampleNamespaceSupport() {
	}

	@Override
	public boolean canCreateCatalog() {
		return true;
	}

	@Override
	public String[] getCreateCatalogCommands(String catalogName) {
		return new String[] {
				"create fixture catalog " + catalogName,
				"initialize fixture catalog " + catalogName
		};
	}

	@Override
	public String[] getDropCatalogCommands(String catalogName) {
		return new String[] { "drop fixture catalog " + catalogName };
	}

	@Override
	public boolean canCreateSchema() {
		return true;
	}

	@Override
	public String[] getCreateSchemaCommands(String schemaName) {
		return new String[] { "create fixture schema if not exists " + schemaName };
	}

	@Override
	public String[] getDropSchemaCommands(String schemaName) {
		return new String[] { "drop fixture schema if exists " + schemaName };
	}
}
