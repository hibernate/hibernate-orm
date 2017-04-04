/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;

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
	public QualifiedTableName getName();

	/**
	 * Does this information describe a physical table as opposed to a view, etc?
	 *
	 * @return {@code true} if this is a physical table; {@code false} otherwise.
	 */
	public boolean isPhysicalTable();

	/**
	 * Get the comments/remarks defined for the table.
	 *
	 * @return The table comments
	 */
	public String getComment();

	/**
	 * Retrieve the named ColumnInformation
	 *
	 * @param columnIdentifier The column identifier (simple name)
	 *
	 * @return The matching column information.  May return {@code null}
	 */
	public ColumnInformation getColumn(Identifier columnIdentifier);

	/**
	 * Retrieve information about the table's primary key, if one is defined (aka, may return {@code null}).
	 *
	 * @return The primary key information, or {@code null} if the table did not define a primary key.
	 */
	public PrimaryKeyInformation getPrimaryKey();

	/**
	 * Obtain an iterable over all the table's defined foreign keys.
	 *
	 * @return The iterable.
	 */
	public Iterable<ForeignKeyInformation> getForeignKeys();

	/**
	 * Retrieve the named ForeignKeyInformation
	 *
	 * @param keyName The foreign key identifier (simple name)
	 *
	 * @return The matching foreign key information.  May return {@code null}
	 */
	public ForeignKeyInformation getForeignKey(Identifier keyName);

	/**
	 * Obtain an iterable over all the table's defined indexes.
	 *
	 * @return The iterable.
	 */
	public Iterable<IndexInformation> getIndexes();

	/**
	 * Retrieve the named IndexInformation
	 * 
	 * @param indexName The index identifier (simple name)
	 * 
	 * @return The matching index information.  May return {@code null}
	 */
	public IndexInformation getIndex(Identifier indexName);

	public void addColumn(ColumnInformation columnIdentifier);
}
