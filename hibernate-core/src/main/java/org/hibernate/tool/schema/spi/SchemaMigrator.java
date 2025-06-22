/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * @param contributableInclusionFilter Filter for Contributable instances to use
	 * @param targetDescriptor description of the target(s) for the alteration commands
	 */
	void doMigration(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			TargetDescriptor targetDescriptor);
}
