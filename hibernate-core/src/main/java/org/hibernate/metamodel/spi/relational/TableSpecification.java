/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.spi.relational;

import org.hibernate.dialect.Dialect;

/**
 * Models what ANSI SQL terms a table specification which is a table or a view or an inline view.
 *
 * @author Steve Ebersole
 */
public interface TableSpecification extends ValueContainer, Loggable {
	/**
	 * Obtain a reference to the schema to which this table specification belongs.
	 *
	 * @return The schema to which this table specification belongs.
	 */
	public Schema getSchema();

	/**
	 *  Gets the logical table name.
	 *
	 *  @return the logical table name.
	 */
	public Identifier getLogicalName();

	/**
	 * Get the table number.
	 *
	 * @return the table number.
	 */
	public int getTableNumber();

	/**
	 * Get the primary key definition for this table spec.
	 *
	 * @return The PK definition.
	 */
	public PrimaryKey getPrimaryKey();

	/**
	 * Factory method for creating a {@link Column} associated with this container.
	 *
	 * @param name The column name
	 *
	 * @return The generated column
	 */
	public Column locateOrCreateColumn(String name);

	/**
	 * Attempt to locate a column with the given name
	 *
	 * @param name The column name
	 *
	 * @return The located column, or {@code null} is none found
	 */
	public Column locateColumn(String name);

	public Column createColumn(String name);

	public Column createColumn(Identifier name);

	/**
	 * Factory method for creating a {@link DerivedValue} associated with this container.
	 *
	 * @param fragment The value expression
	 *
	 * @return The generated value.
	 */
	public DerivedValue locateOrCreateDerivedValue(String fragment);

	/**
	 * Generates a unique ID for the specified columns in this table.
	 *
	 * @param columns - the columns used to generate the ID
	 * @return the ID unique to the specified columns in this table.
	 */
	public int columnListId(Iterable<Column> columns);

	/**
	 * Retrieve a read-only version of foreign keys currently defined for this table.
	 *
	 * @return  a reforeign keys defined on this table.
	 */
	public Iterable<ForeignKey> getForeignKeys();

	/**
	 * Locate a foreign key by name
	 *
	 * @param name The name of the foreign key to locate
	 *
	 * @return The foreign key, or {@code null} to indicate none with that name was found.
	 */
	public ForeignKey locateForeignKey(String name);

	/**
	 * Locate foreign keys with {@code this} table as source and the passed table as the target.
	 *
	 * @param targetTable The table that is the target of interest.
	 *
	 * @return The matching foreign keys, or {@code null} to indicate none were found.
	 */
	public Iterable<ForeignKey> locateForeignKey(TableSpecification targetTable);

	/**
	 * Create a foreign key targeting the specified table as the target.  Columns should be handled through the
	 * returned reference.
	 *
	 * @param targetTable The table that is the target of the foreign key
	 * @param name The (optional) name of the foreign key
	 * @param createConstraint
	 *
	 * @return The foreign key reference.
	 */
	public ForeignKey createForeignKey(TableSpecification targetTable, String name, boolean createConstraint);
	
	public ForeignKey createForeignKey(TableSpecification targetTable, Identifier name, boolean createConstraint);

	public Iterable<Index> getIndexes();

	public void addIndex(Index idx);

	public Iterable<UniqueKey> getUniqueKeys();

	public void addUniqueKey(UniqueKey uk);
	
	public boolean hasUniqueKey(Column column);

	public Iterable<CheckConstraint> getCheckConstraints();

	public void addCheckConstraint(String checkCondition);

	public Iterable<String> getComments();

	public void addComment(String comment);

	public String getQualifiedName(Dialect dialect);
}
