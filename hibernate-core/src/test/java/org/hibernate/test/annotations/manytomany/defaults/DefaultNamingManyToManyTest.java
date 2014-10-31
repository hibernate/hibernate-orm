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
package org.hibernate.test.annotations.manytomany.defaults;

import java.util.Iterator;

import org.junit.Test;

import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.EntityType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests names generated for @JoinTable and @JoinColumn for unidirectional and bidirectional
 * many-to-many associations when the "default" {@link org.hibernate.cfg.naming.NamingStrategyDelegator}
 * is used. The current default does not comply with the JPA spec in some cases. See HHH-9390
 * for more information.
 *
 * NOTE: expected primary table names and join columns are explicit here to ensure that
 * entity names/tables and PK columns are not changed (which would invalidate these test cases).
 *
 * @author Gail Badner
 */
public class DefaultNamingManyToManyTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testBidirNoOverrides() {
		// Employee.contactInfo.phoneNumbers: associated entity: PhoneNumber
		// both have @Entity with no name configured and default primary table names;
		// Primary table names default to unqualified entity classes.
		// PK column for Employee.id: id (default)
		// PK column for PhoneNumber.phNumber: phNumber (default)
		// bidirectional association
		checkDefaultJoinTablAndJoinColumnNames(
				Employee.class,
				"contactInfo.phoneNumbers",
				"employees",
				"Employee_PhoneNumber",
				"employees_id",
				"phoneNumbers_phNumber"
		);
	}

	@Test
	public void testBidirOwnerPKOverride() {
		// Store.customers; associated entity: KnownClient
		// both have @Entity with no name configured and default primary table names
		// Primary table names default to unqualified entity classes.
		// PK column for Store.id: sId
		// PK column for KnownClient.id: id (default)
		// bidirectional association
		checkDefaultJoinTablAndJoinColumnNames(
				Store.class,
				"customers",
				"stores",
				"Store_KnownClient",
				"stores_sId",
				"customers_id"
		);
	}

	@Test
	public void testUnidirOwnerPKAssocEntityNamePKOverride() {
		// Store.items; associated entity: Item
		// Store has @Entity with no name configured and no @Table
		// Item has @Entity(name="ITEM") and no @Table
		// PK column for Store.id: sId
		// PK column for Item: iId
		// unidirectional
		checkDefaultJoinTablAndJoinColumnNames(
				Store.class,
				"items",
				null,
				"Store_ITEM",
				"Store_sId",
				"items_iId"

		);
	}

	@Test
	public void testUnidirOwnerPKAssocPrimaryTableNameOverride() {
		// Store.implantedIn; associated entity: City
		// Store has @Entity with no name configured and no @Table
		// City has @Entity with no name configured and @Table(name = "tbl_city")
		// PK column for Store.id: sId
		// PK column for City.id: id (default)
		// unidirectional
		checkDefaultJoinTablAndJoinColumnNames(
				Store.class,
				"implantedIn",
				null,
				"Store_tbl_city",
				"Store_sId",
				"implantedIn_id"
		);
	}

	@Test
	public void testUnidirOwnerPKAssocEntityNamePrimaryTableOverride() {
		// Store.categories; associated entity: Category
		// Store has @Entity with no name configured and no @Table
		// Category has @Entity(name="CATEGORY") @Table(name="CATEGORY_TAB")
		// PK column for Store.id: sId
		// PK column for Category.id: id (default)
		// unidirectional
		checkDefaultJoinTablAndJoinColumnNames(
				Store.class,
				"categories",
				null,
				"Store_CATEGORY_TAB",
				"Store_sId",
				"categories_id"
		);
	}

	@Test
	public void testUnidirOwnerEntityNamePKAssocPrimaryTableOverride() {
		// Item.producedInCities: associated entity: City
		// Item has @Entity(name="ITEM") and no @Table
		// City has @Entity with no name configured and @Table(name = "tbl_city")
		// PK column for Item: iId
		// PK column for City.id: id (default)
		// unidirectional
		checkDefaultJoinTablAndJoinColumnNames(
				Item.class,
				"producedInCities",
				null,
				"ITEM_tbl_city",
				"ITEM_iId",
				"producedInCities_id"
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9390")
	public void testUnidirOwnerEntityNamePrimaryTableOverride() {
		// Category.clients: associated entity: KnownClient
		// Category has @Entity(name="CATEGORY") @Table(name="CATEGORY_TAB")
		// KnownClient has @Entity with no name configured and no @Table
		// PK column for Category.id: id (default)
		// PK column for KnownClient.id: id (default)
		// unidirectional
		// legacy behavior would use the table name in the generated join column.
		checkDefaultJoinTablAndJoinColumnNames(
				Category.class,
				"clients",
				null,
				"CATEGORY_TAB_KnownClient",
				"CATEGORY_TAB_id",
				"clients_id"

		);
	}

	protected void checkDefaultJoinTablAndJoinColumnNames(
			Class<?> ownerEntityClass,
			String ownerCollectionPropertyName,
			String inverseCollectionPropertyName,
			String expectedCollectionTableName,
			String ownerForeignKeyNameExpected,
			String inverseForeignKeyNameExpected) {
		final org.hibernate.mapping.Collection collection = configuration().getCollectionMapping( ownerEntityClass.getName() + '.' + ownerCollectionPropertyName );
		final org.hibernate.mapping.Table table = collection.getCollectionTable();
		assertEquals( expectedCollectionTableName, table.getName() );

		final org.hibernate.mapping.Collection ownerCollection = configuration().getCollectionMapping(
				ownerEntityClass.getName() + '.' + ownerCollectionPropertyName
		);
		// The default owner and inverse join columns can only be computed if they have PK with 1 column.
		assertEquals ( 1, ownerCollection.getOwner().getKey().getColumnSpan() );
		assertEquals( ownerForeignKeyNameExpected, ownerCollection.getKey().getColumnIterator().next().getText() );

		final EntityType associatedEntityType =  (EntityType) ownerCollection.getElement().getType();
		final PersistentClass associatedPersistentClass =
				configuration().getClassMapping( associatedEntityType.getAssociatedEntityName() );
		assertEquals( 1, associatedPersistentClass.getKey().getColumnSpan() );
		if ( inverseCollectionPropertyName != null ) {
			final org.hibernate.mapping.Collection inverseCollection = configuration().getCollectionMapping(
					associatedPersistentClass.getEntityName() + '.' + inverseCollectionPropertyName
			);
			assertEquals(
					inverseForeignKeyNameExpected,
					inverseCollection.getKey().getColumnIterator().next().getText()
			);
		}
		boolean hasOwnerFK = false;
		boolean hasInverseFK = false;
		for ( Iterator it=ownerCollection.getCollectionTable().getForeignKeyIterator(); it.hasNext(); ) {
			final ForeignKey fk = (ForeignKey) it.next();
			assertSame( ownerCollection.getCollectionTable(), fk.getTable() );
			if ( fk.getColumnSpan() > 1 ) {
				continue;
			}
			if ( fk.getColumn( 0 ).getText().equals( ownerForeignKeyNameExpected ) ) {
				assertSame( ownerCollection.getOwner().getTable(), fk.getReferencedTable() );
				hasOwnerFK = true;
			}
			else  if ( fk.getColumn( 0 ).getText().equals( inverseForeignKeyNameExpected ) ) {
				assertSame( associatedPersistentClass.getTable(), fk.getReferencedTable() );
				hasInverseFK = true;
			}
		}
		assertTrue( hasOwnerFK );
		assertTrue( hasInverseFK );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Category.class,
				City.class,
				Employee.class,
				Item.class,
				KnownClient.class,
				PhoneNumber.class,
				Store.class,
		};
	}
}
