/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg;

import java.util.List;


/**
 * A set of rules for determining the physical column
 * and table names given the information in the mapping
 * document. May be used to implement project-scoped
 * naming standards for database objects.
 *
 * #propertyToTableName(String, String) should be replaced by
 * {@link #collectionTableName(String,String,String,String,String)}
 *
 * @see DefaultNamingStrategy
 * @see ImprovedNamingStrategy
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public interface NamingStrategy {
	/**
	 * Return a table name for an entity class
	 * @param className the fully-qualified class name
	 * @return a table name
	 */
	public String classToTableName(String className);
	/**
	 * Return a column name for a property path expression
	 * @param propertyName a property path
	 * @return a column name
	 */
	public String propertyToColumnName(String propertyName);
	/**
	 * Alter the table name given in the mapping document
	 * @param tableName a table name
	 * @return a table name
	 */
	public String tableName(String tableName);
	/**
	 * Alter the column name given in the mapping document
	 * @param columnName a column name
	 * @return a column name
	 */
	public String columnName(String columnName);
	/**
	 * Return a collection table name ie an association having a join table
	 *
	 * @param ownerEntity
	 * @param ownerEntityTable owner side table name
	 * @param associatedEntity
	 * @param associatedEntityTable reverse side table name if any
	 * @param propertyName collection role
	 */
	public String collectionTableName(
			String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable,
			String propertyName
	);
	/**
	 * Return the join key column name ie a FK column used in a JOINED strategy or for a secondary table
	 *
	 * @param joinedColumn joined column name (logical one) used to join with
	 * @param joinedTable joined table name (ie the referenced table) used to join with
	 */
	public String joinKeyColumnName(String joinedColumn, String joinedTable);
	/**
	 * Return the foreign key column name for the given parameters
	 * @param propertyName the property name involved
	 * @param propertyEntityName
	 * @param propertyTableName the property table name involved (logical one)
	 * @param referencedColumnName the referenced column name involved (logical one)
	 */
	public String foreignKeyColumnName(
			String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName
	);
	/**
	 * Return a constraint name for foreign keys that are not explicitly named in the mappings/annotations.
	 * 
	 * @param tableName The foreign key's source table
	 * @param columnNames The source columns within the foreign key
	 * @param tableName The foreign key's target table
	 * @param columnNames The target columns within the foreign key
	 * @return The generated foreign key constraint name
	 */
	public String foreignKeyName(String sourceTableName, List<String> sourceColumnNames,
			String targetTableName, List<String> targetColumnNames);
	/**
	 * Return a constraint name for unique keys that are not explicitly named in the mappings/annotations.
	 * 
	 * @param tableName The unique key's table
	 * @param columnNames The columns within the unique key
	 * @return The generated unique key constraint name
	 */
	public String uniqueKeyName(String tableName, List<String> columnNames);
	/**
	 * Return a constraint name for indexes that are not explicitly named in the mappings/annotations.
	 * 
	 * @param tableName The index's table
	 * @param columnNames The columns within the index
	 * @return The generated index constraint name
	 */
	public String indexName(String tableName, List<String> columnNames);
	/**
	 * Return the logical column name used to refer to a column in the metadata
	 * (like index, unique constraints etc)
	 * A full bijection is required between logicalNames and physical ones
	 * logicalName have to be case insersitively unique for a given table
	 *
	 * @param columnName given column name if any
	 * @param propertyName property name of this column
	 */
	public String logicalColumnName(String columnName, String propertyName);
	/**
	 * Returns the logical collection table name used to refer to a table in the mapping metadata
	 *
	 * @param tableName the metadata explicit name
	 * @param ownerEntityTable owner table entity table name (logical one)
	 * @param associatedEntityTable reverse side table name if any (logical one)
	 * @param propertyName collection role
	 */
	public String logicalCollectionTableName(String tableName, String ownerEntityTable, String associatedEntityTable, String propertyName);

	/**
	 * Returns the logical foreign key column name used to refer to this column in the mapping metadata
	 *
	 * @param columnName given column name in the metadata if any
	 * @param propertyName property name
	 * @param referencedColumn referenced column name (logical one) in the join
	 */
	public String logicalCollectionColumnName(String columnName, String propertyName, String referencedColumn);
}
