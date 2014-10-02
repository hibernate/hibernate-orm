/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.cfg.naming;

/**
 * @author Gail Badner
 */
public interface NamingStrategyDelegate {

	public String determinePrimaryTableLogicalName(String entityName, String jpaEntityName);

	public String determineAttributeColumnName(String propertyName);

	/**
	 * Alter the table name given in the mapping document
	 * @param tableName a table name
	 * @return a table name
	 */
	public String toPhysicalTableName(String tableName);

	/**
	 * Alter the column name given in the mapping document
	 * @param columnName a column name
	 * @return a column name
	 */
	public String toPhysicalColumnName(String columnName);


	public String determineElementCollectionTableLogicalName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String propertyName);

	public String determineElementCollectionForeignKeyColumnName(
			String propertyName,
			String propertyEntityName,
			String propertyJpaEntityName,
			String propertyTableName,
			String referencedColumnName);


	public String determineEntityAssociationJoinTableLogicalName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String associatedEntityName,
			String associatedJpaEntityName,
			String associatedEntityTable,
			String propertyName);

	public String determineEntityAssociationForeignKeyColumnName(
			String propertyName,
			String propertyEntityName,
			String propertyJpaEntityName,
			String propertyTableName,
			String referencedColumnName);

	public String determineJoinKeyColumnName(String joinedColumn, String joinedTable);

	public String logicalColumnName(String columnName, String propertyName);

	public String logicalElementCollectionTableName(
			String tableName,
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String propertyName);

	public String logicalEntityAssociationJoinTableName(
			String tableName,
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String associatedEntityName,
			String associatedJpaEntityName,
			String associatedEntityTable,
			String propertyName);

	public String logicalCollectionColumnName(String columnName, String propertyName, String referencedColumn);
}
