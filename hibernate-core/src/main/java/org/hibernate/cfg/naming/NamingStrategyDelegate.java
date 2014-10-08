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
 * A
 * @author Gail Badner
 */
public interface NamingStrategyDelegate {

	/**
	 * Determine the name of a entity's primary table when a name is not explicitly configured.
	 *
	 * @param entityName The fully qualified entity name
	 * @param jpaEntityName The entity name provided by the {@link javax.persistence.Entity}
	 *                      {@code name} attribute; or, if not mapped in this way, then the
	 *                      unqualified entity name.
	 *
	 * @return The implicit table name.
	 */
	public String determineImplicitPrimaryTableName(String entityName, String jpaEntityName);

	/**
	 * Determine the name of a property's column when a name is not explicitly configured.
	 *
	 * @param propertyPath The property path (not qualified by the entity name)
	 * @return The implicit column name.
	 */
	public String determineImplicitPropertyColumnName(String propertyPath);

	/**
	 * Determine the name of a collection table for a collection of (non-entity) values
	 * when a name is not explicitly configured.
	 *
	 * @param ownerEntityName The fully qualified entity name for the entity that owns the collection.
	 * @param ownerJpaEntityName The entity name provided by the {@link javax.persistence.Entity}
	 *                      {@code name} attribute for the entity that owns the collection;
	 *                      or, if not mapped in this way, then the unqualified owner entity name.
	 * @param ownerEntityTable The owner entity's physical primary table name;
	 * @param propertyPath The property path (not qualified by the entity name),
	 * @return The implicit table name.
	 */
	public String determineImplicitElementCollectionTableName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String propertyPath);

	/**
	 * Determine the name of the join column in a collection table for
	 * a collection of (non-entity) values when a name is not explicitly configured.
	 *
	 * @param ownerEntityName The fully qualified name of the entity that owns the collection.
	 * @param ownerJpaEntityName The entity name provided by the {@link javax.persistence.Entity}
	 *                      {@code name} attribute for the entity that owns the collection;
	 *                      or, if not mapped in this way, then the unqualified entity name.
	 * @param ownerEntityTable The owner entity's physical primary table name;
	 * @param referencedColumnName The physical name of the column that the join column references.
	 * @param propertyPath The property path (not qualified by the entity name),
	 * @return The implicit column name.
	 */
	public String determineImplicitElementCollectionJoinColumnName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String referencedColumnName,
			String propertyPath);

	/**
	 * Determine the name of the join table for an entity (singular or plural) association when
	 * a name is not explicitly configured.
	 *
	 * @param ownerEntityName The fully qualified name of the entity that owns the association;.
	 * @param ownerJpaEntityName The entity name provided by the {@link javax.persistence.Entity}
	 *                      {@code name} attribute for the entity that owns the association;
	 *                      or, if not mapped in this way, then the unqualified owner entity name.
	 * @param ownerEntityTable The owner entity's physical primary table name;
	 * @param associatedEntityName The fully qualified name of the associated entity.
	 * @param associatedJpaEntityName The entity name provided by the {@link javax.persistence.Entity}
	 *                      {@code name} attribute for the associated entity;
	 *                      or, if not mapped in this way, then the unqualified associated entity name.
	 * @param associatedEntityTable The associated entity's physical primary table name;
	 * @param propertyPath The property path (not qualified by the entity name),
	 * @return The implicit table name.
	 */
	public String determineImplicitEntityAssociationJoinTableName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String associatedEntityName,
			String associatedJpaEntityName,
			String associatedEntityTable,
			String propertyPath);

	/**
	 * Determine the name of join column for an entity (singular or plural) association when
	 * a name is not explicitly configured.
	 *
	 * @param propertyEntityName The fully qualified name of the entity that contains the association;
	 * @param propertyJpaEntityName The entity name provided by the {@link javax.persistence.Entity}
	 *                      {@code name} attribute for the entity that contains the association;
	 *                      or, if not mapped in this way, then the unqualified entity name.
	 * @param propertyTableName The physical primary table name for the entity that contains the association.
	 * @param referencedColumnName  The physical name of the column that the join column references.
	 * @param propertyPath The property path (not qualified by the entity name),
	 * @return The implicit table name.
	 */
	public String determineImplicitEntityAssociationJoinColumnName(
			String propertyEntityName,
			String propertyJpaEntityName,
			String propertyTableName,
			String referencedColumnName,
			String propertyPath);

	public String toPhysicalJoinKeyColumnName(String joinedColumn, String joinedTable);

	public String determineLogicalColumnName(String columnName, String propertyName);

	public String determineLogicalElementCollectionTableName(
			String tableName,
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String propertyName);

	public String determineLogicalEntityAssociationJoinTableName(
			String tableName,
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String associatedEntityName,
			String associatedJpaEntityName,
			String associatedEntityTable,
			String propertyName);

	public String determineLogicalCollectionColumnName(String columnName, String propertyName, String referencedColumn);

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

}
