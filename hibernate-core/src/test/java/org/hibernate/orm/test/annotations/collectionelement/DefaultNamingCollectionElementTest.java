/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.hibernate.Filter;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.orm.test.annotations.Country;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests @ElementCollection using the default "legacy" NamingStrategyDelegator which does not
 * comply with JPA spec in some cases. See HHH-9387 and HHH-9389 for more information..
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				Boy.class,
				Country.class,
				TestCourse.class,
				Matrix.class,
				Owner.class,
				BugSystem.class
		}
)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "legacy-hbm")
})
public class DefaultNamingCollectionElementTest {

	@Test
	public void testSimpleElement(SessionFactoryScope scope) {
		assertEquals(
				"BoyFavoriteNumbers",
				scope.getMetadataImplementor().getCollectionBinding( Boy.class.getName() + '.' + "favoriteNumbers" )
						.getCollectionTable().getName()
		);

		scope.inTransaction(
				session -> {
					Boy boy = new Boy();
					boy.setFirstName( "John" );
					boy.setLastName( "Doe" );
					boy.getNickNames().add( "Johnny" );
					boy.getNickNames().add( "Thing" );
					boy.getScorePerNickName().put( "Johnny", 3 );
					boy.getScorePerNickName().put( "Thing", 5 );
					int[] favNbrs = new int[4];
					for ( int index = 0; index < favNbrs.length - 1; index++ ) {
						favNbrs[index] = index * 3;
					}
					boy.setFavoriteNumbers( favNbrs );
					boy.getCharacters().add( CharacterTrait.GENTLE );
					boy.getCharacters().add( CharacterTrait.CRAFTY );

					HashMap<String, FavoriteFood> foods = new HashMap<>();
					foods.put( "breakfast", FavoriteFood.PIZZA );
					foods.put( "lunch", FavoriteFood.KUNGPAOCHICKEN );
					foods.put( "dinner", FavoriteFood.SUSHI );
					boy.setFavoriteFood( foods );
					session.persist( boy );

					session.getTransaction().commit();

					session.clear();

					session.beginTransaction();
					boy = session.get( Boy.class, boy.getId() );
					assertNotNull( boy.getNickNames() );
					assertTrue( boy.getNickNames().contains( "Thing" ) );
					assertNotNull( boy.getScorePerNickName() );
					assertTrue( boy.getScorePerNickName().containsKey( "Thing" ) );
					assertEquals( Integer.valueOf( 5 ), boy.getScorePerNickName().get( "Thing" ) );
					assertNotNull( boy.getFavoriteNumbers() );
					assertEquals( 3, boy.getFavoriteNumbers()[1] );
					assertTrue( boy.getCharacters().contains( CharacterTrait.CRAFTY ) );
					assertEquals( FavoriteFood.SUSHI, boy.getFavoriteFood().get( "dinner" ) );
					assertEquals( FavoriteFood.KUNGPAOCHICKEN, boy.getFavoriteFood().get( "lunch" ) );
					assertEquals( FavoriteFood.PIZZA, boy.getFavoriteFood().get( "breakfast" ) );
					var result = session.createQuery(
							"select boy from Boy boy join boy.nickNames names where names = :name" )
							.setParameter( "name", "Thing" ).list();
					assertEquals( 1, result.size() );
					session.remove( boy );
				}
		);
	}

	@Test
	public void testCompositeElement(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Boy boy = new Boy();
					boy.setFirstName( "John" );
					boy.setLastName( "Doe" );
					Toy toy = new Toy();
					toy.setName( "Balloon" );
					toy.setSerial( "serial001" );
					toy.setBrand( new Brand() );
					toy.getBrand().setName( "Bandai" );
					boy.getFavoriteToys().add( toy );
					session.persist( boy );
					session.getTransaction().commit();

					session.clear();

					session.beginTransaction();
					boy = session.get( Boy.class, boy.getId() );
					assertNotNull( boy );
					assertNotNull( boy.getFavoriteToys() );
					assertTrue( boy.getFavoriteToys().contains( toy ) );
					Toy next = boy.getFavoriteToys().iterator().next();
					assertEquals( boy, next.getOwner(), "@Parent is failing" );
					session.remove( boy );
				}
		);
	}

	@Test
	public void testAttributedJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Country country = new Country();
					country.setName( "Australia" );
					session.persist( country );

					Boy boy = new Boy();
					boy.setFirstName( "John" );
					boy.setLastName( "Doe" );
					CountryAttitude attitude = new CountryAttitude();
					// TODO: doesn't work
					attitude.setBoy( boy );
					attitude.setCountry( country );
					attitude.setLikes( true );
					boy.getCountryAttitudes().add( attitude );
					session.persist( boy );
					session.getTransaction().commit();

					session.clear();

					session.beginTransaction();
					boy = session.get( Boy.class, boy.getId() );
					assertTrue( boy.getCountryAttitudes().contains( attitude ) );
					session.remove( boy );
					session.remove( session.get( Country.class, country.getId() ) );
				}
		);
	}

	@Test
	public void testLazyCollectionofElements(SessionFactoryScope scope) {
		assertEquals(
				"BoyFavoriteNumbers",
				scope.getMetadataImplementor().getCollectionBinding( Boy.class.getName() + '.' + "favoriteNumbers" )
						.getCollectionTable().getName()
		);

		scope.inTransaction(
				session -> {
					Boy boy = new Boy();
					boy.setFirstName( "John" );
					boy.setLastName( "Doe" );
					boy.getNickNames().add( "Johnny" );
					boy.getNickNames().add( "Thing" );
					boy.getScorePerNickName().put( "Johnny", 3 );
					boy.getScorePerNickName().put( "Thing", 5 );
					int[] favNbrs = new int[4];
					for ( int index = 0; index < favNbrs.length - 1; index++ ) {
						favNbrs[index] = index * 3;
					}
					boy.setFavoriteNumbers( favNbrs );
					boy.getCharacters().add( CharacterTrait.GENTLE );
					boy.getCharacters().add( CharacterTrait.CRAFTY );
					session.persist( boy );
					session.getTransaction().commit();

					session.clear();

					session.beginTransaction();
					boy = session.get( Boy.class, boy.getId() );
					assertNotNull( boy.getNickNames() );
					assertTrue( boy.getNickNames().contains( "Thing" ) );
					assertNotNull( boy.getScorePerNickName() );
					assertTrue( boy.getScorePerNickName().containsKey( "Thing" ) );
					assertEquals( Integer.valueOf( 5 ), boy.getScorePerNickName().get( "Thing" ) );
					assertNotNull( boy.getFavoriteNumbers() );
					assertEquals( 3, boy.getFavoriteNumbers()[1] );
					assertTrue( boy.getCharacters().contains( CharacterTrait.CRAFTY ) );
					var result = session.createQuery(
							"select boy from Boy boy join boy.nickNames names where names = :name" )
							.setParameter( "name", "Thing" ).list();
					assertEquals( 1, result.size() );
					session.remove( boy );
				}
		);
	}

	@Test
	public void testFetchEagerAndFilter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestCourse test = new TestCourse();

					LocalizedString title = new LocalizedString( "title in english" );
					title.getVariations().put( Locale.FRENCH.getLanguage(), "title en francais" );
					test.setTitle( title );
					session.persist( test );

					session.flush();
					session.clear();

					Filter filter = session.enableFilter( "selectedLocale" );
					filter.setParameter( "param", "fr" );

					assertEquals( 1,
							session.createQuery( "from TestCourse t" ).list().size() );

					TestCourse t = session.get( TestCourse.class, test.getTestCourseId() );
					assertEquals( 1, t.getTitle().getVariations().size() );
				}
		);
	}

	@Test
	public void testMapKeyType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Matrix m = new Matrix();
					m.getMvalues().put( 1, 1.1f );
					session.persist( m );
					session.flush();
					session.clear();
					m = session.get( Matrix.class, m.getId() );
					assertEquals( 1.1f, m.getMvalues().get( 1 ), 0.01f );
				}
		);
	}

	@Test
	public void testDefaultValueColumnForBasic(SessionFactoryScope scope) {
		final MetadataImplementor metadataImplementor = scope.getMetadataImplementor();
		isDefaultValueCollectionColumnPresent( metadataImplementor, Boy.class.getName(), "hatedNames" );
		isDefaultValueCollectionColumnPresent( metadataImplementor, Boy.class.getName(), "preferredNames" );
		isCollectionColumnPresent( metadataImplementor, Boy.class.getName(), "nickNames", "nickNames" );
		isDefaultValueCollectionColumnPresent( metadataImplementor, Boy.class.getName(), "scorePerPreferredName" );
	}

	private void isDefaultValueCollectionColumnPresent(
			MetadataImplementor metadataImplementor,
			String collectionOwner,
			String propertyName) {
		isCollectionColumnPresent( metadataImplementor, collectionOwner, propertyName, propertyName );
	}

	private void isCollectionColumnPresent(
			MetadataImplementor metadataImplementor,
			String collectionOwner,
			String propertyName,
			String columnName) {
		final Collection collection = metadataImplementor.getCollectionBinding( collectionOwner + "." + propertyName );
		final Iterator<Column> columnIterator = collection.getCollectionTable().getColumns().iterator();
		boolean hasDefault = false;
		while ( columnIterator.hasNext() ) {
			Column column = columnIterator.next();
			if ( columnName.equals( column.getName() ) ) {
				hasDefault = true;
			}
		}
		assertTrue( hasDefault, "Could not find " + columnName );
	}

	@Test
	@JiraKey(value = "HHH-9387")
	public void testDefaultTableNameNoOverrides(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Products has @Entity (no @Table)
		checkDefaultCollectionTableName( scope.getMetadataImplementor(), BugSystem.class, "bugs", "BugSystem_bugs" );
	}

	@Test
	@JiraKey(value = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableOverride(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Boy has @Entity @Table(name="tbl_Boys")
		checkDefaultCollectionTableName( scope.getMetadataImplementor(), Boy.class, "hatedNames", "Boy_hatedNames" );
	}

	@Test
	@JiraKey(value = "HHH-9387")
	public void testDefaultTableNameOwnerEntityNameAndPKColumnOverride(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated collection table.
		checkDefaultCollectionTableName( scope.getMetadataImplementor(), Matrix.class, "mvalues", "Matrix_mvalues" );
	}

	@Test
	@JiraKey(value = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableAndEntityNamesOverride(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated join column.
		checkDefaultJoinColumnName( scope.getMetadataImplementor(), Owner.class, "elements", "Owner_id" );
	}

	protected void checkDefaultCollectionTableName(
			MetadataImplementor metadataImplementor,
			Class<?> ownerEntityClass,
			String ownerCollectionPropertyName,
			String expectedCollectionTableName) {
		final Collection collection = metadataImplementor.getCollectionBinding(
				ownerEntityClass.getName() + '.' + ownerCollectionPropertyName
		);
		final org.hibernate.mapping.Table table = collection.getCollectionTable();
		assertEquals( expectedCollectionTableName, table.getName() );
	}

	@Test
	@JiraKey(value = "HHH-9389")
	public void testDefaultJoinColumnNoOverrides(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Products has @Entity (no @Table)
		checkDefaultJoinColumnName( scope.getMetadataImplementor(), BugSystem.class, "bugs", "BugSystem_id" );
	}

	@Test
	@JiraKey(value = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableOverride(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Boy has @Entity @Table(name="tbl_Boys")
		checkDefaultJoinColumnName( scope.getMetadataImplementor(), Boy.class, "hatedNames", "Boy_id" );
	}

	@Test
	@JiraKey(value = "HHH-9389")
	public void testDefaultJoinColumnOwnerEntityNameAndPKColumnOverride(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated join column.
		checkDefaultJoinColumnName( scope.getMetadataImplementor(), Matrix.class, "mvalues", "Matrix_mId" );
	}

	@Test
	@JiraKey(value = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableAndEntityNamesOverride(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated collection table.
		checkDefaultCollectionTableName( scope.getMetadataImplementor(), Owner.class, "elements", "Owner_elements" );
	}

	protected void checkDefaultJoinColumnName(
			MetadataImplementor metadataImplementor,
			Class<?> ownerEntityClass,
			String ownerCollectionPropertyName,
			String ownerForeignKeyNameExpected) {
		final Collection ownerCollection = metadataImplementor.getCollectionBinding(
				ownerEntityClass.getName() + '.' + ownerCollectionPropertyName
		);
		// The default owner join column can only be computed if it has a PK with 1 column.
		assertEquals( 1, ownerCollection.getOwner().getKey().getColumnSpan() );
		assertEquals( ownerForeignKeyNameExpected, ownerCollection.getKey().getSelectables().get( 0 ).getText() );

		boolean hasOwnerFK = false;
		for ( final ForeignKey fk : ownerCollection.getCollectionTable().getForeignKeyCollection() ) {
			assertSame( ownerCollection.getCollectionTable(), fk.getTable() );
			if ( fk.getColumnSpan() > 1 ) {
				continue;
			}
			if ( fk.getColumn( 0 ).getText().equals( ownerForeignKeyNameExpected ) ) {
				assertSame( ownerCollection.getOwner().getTable(), fk.getReferencedTable() );
				hasOwnerFK = true;
			}
		}
		assertTrue( hasOwnerFK );
	}
}
