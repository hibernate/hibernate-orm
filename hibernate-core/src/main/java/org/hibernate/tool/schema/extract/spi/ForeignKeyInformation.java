/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.boot.model.naming.Identifier;

/**
 * @author Steve Ebersole
 */
public interface ForeignKeyInformation {
	/**
	 * Obtain the identifier for this FK.
	 *
	 * @return The FK identifier.
	 */
	Identifier getForeignKeyIdentifier();

	/**
	 * Get the column mappings that define the reference.  Returned in sequential order.
	 *
	 * @return The sequential column reference mappings.
	 */
	Iterable<ColumnReferenceMapping> getColumnReferenceMappings();

	interface ColumnReferenceMapping {
		/**
		 * Obtain the information about the referencing column (the source column, which points to
		 * the referenced column).
		 *
		 * @return The referencing column.
		 */
		ColumnInformation getReferencingColumnMetadata();

		/**
		 * Obtain the information about the referenced column (the target side).
		 *
		 * @return The referenced column
		 */
		ColumnInformation getReferencedColumnMetadata();
	}
}
