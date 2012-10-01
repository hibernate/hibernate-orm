/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.ObjectName;

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
	public ObjectName getName();

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
	 * Get an iterable over all of the table's columns.
	 *
	 * @return All of the table's columns
	 */
	public Iterable<ColumnInformation> getColumns();

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
	 * Obtain an iterable over all the table's defined indexes
	 */
	public IndexInformation getIndex(Identifier indexName);
}
