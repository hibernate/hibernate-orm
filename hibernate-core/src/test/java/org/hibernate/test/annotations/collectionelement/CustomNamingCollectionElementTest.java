/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.collectionelement;

import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.naming.AbstractLegacyNamingStrategyDelegate;
import org.hibernate.cfg.naming.LegacyHbmNamingStrategyDelegate;
import org.hibernate.cfg.naming.LegacyNamingStrategyDelegate;
import org.hibernate.cfg.naming.LegacyNamingStrategyDelegator;
import org.hibernate.cfg.naming.NamingStrategyDelegate;
import org.hibernate.testing.TestForIssue;

/**
 * @author Gail Badner
 */
public class CustomNamingCollectionElementTest extends CollectionElementTest {

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategyDelegator( new MyLegacyNamingStrategyDelegator() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerEntityNameAndPKColumnOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		// MyNamingStrategyDelegator will use the owner primary table name (instead of JPA entity name) in generated collection table.
		checkDefaultCollectionTableName( Matrix.class, "mvalues", "Mtx_mvalues" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableAndEntityNamesOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		// MyNamingStrategyDelegator will use owner primary table name (instead of JPA entity name) in generated collection table.
		checkDefaultCollectionTableName( Owner.class, "elements", "OWNER_TABLE_elements" );
	}


	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerEntityNameAndPKColumnOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		// MyNamingStrategyDelegator will use owner primary table name, which will default to the JPA entity name
		// in generated join column.
		checkDefaultJoinColumnName( Matrix.class, "mvalues", "Mtx_mId" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableAndEntityNamesOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		// MyNamingStrategyDelegator will use the table name (instead of JPA entity name) in generated join column.
		checkDefaultJoinColumnName( Owner.class, "elements", "OWNER_TABLE_id" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Boy has @Entity @Table(name="tbl_Boys")
		// MyNamingStrategyDelegator will use the table name (instead of JPA entity name) in generated join column.
		checkDefaultJoinColumnName( Boy.class, "hatedNames", "tbl_Boys_id" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Boy has @Entity @Table(name="tbl_Boys")
		// MyNamingStrategyDelegator will use the table name (instead of JPA entity name) in generated join column.
		checkDefaultCollectionTableName( Boy.class, "hatedNames", "tbl_Boys_hatedNames" );
	}

	static class MyLegacyNamingStrategyDelegator extends LegacyNamingStrategyDelegator {
		private final NamingStrategyDelegate hbmNamingStrategyDelegate = new LegacyHbmNamingStrategyDelegate( this );
		private final NamingStrategyDelegate nonHbmNamingStrategyDelegate = new MyNonHbmNamingStrategyDelegator( this );

		@Override
		public NamingStrategyDelegate getNamingStrategyDelegate(boolean isHbm) {
			return isHbm ? hbmNamingStrategyDelegate : nonHbmNamingStrategyDelegate;
		}

		@Override
		public NamingStrategy getNamingStrategy() {
			return EJB3NamingStrategy.INSTANCE;
		}

		private class MyNonHbmNamingStrategyDelegator extends AbstractLegacyNamingStrategyDelegate {
			MyNonHbmNamingStrategyDelegator(LegacyNamingStrategyDelegate.LegacyNamingStrategyDelegateContext context)  {
				super( context );
			}

			@Override
			public String toPhysicalTableName(String tableName) {
				return getNamingStrategy().tableName( tableName );
			}

			@Override
			public String toPhysicalColumnName(String columnName) {
				return getNamingStrategy().columnName( columnName );
			}

			@Override
			public String determineElementCollectionTableLogicalName(
					String ownerEntityName,
					String ownerJpaEntityName,
					String ownerEntityTable,
					String propertyNamePath) {
				return getNamingStrategy().collectionTableName(
						ownerEntityName,
						ownerEntityTable,
						null,
						null,
						propertyNamePath
				);
			}

			@Override
			public String determineElementCollectionForeignKeyColumnName(
					String propertyName,
					String propertyEntityName,
					String propertyJpaEntityName,
					String propertyTableName,
					String referencedColumnName) {
				return getNamingStrategy().foreignKeyColumnName(
						propertyName,
						propertyEntityName,
						propertyTableName,
						referencedColumnName
				);
			}

			@Override
			public String determineEntityAssociationJoinTableLogicalName(
					String ownerEntityName,
					String ownerJpaEntityName,
					String ownerEntityTable,
					String associatedEntityName,
					String associatedJpaEntityName,
					String associatedEntityTable,
					String propertyNamePath) {
				return getNamingStrategy().collectionTableName(
						ownerEntityName,
						ownerEntityTable,
						associatedEntityName,
						associatedEntityTable,
						propertyNamePath
				);
			}

			@Override
			public String determineEntityAssociationForeignKeyColumnName(
					String propertyName,
					String propertyEntityName,
					String propertyJpaEntityName,
					String propertyTableName,
					String referencedColumnName) {
				return getNamingStrategy().foreignKeyColumnName(
						propertyName,
						propertyEntityName,
						propertyTableName,
						referencedColumnName
				);
			}

			@Override
			public String logicalElementCollectionTableName(
					String tableName,
					String ownerEntityName,
					String ownerJpaEntityName,
					String ownerEntityTable,
					String propertyName) {
				return getNamingStrategy().logicalCollectionTableName(
						tableName,
						ownerEntityTable,
						null,
						propertyName
				);
			}

			@Override
			public String logicalEntityAssociationJoinTableName(
					String tableName,
					String ownerEntityName,
					String ownerJpaEntityName,
					String ownerEntityTable,
					String associatedEntityName,
					String associatedJpaEntityName,
					String associatedEntityTable,
					String propertyName) {
				return getNamingStrategy().logicalCollectionTableName(
						tableName,
						ownerEntityTable,
						associatedEntityTable,
						propertyName
				);
			}
		}
	}

}
