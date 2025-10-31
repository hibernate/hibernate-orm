/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany.defaults;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.type.EntityType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests names generated for {@code @JoinTable} and {@code @JoinColumn} for unidirectional
 * and bidirectional many-to-many associations using the "legacy JPA" naming strategy, which
 * does not comply with JPA spec in all cases.  See HHH-9390 for more information.
 * <p>
 * Expected primary table names and join columns are explicit here to ensure that entity
 * names/tables and PK columns are not changed (which would invalidate these test cases).
 *
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				Category.class,
				City.class,
				Employee.class,
				Item.class,
				KnownClient.class,
				PhoneNumber.class,
				Store.class,
		}
)
@ServiceRegistry(
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
						provider = ManyToManyImplicitNamingTest.ImplicitNamingStrategyProvider.class
				)
		}
)
@SessionFactory
public class ManyToManyImplicitNamingTest {

	public static class ImplicitNamingStrategyProvider
			implements SettingProvider.Provider<ImplicitNamingStrategy> {
		@Override
		public ImplicitNamingStrategy getSetting() {
			return ImplicitNamingStrategyLegacyJpaImpl.INSTANCE;
		}
	}

	@Test
	public void testBidirNoOverrides(SessionFactoryScope scope) {
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
				"phoneNumbers_phNumber",
				scope
		);
	}

	@Test
	public void testBidirOwnerPKOverride(SessionFactoryScope scope) {
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
				"customers_id",
				scope
		);
	}

	@Test
	public void testUnidirOwnerPKAssocEntityNamePKOverride(SessionFactoryScope scope) {
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
				"items_iId",
				scope
		);
	}

	@Test
	public void testUnidirOwnerPKAssocPrimaryTableNameOverride(SessionFactoryScope scope) {
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
				"implantedIn_id",
				scope
		);
	}

	@Test
	public void testUnidirOwnerPKAssocEntityNamePrimaryTableOverride(SessionFactoryScope scope) {
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
				"categories_id",
				scope
		);
	}

	@Test
	public void testUnidirOwnerEntityNamePKAssocPrimaryTableOverride(SessionFactoryScope scope) {
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
				"producedInCities_id",
				scope
		);
	}

	@Test
	@JiraKey(value = "HHH-9390")
	public void testUnidirOwnerEntityNamePrimaryTableOverride(SessionFactoryScope scope) {
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
				"clients_id",
				scope
		);
	}

	protected void checkDefaultJoinTablAndJoinColumnNames(
			Class<?> ownerEntityClass,
			String ownerCollectionPropertyName,
			String inverseCollectionPropertyName,
			String expectedCollectionTableName,
			String ownerForeignKeyNameExpected,
			String inverseForeignKeyNameExpected,
			SessionFactoryScope scope) {
		MetadataImplementor metadata = scope.getMetadataImplementor();
		final org.hibernate.mapping.Collection collection = metadata.getCollectionBinding(
				ownerEntityClass.getName() + '.' + ownerCollectionPropertyName );
		final org.hibernate.mapping.Table table = collection.getCollectionTable();
		assertThat( table.getName() ).isEqualTo( expectedCollectionTableName );

		final org.hibernate.mapping.Collection ownerCollection = metadata.getCollectionBinding(
				ownerEntityClass.getName() + '.' + ownerCollectionPropertyName
		);
		// The default owner and inverse join columns can only be computed if they have PK with 1 column.
		assertThat( ownerCollection.getOwner().getKey().getColumnSpan() ).isEqualTo( 1 );
		assertThat( ownerCollection.getKey().getColumns().get( 0 ).getText() ).isEqualTo( ownerForeignKeyNameExpected );

		final EntityType associatedEntityType = (EntityType) ownerCollection.getElement().getType();
		final PersistentClass associatedPersistentClass =
				metadata.getEntityBinding( associatedEntityType.getAssociatedEntityName() );
		assertThat( associatedPersistentClass.getKey().getColumnSpan() ).isEqualTo( 1 );
		if ( inverseCollectionPropertyName != null ) {
			final org.hibernate.mapping.Collection inverseCollection = metadata.getCollectionBinding(
					associatedPersistentClass.getEntityName() + '.' + inverseCollectionPropertyName
			);
			assertThat( inverseCollection.getKey().getSelectables().get( 0 ).getText() )
					.isEqualTo( inverseForeignKeyNameExpected );
		}
		boolean hasOwnerFK = false;
		boolean hasInverseFK = false;
		for ( final ForeignKey fk : ownerCollection.getCollectionTable().getForeignKeyCollection() ) {
			assertThat( fk.getTable() ).isSameAs( ownerCollection.getCollectionTable() );
			if ( fk.getColumnSpan() > 1 ) {
				continue;
			}
			if ( fk.getColumn( 0 ).getText().equals( ownerForeignKeyNameExpected ) ) {
				assertThat( fk.getReferencedTable() ).isSameAs( ownerCollection.getOwner().getTable() );
				hasOwnerFK = true;
			}
			else if ( fk.getColumn( 0 ).getText().equals( inverseForeignKeyNameExpected ) ) {
				assertThat( fk.getReferencedTable() ).isSameAs( associatedPersistentClass.getTable() );
				hasInverseFK = true;
			}
		}
		assertThat( hasOwnerFK ).isTrue();
		assertThat( hasInverseFK ).isTrue();
	}

}
