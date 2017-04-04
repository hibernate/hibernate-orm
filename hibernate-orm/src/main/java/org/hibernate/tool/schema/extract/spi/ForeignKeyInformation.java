/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public Identifier getForeignKeyIdentifier();

	/**
	 * Get the column mappings that define the reference.  Returned in sequential order.
	 *
	 * @return The sequential column reference mappings.
	 */
	public Iterable<ColumnReferenceMapping> getColumnReferenceMappings();

	public static interface ColumnReferenceMapping {
		/**
		 * Obtain the information about the referencing column (the source column, which points to
		 * the referenced column).
		 *
		 * @return The referencing column.
		 */
		public ColumnInformation getReferencingColumnMetadata();

		/**
		 * Obtain the information about the referenced column (the target side).
		 *
		 * @return The referenced column
		 */
		public ColumnInformation getReferencedColumnMetadata();
	}
}
