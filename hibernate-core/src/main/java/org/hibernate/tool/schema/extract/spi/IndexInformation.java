/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.spi;

import java.util.List;

import org.hibernate.boot.model.naming.Identifier;

/**
 * Provides access to information about existing index in the database
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
public interface IndexInformation {
	/**
	 * Obtain the identifier for this index.
	 *
	 * @return The index identifier.
	 */
	public Identifier getIndexIdentifier();

	/**
	 * Obtain the columns indexed under this index.  Returned in sequential order.
	 *
	 * @return The columns
	 */
	public List<ColumnInformation> getIndexedColumns();
}
