/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;

/**
 * Used to specify the {@link SchemaFilter}s to be used by create, drop, migrate and validate
 * operations on the database schema. These filters can be used to limit the scope of operations to specific namespaces,
 * tables and sequences.
 *
 * @since 5.1
 */
@Incubating
public interface SchemaFilterProvider {
	/**
	 * Get the filter to be applied to {@link SchemaCreator} processing
	 *
	 * @return The {@link SchemaCreator} filter
	 */
	SchemaFilter getCreateFilter();

	/**
	 * Get the filter to be applied to {@link SchemaDropper} processing
	 *
	 * @return The {@link SchemaDropper} filter
	 */
	SchemaFilter getDropFilter();

	/**
	 * Get the filter to be applied to {@link SchemaTruncator} processing
	 *
	 * @return The {@link SchemaTruncator} filter
	 */
	SchemaFilter getTruncatorFilter();

	/**
	 * Get the filter to be applied to {@link SchemaMigrator} processing
	 *
	 * @return The {@link SchemaMigrator} filter
	 */
	SchemaFilter getMigrateFilter();

	/**
	 * Get the filter to be applied to {@link SchemaValidator} processing
	 *
	 * @return The {@link SchemaValidator} filter
	 */
	SchemaFilter getValidateFilter();
}
