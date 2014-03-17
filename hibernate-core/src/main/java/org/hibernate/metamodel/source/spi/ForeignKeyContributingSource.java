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
package org.hibernate.metamodel.source.spi;

import java.util.List;

import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * Additional contract for things which describe foreign keys.
 *
 * @author Steve Ebersole
 */
public interface ForeignKeyContributingSource {
	/**
	 * Retrieve the name of the foreign key as supplied by the user, or {@code null} if the user supplied none.
	 *
	 * @return The user supplied foreign key name.
	 */
	public String getExplicitForeignKeyName();
	
	/**
	 * Primarily exists to support JPA's @ForeignKey(NO_CONSTRAINT).
	 * 
	 * @return True if the FK constraint should be created, false if not.
	 */
	public boolean createForeignKeyConstraint();

	/**
	 * Is "cascade delete" enabled for the foreign key? In other words, if a record in the parent (referenced)
	 * table is deleted, should the corresponding records in the child table automatically be deleted?
	 *
	 * @return true, if the cascade delete is enabled; false, otherwise.
	 */
	public boolean isCascadeDeleteEnabled();

	/**
	 * Retrieve the delegate for resolving foreign key target columns.  This corresponds directly to
	 * HBM {@code <property-ref/>} and JPA {@link javax.persistence.JoinColumn} mappings.
	 * <p/>
	 * By default foreign keys target the primary key of the targeted table.  {@code <property-ref/>} and
	 * {@link javax.persistence.JoinColumn} mappings represents ways to instead target non-PK columns.  Implementers
	 * should return {@code null} to indicate targeting primary key columns.
	 *
	 * @return The delegate, or {@code null}
	 */
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate();

	/**
	 * By default foreign keys target the columns defined as the primary key of the targeted table.  This contract
	 * helps account for cases where other columns should be targeted instead.
	 */
	public static interface JoinColumnResolutionDelegate {
		/**
		 * Resolve the (other, non-PK) columns which should targeted by the foreign key.
		 *
		 * @param context The context for resolving those columns.
		 *
		 * @return The resolved target columns.
		 */
		public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context);

		public TableSpecification getReferencedTable(JoinColumnResolutionContext context);

		/**
		 * Retrieves the explicitly named attribute that maps to the non-PK foreign-key target columns.
		 *
		 * @return The explicitly named referenced attribute, or {@code null}.  This most likely always {@code null}
		 * 		from annotations cases.
		 */
		public String getReferencedAttributeName();
	}

	/**
	 * Means to allow the {@link JoinColumnResolutionDelegate} access to the relational values it needs.
	 */
	public static interface JoinColumnResolutionContext {
		/**
		 * Given an attribute name, resolve the columns.  This is used in the HBM {@code property-ref/>} case.
		 *
		 * @param attributeName The name of the referenced property.
		 *
		 * @return The corresponding referenced columns
		 */
		public List<? extends Value> resolveRelationalValuesForAttribute(String attributeName);

		public TableSpecification resolveTableForAttribute(String attributeName);

		/**
		 * Resolve a column reference given the logical names of both the table and the column.  Used in the
		 * {@link javax.persistence.JoinColumn} case
		 *
		 * @param logicalColumnName The logical column name.
		 * @param logicalTableName The logical table name.
		 * @param logicalSchemaName The logical schema name.
		 * @param logicalCatalogName The logical catalog name.
		 *
		 * @return The column.
		 */
		public Column resolveColumn(String logicalColumnName, String logicalTableName, String logicalSchemaName, String logicalCatalogName);

		/**
		 * Resolve a table reference given the logical names of the table and the column.  Used in the
		 * {@link javax.persistence.JoinColumn} case
		 *
		 * @param logicalTableName The logical table name.
		 * @param logicalSchemaName The logical schema name.
		 * @param logicalCatalogName The logical catalog name.
		 *
		 * @return The column.
		 */
		public TableSpecification resolveTable(String logicalTableName, String logicalSchemaName, String logicalCatalogName);
	}
}
