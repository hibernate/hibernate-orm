/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * Tests @ElementCollection using the "improved" NamingStrategyDelegator which complies
 * with JPA spec.
 *
 * @author Gail Badner
 */
public class ImprovedNamingCollectionElementTest extends DefaultNamingCollectionElementTest {
	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		metadataBuilder.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerEntityNameAndPKColumnOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		checkDefaultCollectionTableName( Matrix.class, "mvalues", "Mtx_mvalues" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableAndEntityNamesOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		checkDefaultCollectionTableName( Owner.class, "elements", "OWNER_elements" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerEntityNameAndPKColumnOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		checkDefaultJoinColumnName( Matrix.class, "mvalues", "Mtx_mId" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableAndEntityNamesOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		checkDefaultJoinColumnName( Owner.class, "elements", "OWNER_id" );
	}
}
