/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.function.Function;

import org.hibernate.dialect.namespace.spi.NamespaceSupport;

/// Provider-owned namespace lifecycle strategy used by community Dialects with
/// custom commands.
///
/// @author Steve Ebersole
/// @since 8.0
final class CommunityNamespaceSupport implements NamespaceSupport {
	private final boolean canCreateCatalog;
	private final Function<String, String[]> createCatalogCommands;
	private final Function<String, String[]> dropCatalogCommands;
	private final boolean canCreateSchema;
	private final Function<String, String[]> createSchemaCommands;
	private final Function<String, String[]> dropSchemaCommands;

	CommunityNamespaceSupport(
			boolean canCreateCatalog,
			Function<String, String[]> createCatalogCommands,
			Function<String, String[]> dropCatalogCommands,
			boolean canCreateSchema,
			Function<String, String[]> createSchemaCommands,
			Function<String, String[]> dropSchemaCommands) {
		this.canCreateCatalog = canCreateCatalog;
		this.createCatalogCommands = createCatalogCommands;
		this.dropCatalogCommands = dropCatalogCommands;
		this.canCreateSchema = canCreateSchema;
		this.createSchemaCommands = createSchemaCommands;
		this.dropSchemaCommands = dropSchemaCommands;
	}

	@Override
	public boolean canCreateCatalog() {
		return canCreateCatalog;
	}

	@Override
	public String[] getCreateCatalogCommands(String catalogName) {
		return createCatalogCommands.apply( catalogName );
	}

	@Override
	public String[] getDropCatalogCommands(String catalogName) {
		return dropCatalogCommands.apply( catalogName );
	}

	@Override
	public boolean canCreateSchema() {
		return canCreateSchema;
	}

	@Override
	public String[] getCreateSchemaCommands(String schemaName) {
		return createSchemaCommands.apply( schemaName );
	}

	@Override
	public String[] getDropSchemaCommands(String schemaName) {
		return dropSchemaCommands.apply( schemaName );
	}

	static String[] unsupportedCatalog(String name) {
		throw new UnsupportedOperationException( "Catalog lifecycle is not supported" );
	}

	static String[] unsupportedSchema(String name) {
		throw new UnsupportedOperationException( "Schema lifecycle is not supported" );
	}
}
