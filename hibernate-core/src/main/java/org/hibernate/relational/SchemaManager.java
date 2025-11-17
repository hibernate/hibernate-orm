/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational;

import org.hibernate.Incubating;
import org.hibernate.tool.schema.spi.GeneratorSynchronizer;

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
 * operations like {@link #populate}, {@link #resynchronizeGenerators},
 * and {@link #forSchema}.
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
	 * Truncate the database tables mapped by Hibernate entities, reset all associated
	 * {@linkplain jakarta.persistence.SequenceGenerator sequences} and tables backing
	 * {@linkplain jakarta.persistence.TableGenerator table generators}, and then
	 * reimport initial data from {@code /import.sql} and any other configured
	 * {@linkplain org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
	 * load script}.
	 * <p>
	 * Programmatic way to run {@link org.hibernate.tool.schema.spi.SchemaTruncator}.
	 * <p>
	 * This operation does not affect the {@linkplain org.hibernate.Cache second-level cache}.
	 * Therefore, after calling {@code truncateMappedObjects()}, it might be necessary to
	 * also call {@link org.hibernate.Cache#evictAllRegions} to clean up data held in the
	 * second-level cache.
	 *
	 * @apiNote This operation is a synonym for {@link #truncate}.
	 */
	void truncateMappedObjects();

	/**
	 * Truncate the given database table, and reset any associated
	 * {@linkplain jakarta.persistence.SequenceGenerator sequence} or table backing a
	 * {@linkplain jakarta.persistence.TableGenerator table generator}.
	 * Do not repopulate the table.
	 * <p>
	 * This operation does not affect the {@linkplain org.hibernate.Cache second-level cache}.
	 * Therefore, after calling {@code truncate()}, it might be necessary to also call
	 * {@link org.hibernate.Cache#evictRegion(String)} to clean up data held in the
	 * second-level cache.
	 *
	 * @param tableName The name of the table to truncate, which must be a table mapped by
	 *                  some entity class or collection
	 *
	 * @since 7.2
	 */
	@Incubating
	void truncateTable(String tableName);

	/**
	 * Populate the database by executing {@code /import.sql} and any other configured
	 * {@linkplain org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
	 * load script}.
	 * <p>
	 * This operation does not automatically resynchronize sequences or tables backing
	 * {@linkplain jakarta.persistence.TableGenerator table generators}, and so it might
	 * be necessary to call {@link #resynchronizeGenerators} after calling this method.
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
	 * Resynchronize {@linkplain jakarta.persistence.SequenceGenerator sequences} and
	 * {@linkplain jakarta.persistence.TableGenerator table-based generators} after
	 * importing entity data.
	 * <p>
	 * When data is imported to the database without the use of a Hibernate session,
	 * a database sequence might become stale with respect to the data in the table for
	 * which it is used to generate unique keys. This operation restarts every sequence
	 * so that the next generated unique key will be larger than the largest key
	 * currently in use. A similar phenomenon might occur for the database table backing
	 * a table-based generator, and so this operation also updates such tables.
	 * <p>
	 * Programmatic way to run {@link GeneratorSynchronizer}.
	 *
	 * @since 7.2
	 */
	@Incubating
	void resynchronizeGenerators();

	/**
	 * Obtain an instance which targets the given schema.
	 * <p>
	 * This is especially useful when using multiple schemas, for example, in
	 * {@linkplain org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_SCHEMA_MAPPER
	 * schema-based multitenancy}.
	 *
	 * @param schemaName The name of the schema to target
	 *
	 * @since 7.1
	 */
	@Incubating
	SchemaManager forSchema(String schemaName);

	/**
	 * Obtain an instance which targets the given catalog.
	 *
	 * @param catalogName The name of the catalog to target
	 *
	 * @since 7.1
	 */
	@Incubating
	SchemaManager forCatalog(String catalogName);
}
