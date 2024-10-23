/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
 * functionality in the future.
 *
 * @since 6.2
 * @author Gavin King
 */
@Incubating
public interface SchemaManager extends jakarta.persistence.SchemaManager {
	/**
	 * Export database objects mapped by Hibernate entities.
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
	 * Truncate the database tables mapped by Hibernate entities, and
	 * then re-import initial data from any configured
	 * {@linkplain org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
	 * load script}.
	 * <p>
	 * Programmatic way to run {@link org.hibernate.tool.schema.spi.SchemaTruncator}.
	 *
	 * @apiNote This operation is a synonym for {@link #truncate}.
	 */
	void truncateMappedObjects();
}
