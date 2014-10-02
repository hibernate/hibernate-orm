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
import org.hibernate.cfg.naming.LegacyNamingStrategyDelegator;
import org.hibernate.testing.TestForIssue;

/**
 * @author Gail Badner
 */
public class LegacyNamingCollectionElementTest extends CollectionElementTest {

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategyDelegator( new LegacyNamingStrategyDelegator( EJB3NamingStrategy.INSTANCE ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerEntityNameAndPKColumnOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated collection table.
		checkDefaultCollectionTableName( Matrix.class, "mvalues", "Matrix_mvalues" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableAndEntityNamesOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated collection table.
		checkDefaultCollectionTableName( Owner.class, "elements", "Owner_elements" );
	}


	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerEntityNameAndPKColumnOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated join column.
		checkDefaultJoinColumnName( Matrix.class, "mvalues", "Matrix_mId" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableAndEntityNamesOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated join column.
		checkDefaultJoinColumnName( Owner.class, "elements", "Owner_id" );
	}
}
