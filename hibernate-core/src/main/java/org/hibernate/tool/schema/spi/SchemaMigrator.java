/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;

/**
 * Service delegate for handling schema migration.
 *
 * @author Steve Ebersole
 */
public interface SchemaMigrator {
	/**
	 * Perform a migration to the specified targets.
	 *
	 * @param metadata The "compiled" mapping metadata.
	 * @param existingDatabase Access to the information about the existing database.
	 * @param createNamespaces Should the schema(s)/catalog(s) actually be created?
	 * @param targets The migration targets
	 *
	 * @throws SchemaManagementException
	 */
	public void doMigration(
			Metadata metadata,
			DatabaseInformation existingDatabase,
			boolean createNamespaces,
			List<Target> targets) throws SchemaManagementException;
}
