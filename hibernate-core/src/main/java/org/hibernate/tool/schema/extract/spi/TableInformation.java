/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedTableName;

/**
 * Provides access to information about existing tables in the database
 *
 * @author Christoph Sturm
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public interface TableInformation {
	/**
	 * Get the qualified name of the table.
	 *
	 * @return The qualified table name
	 */
	QualifiedTableName getName();

	/**
	 * Does this information describe a physical table as opposed to a view, etc?
	 *
	 * @return {@code true} if this is a physical table; {@code false} otherwise.
	 */
	boolean isPhysicalTable();

	/**
	 * Get the comments/remarks defined for the table.
	 *
	 * @return The table comments
	 */
	String getComment();

	/**
	 * Retrieve the named ColumnInformation
	 *
	 * @param columnIdentifier The column identifier (simple name)
	 *
	 * @return The matching column information.  May return {@code null}
	 */
	ColumnInformation getColumn(Identifier columnIdentifier);

	/**
	 * Retrieve information about the table's primary key, if one is defined (aka, may return {@code null}).
	 *
	 * @return The primary key information, or {@code null} if the table did not define a primary key.
	 */
	PrimaryKeyInformation getPrimaryKey();

	/**
	 * Obtain an iterable over all the table's defined foreign keys.
	 *
	 * @return The iterable.
	 */
	Iterable<ForeignKeyInformation> getForeignKeys();

	/**
	 * Retrieve the named ForeignKeyInformation
	 *
	 * @param keyName The foreign key identifier (simple name)
	 *
	 * @return The matching foreign key information.  May return {@code null}
	 */
	ForeignKeyInformation getForeignKey(Identifier keyName);

	/**
	 * Obtain an iterable over all the table's defined indexes.
	 *
	 * @return The iterable.
	 */
	Iterable<IndexInformation> getIndexes();

	/**
	 * Retrieve the named IndexInformation
	 *
	 * @param indexName The index identifier (simple name)
	 *
	 * @return The matching index information.  May return {@code null}
	 */
	IndexInformation getIndex(Identifier indexName);

	void addColumn(ColumnInformation columnIdentifier);
}
