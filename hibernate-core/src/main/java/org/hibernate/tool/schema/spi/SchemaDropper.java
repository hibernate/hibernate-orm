/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;

/**
 * Service delegate for handling schema dropping.
 */
@Incubating
public interface SchemaDropper {
	/**
	 * Perform a schema drop from the indicated source(s) to the indicated target(s).
	 *
	 * @param metadata Represents the schema to be dropped.
	 * @param options Options for executing the drop
	 * @param contributableInclusionFilter Filter for Contributable instances to use
	 * @param sourceDescriptor description of the source(s) of drop commands
	 * @param targetDescriptor description of the target(s) for the drop commands
	 */
	void doDrop(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			SourceDescriptor sourceDescriptor,
			TargetDescriptor targetDescriptor);

	/**
	 * Build a delayed Runnable for performing schema dropping.  This implicitly
	 * targets the underlying data-store.
	 *
	 * @param metadata The metadata to drop
	 * @param options The drop options
	 * @param contributableInclusionFilter Filter for Contributable instances to use
	 * @param sourceDescriptor For access to the {@link SourceDescriptor#getScriptSourceInput()}
	 *
	 * @return The Runnable
	 */
	DelayedDropAction buildDelayedAction(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			SourceDescriptor sourceDescriptor);
}
