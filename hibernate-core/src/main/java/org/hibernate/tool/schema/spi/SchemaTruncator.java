/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;

/**
 * Service delegate for handling schema truncation.
 *
 * @author Gavin King
 *
 * @since 6.2
 */
@Incubating
public interface SchemaTruncator {
	/**
	 * Perform schema truncation from the indicated source(s) to the indicated target(s).
	 * @param metadata Represents the schema to be dropped.
	 * @param options Options for executing the drop
	 * @param contributableInclusionFilter Filter for Contributable instances to use
	 * @param targetDescriptor description of the target(s) for the drop commands
	 */
	void doTruncate(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			TargetDescriptor targetDescriptor);

}
