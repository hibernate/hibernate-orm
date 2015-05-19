/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.boot.model.naming.Identifier;

/**
 * Provides access to information about existing primary key for a table
 *
 * @author Steve Ebersole
 */
public interface PrimaryKeyInformation {
	/**
	 * Obtain the identifier for this PK.
	 *
	 * @return The PK identifier.
	 */
	public Identifier getPrimaryKeyIdentifier();

	/**
	 * Obtain the columns making up the primary key.  Returned in sequential order.
	 *
	 * @return The columns
	 */
	public Iterable<ColumnInformation> getColumns();
}
