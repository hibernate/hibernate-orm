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
