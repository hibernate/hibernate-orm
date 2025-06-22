/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;

/**
 * Service delegate for handling schema validations
 */
@Incubating
public interface SchemaValidator {
	/**
	 * Perform the validation of the schema described by Metadata
	 *
	 * @param metadata Represents the schema to be validated
	 * @param options Options for executing the validation
	 * @param contributableInclusionFilter Filter for Contributable instances to use
	 */
	void doValidation(Metadata metadata, ExecutionOptions options, ContributableMatcher contributableInclusionFilter);
}
