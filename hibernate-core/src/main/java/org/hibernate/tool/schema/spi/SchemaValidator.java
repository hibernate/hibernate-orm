/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;

/**
 * Service delegate for handling schema validations
 *
 * @author Steve Ebersole
 */
public interface SchemaValidator {
	/**
	 * Handle schema validation requests
	 *
	 * @param metadata The "compiled" mapping metadata.
	 * @param databaseInformation Access to the existing database information.
	 *
	 * @throws SchemaManagementException
	 */
	public void doValidation(
			Metadata metadata,
			DatabaseInformation databaseInformation) throws SchemaManagementException;
}
