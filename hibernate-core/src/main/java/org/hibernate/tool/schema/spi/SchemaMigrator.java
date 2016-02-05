/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;

/**
 * Service delegate for handling schema migration.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SchemaMigrator {
	/**
	 * Perform a schema migration (alteration) from the indicated source(s) to the indicated target(s).
	 *
	 * @param metadata Represents the schema to be altered.
	 * @param options Options for executing the alteration
	 * @param targetDescriptor description of the target(s) for the alteration commands
	 */
	void doMigration(Metadata metadata, ExecutionOptions options, TargetDescriptor targetDescriptor);
}
