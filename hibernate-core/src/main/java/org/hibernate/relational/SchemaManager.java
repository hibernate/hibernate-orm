/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 */
	void dropMappedObjects(boolean dropSchemas);

	/**
	 * Validate that the database objects mapped by Hibernate entities
	 * have the expected definitions.
	 * <p>
	 * Programmatic way to run {@link org.hibernate.tool.schema.spi.SchemaValidator}.
	 */
	void validateMappedObjects();

	/**
	 * Truncate the database tables mapped by Hibernate entities, and
	 * then re-import initial data from any configured
	 * {@linkplain org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
	 * load script}.
	 * <p>
	 * Programmatic way to run {@link org.hibernate.tool.schema.spi.SchemaTruncator}.
	 */
	void truncateMappedObjects();

	@Override
	default void create(boolean createSchemas) {
		exportMappedObjects( createSchemas );
	}

	@Override
	default void drop(boolean dropSchemas) {
		drop( dropSchemas );
	}
}
