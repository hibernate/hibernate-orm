/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational;

import org.hibernate.Incubating;

/**
 * Allows programmatic {@linkplain #exportMappedObjects schema export},
 * {@linkplain #validateMappedObjects schema validation},
 * {@linkplain #truncateMappedObjects data cleanup}, and
 * {@linkplain #dropMappedObjects schema cleanup} as a convenience for
 * writing tests.
 *
 * @see org.hibernate.SessionFactory#getSchemaManager()
 *
 * @apiNote This interface was added to JPA 3.2 as
 * {@link jakarta.persistence.SchemaManager}, which it now inherits,
 * with a minor change to the naming of its operations. It is retained
 * for backward compatibility and as a place to define additional
 * functionality such as {@link #populate()}.
 *
 * @since 6.2
 * @author Gavin King
 */
@Incubating
public interface SchemaManager extends jakarta.persistence.SchemaManager {
	/**
	 * Export database objects mapped by Hibernate entities, and then
	 * import initial data from {@code /import.sql} and any other configured
	 * {@linkplain org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
	 * load script}.
	 * <p>
	 * Programmatic way to run {@link org.hibernate.tool.schema.spi.SchemaCreator}.
	 *
	 * @param createSchemas if {@code true}, attempt to create schemas,
	 *                      otherwise, assume the schemas already exist
	 *
	 * @apiNote This operation is a synonym for {@link #create}.
	 */
	void exportMappedObjects(boolean createSchemas);

	/**
	 * Drop database objects mapped by Hibernate entities, undoing the
	 * {@linkplain #exportMappedObjects(boolean) previous export}.
	 * <p>
	 * Programmatic way to run {@link org.hibernate.tool.schema.spi.SchemaDropper}.
	 *
	 * @param dropSchemas if {@code true}, drop schemas,
	 *                    otherwise, leave them be
	 *
	 * @apiNote This operation is a synonym for {@link #drop}.
	 */
	void dropMappedObjects(boolean dropSchemas);

	/**
	 * Validate that the database objects mapped by Hibernate entities
	 * have the expected definitions.
	 * <p>
	 * Programmatic way to run {@link org.hibernate.tool.schema.spi.SchemaValidator}.
	 *
	 * @apiNote This operation is a synonym for {@link #validate}.
	 */
	void validateMappedObjects();

	/**
	 * Truncate the database tables mapped by Hibernate entities, and then
	 * reimport initial data from {@code /import.sql} and any other configured
	 * {@linkplain org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
	 * load script}.
	 * <p>
	 * Programmatic way to run {@link org.hibernate.tool.schema.spi.SchemaTruncator}.
	 * <p>
	 * This operation does not affect the {@linkplain org.hibernate.Cache second-level cache}.
	 * Therefore, after calling {@code truncate()}, it might be necessary to also call
	 * {@link org.hibernate.Cache#evictAllRegions} to clean up data held in the second-level
	 * cache.
	 *
	 * @apiNote This operation is a synonym for {@link #truncate}.
	 */
	void truncateMappedObjects();

	/**
	 * Populate the database by executing {@code /import.sql} and any other configured
	 * {@linkplain org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
	 * load script}.
	 * <p>
	 * Programmatic way to run {@link org.hibernate.tool.schema.spi.SchemaPopulator}.
	 *
	 * @since 7.0
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
	 */
	@Incubating
	void populate();

	/**
	 * Obtain an instance which targets the given schema.
	 * @param schemaName The name of the schema to target
	 *
	 * @since 7.1
	 */
	@Incubating
	SchemaManager forSchema(String schemaName);

	/**
	 * Obtain an instance which targets the given schema of the given catalog.
	 * @param schemaName The name of the schema to target
	 * @param catalogName The name of the catalog to target
	 *
	 * @since 7.1
	 */
	@Incubating
	SchemaManager forSchemaAndCatalog(String schemaName, String catalogName);
}
