/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitCollectionTableNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Gail Badner
 */
public class CustomImprovedNamingCollectionElementTest extends ImprovedNamingCollectionElementTest {

	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		super.configureMetadataBuilder( metadataBuilder );
		metadataBuilder.applyImplicitNamingStrategy( new MyImprovedNamingStrategy() );
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

	static class MyImprovedNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {
		@Override
		public Identifier determineCollectionTableName(ImplicitCollectionTableNameSource source) {
			// This impl uses the owner entity table name instead of the JPA entity name when
			// generating the implicit name.
			final String name = source.getOwningPhysicalTableName().getText()
					+ '_'
					+ transformAttributePath( source.getOwningAttributePath() );

			return toIdentifier( name, source.getBuildingContext() );
		}

		@Override
		public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
			final String name = source.getReferencedTableName() + "_" + source.getReferencedColumnName();
			return toIdentifier( name, source.getBuildingContext() );
		}
	}
}
