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
import org.hibernate.cfg.naming.HbmNamingStrategyDelegate;
import org.hibernate.cfg.naming.ImprovedNamingStrategyDelegator;
import org.hibernate.cfg.naming.JpaNamingStrategyDelegate;
import org.hibernate.testing.TestForIssue;

/**
 * @author Gail Badner
 */
public class CustomImprovedNamingCollectionElementTest extends ImprovedNamingCollectionElementTest {

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategyDelegator( new MyImprovedNamingStrategyDelegator() );
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

	static class MyImprovedNamingStrategyDelegator extends ImprovedNamingStrategyDelegator {
		public MyImprovedNamingStrategyDelegator() {
			super( new HbmNamingStrategyDelegate(), new MyNonHbmNamingStrategyDelegate() );
		}

		private static class MyNonHbmNamingStrategyDelegate extends JpaNamingStrategyDelegate {
			@Override
			public String determineImplicitElementCollectionTableName(
					String ownerEntityName,
					String ownerJpaEntityName,
					String ownerEntityTable,
					String propertyPath) {
				// This impl uses the owner entity table name instead of the JPA entity name when
				// generating the implicit name.
				int loc = propertyPath.lastIndexOf(".");
				final String unqualifiedPropertyName = loc < 0 ? propertyPath : propertyPath.substring( loc + 1 );
				return ownerEntityTable
						+ '_'
						+ unqualifiedPropertyName;
			}

			@Override
			public String determineImplicitElementCollectionJoinColumnName(
					String ownerEntityName,
					String ownerJpaEntityName,
					String ownerEntityTable,
					String referencedColumnName,
					String propertyPath) {
				return ownerEntityTable
						+ '_'
						+ referencedColumnName;
			}
		}
	}
}
