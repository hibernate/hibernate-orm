/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;

/**
 * Service delegate for handling schema population.
 *
 * @author Gavin King
 *
 * @since 7.0
 */
@Incubating
public interface SchemaPopulator {
	/**
	 * Perform schema population to the indicated target(s).
	 *
	 * @param options Options for executing the creation
	 * @param targetDescriptor description of the target(s) for the creation commands
	 */
	void doPopulation(ExecutionOptions options, TargetDescriptor targetDescriptor);
}
