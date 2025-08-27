/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;

/**
 * Service delegate for handling schema creation.
 * <p>
 * The actual contract here is kind of convoluted with the design
 * idea of allowing this to work in ORM (JDBC) as well as in non-JDBC
 * environments (OGM, e.g.) simultaneously.
 */
@Incubating
public interface SchemaCreator {
	/**
	 * Perform a schema creation from the indicated source(s) to the indicated target(s).
	 *
	 * @param metadata Represents the schema to be created.
	 * @param options Options for executing the creation
	 * @param contributableInclusionFilter Filter for Contributable instances to use
	 * @param sourceDescriptor description of the source(s) of creation commands
	 * @param targetDescriptor description of the target(s) for the creation commands
	 */
	void doCreation(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			SourceDescriptor sourceDescriptor,
			TargetDescriptor targetDescriptor);
}
