/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;

/**
 * Used to specify the {@link org.hibernate.tool.schema.spi.SchemaFilter}s to be used by create, drop, migrate and validate
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
