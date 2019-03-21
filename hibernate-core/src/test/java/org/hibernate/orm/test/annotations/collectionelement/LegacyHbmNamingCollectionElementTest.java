/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Filter;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.IMPLICIT_NAMING_STRATEGY;
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
@SuppressWarnings("unchecked")
public class LegacyHbmNamingCollectionElementTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	@FailureExpected(value = "hql query set parameter, issue with type inference")
	public void testSimpleElement() {
		assertEquals(
				"BoyFavoriteNumbers",
				getMetadata().getCollectionBinding( Boy.class.getName() + '.' + "favoriteNumbers" )
						.getMappedTable().getName()
		);
		inSession(
				session -> {
					Boy boy = new Boy();

					try {
						session.getTransaction().begin();

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
						boy.getCharacters().add( Character.GENTLE );
						boy.getCharacters().add( Character.CRAFTY );

						HashMap<String, FavoriteFood> foods = new HashMap<String, FavoriteFood>();
						foods.put( "breakfast", FavoriteFood.PIZZA );
						foods.put( "lunch", FavoriteFood.KUNGPAOCHICKEN );
						foods.put( "dinner", FavoriteFood.SUSHI );
						boy.setFavoriteFood( foods );
						session.persist( boy );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					session.clear();

					try {
						session.getTransaction().begin();
						boy = session.get( Boy.class, boy.getId() );
						assertNotNull( boy.getNickNames() );
						assertTrue( boy.getNickNames().contains( "Thing" ) );
						assertNotNull( boy.getScorePerNickName() );
						assertTrue( boy.getScorePerNickName().containsKey( "Thing" ) );
						assertEquals( Integer.valueOf( 5 ), boy.getScorePerNickName().get( "Thing" ) );
						assertNotNull( boy.getFavoriteNumbers() );
						assertEquals( 3, boy.getFavoriteNumbers()[1] );
						assertTrue( boy.getCharacters().contains( Character.CRAFTY ) );
						assertTrue( boy.getFavoriteFood().get( "dinner" ).equals( FavoriteFood.SUSHI ) );
						assertTrue( boy.getFavoriteFood().get( "lunch" ).equals( FavoriteFood.KUNGPAOCHICKEN ) );
						assertTrue( boy.getFavoriteFood().get( "breakfast" ).equals( FavoriteFood.PIZZA ) );
						List result = session.createQuery(
								"select boy from Boy boy join boy.nickNames names where names = :name" )
								.setParameter( "name", "Thing" ).list();
						assertEquals( 1, result.size() );
						session.delete( boy );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@FailureExpected("@Parent not yet implemented")
	@Test
	public void testCompositeElement() {
		inSession(
				session -> {
					Boy boy = new Boy();
					Toy toy = new Toy();
					try {
						session.getTransaction().begin();
						boy.setFirstName( "John" );
						boy.setLastName( "Doe" );
						toy.setName( "Balloon" );
						toy.setSerial( "serial001" );
						toy.setBrand( new Brand() );
						toy.getBrand().setName( "Bandai" );
						boy.getFavoriteToys().add( toy );
						session.persist( boy );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					session.clear();

					try {
						session.getTransaction().begin();
						boy = session.get( Boy.class, boy.getId() );
						assertNotNull( boy );
						assertNotNull( boy.getFavoriteToys() );
						assertTrue( boy.getFavoriteToys().contains( toy ) );
						Boy owner = boy.getFavoriteToys().iterator().next().getOwner();
						assertEquals( boy, owner, "@Parent is failing" );
						session.delete( boy );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	public void testAttributedJoin() {
		inSession(
				session -> {
					Country country = new Country();
					Boy boy = new Boy();
					CountryAttitude attitude = new CountryAttitude();
					try {
						session.getTransaction().begin();
						country.setName( "Australia" );
						session.persist( country );

						boy.setFirstName( "John" );
						boy.setLastName( "Doe" );
						// TODO: doesn't work
						attitude.setBoy( boy );
						attitude.setCountry( country );
						attitude.setLikes( true );
						boy.getCountryAttitudes().add( attitude );
						session.persist( boy );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					session.clear();

					try {
						session.getTransaction().begin();
						boy = session.get( Boy.class, boy.getId() );
						assertTrue( boy.getCountryAttitudes().contains( attitude ) );
						session.delete( boy );
						session.delete( session.get( Country.class, country.getId() ) );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	@FailureExpected(value = "hql query set parameter, issue with type inference")
	public void testLazyCollectionofElements() {
		assertEquals(
				"BoyFavoriteNumbers",
				getMetadata().getCollectionBinding( Boy.class.getName() + '.' + "favoriteNumbers" )
						.getCollectionTable().getName()
		);
		inSession(
				session -> {
					session.getTransaction().begin();
					Boy boy = new Boy();
					try {
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
						boy.getCharacters().add( Character.GENTLE );
						boy.getCharacters().add( Character.CRAFTY );
						session.persist( boy );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					session.clear();

					try {
						session.getTransaction().begin();
						boy = session.get( Boy.class, boy.getId() );
						assertNotNull( boy.getNickNames() );
						assertTrue( boy.getNickNames().contains( "Thing" ) );
						assertNotNull( boy.getScorePerNickName() );
						assertTrue( boy.getScorePerNickName().containsKey( "Thing" ) );
						assertEquals( new Integer( 5 ), boy.getScorePerNickName().get( "Thing" ) );
						assertNotNull( boy.getFavoriteNumbers() );
						assertEquals( 3, boy.getFavoriteNumbers()[1] );
						assertTrue( boy.getCharacters().contains( Character.CRAFTY ) );

						List result = session.createQuery(
								"select boy from Boy boy join boy.nickNames names where names = :name" )
								.setParameter( "name", "Thing" ).list();
						assertEquals( 1, result.size() );
						session.delete( boy );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);

	}

	@Test
	@FailureExpected("Filters not yet implemented")
	public void testFetchEagerAndFilter() {
		inTransaction(
				session -> {
					TestCourse test = new TestCourse();

					LocalizedString title = new LocalizedString( "title in english" );
					title.getVariations().put( Locale.FRENCH.getLanguage(), "title en francais" );
					test.setTitle( title );
					session.save( test );

					session.flush();
					session.clear();

					Filter filter = session.enableFilter( "selectedLocale" );
					filter.setParameter( "param", "fr" );

					Query q = session.createQuery( "from TestCourse t" );
					List l = q.list();
					assertEquals( 1, l.size() );

					TestCourse t = session.get( TestCourse.class, test.getTestCourseId() );
					assertEquals( 1, t.getTitle().getVariations().size() );
				}
		);
	}

	@Test
	public void testMapKeyType() {
		Matrix m = new Matrix();
		inTransaction(
				session -> {
					m.getMvalues().put( 1, 1.1f );
					session.persist( m );
					session.flush();
					session.clear();
					Matrix matrix = session.get( Matrix.class, m.getId() );
					assertEquals( 1.1f, matrix.getMvalues().get( 1 ), 0.01f );
				}
		);

		inTransaction(
				session -> {
					Matrix matrix = session.get( Matrix.class, m.getId() );
					session.delete( matrix );
				}
		);
	}

	@Test
	public void testDefaultValueColumnForBasic() {
		isDefaultValueCollectionColumnPresent( Boy.class.getName(), "hatedNames" );
		isDefaultValueCollectionColumnPresent( Boy.class.getName(), "preferredNames" );
		isCollectionColumnPresent( Boy.class.getName(), "nickNames", "nickNames" );
		isDefaultValueCollectionColumnPresent( Boy.class.getName(), "scorePerPreferredName" );
	}

	private void isDefaultValueCollectionColumnPresent(String collectionOwner, String propertyName) {
		isCollectionColumnPresent( collectionOwner, propertyName, propertyName );
	}

	private void isCollectionColumnPresent(String collectionOwner, String propertyName, String columnName) {
		final Collection collection = getMetadata().getCollectionBinding( collectionOwner + "." + propertyName );
		boolean hasDefault = false;
		Set<Column> mappedColumns = collection.getMappedTable().getMappedColumns();
		for ( Column column : mappedColumns ) {
			if ( columnName.equals( column.getName().getText() ) ) {
				hasDefault = true;
			}
		}
		assertTrue( hasDefault, "Could not find " + columnName );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9387")
	public void testDefaultTableNameNoOverrides() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Products has @Entity (no @Table)
		checkDefaultCollectionTableName( BugSystem.class, "bugs", "BugSystem_bugs" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Boy has @Entity @Table(name="tbl_Boys")
		checkDefaultCollectionTableName( Boy.class, "hatedNames", "Boy_hatedNames" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerEntityNameAndPKColumnOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated collection table.
		checkDefaultCollectionTableName( Matrix.class, "mvalues", "Matrix_mvalues" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableAndEntityNamesOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated join column.
		checkDefaultJoinColumnName( Owner.class, "elements", "Owner_id" );
	}

	protected void checkDefaultCollectionTableName(
			Class<?> ownerEntityClass,
			String ownerCollectionPropertyName,
			String expectedCollectionTableName) {
		final org.hibernate.mapping.Collection collection = getMetadata().getCollectionBinding(
				ownerEntityClass.getName() + '.' + ownerCollectionPropertyName
		);
		final org.hibernate.mapping.Table table = (Table) collection.getMappedTable();
		assertEquals( expectedCollectionTableName, table.getName() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9389")
	public void testDefaultJoinColumnNoOverrides() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Products has @Entity (no @Table)
		checkDefaultJoinColumnName( BugSystem.class, "bugs", "BugSystem_id" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Boy has @Entity @Table(name="tbl_Boys")
		checkDefaultJoinColumnName( Boy.class, "hatedNames", "Boy_id" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerEntityNameAndPKColumnOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated join column.
		checkDefaultJoinColumnName( Matrix.class, "mvalues", "Matrix_mId" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableAndEntityNamesOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		// Legacy behavior used unqualified entity name (instead of JPA entity name) in generated collection table.
		checkDefaultCollectionTableName( Owner.class, "elements", "Owner_elements" );
	}

	protected void checkDefaultJoinColumnName(
			Class<?> ownerEntityClass,
			String ownerCollectionPropertyName,
			String ownerForeignKeyNameExpected) {
		final org.hibernate.mapping.Collection ownerCollection = getMetadata().getCollectionBinding(
				ownerEntityClass.getName() + '.' + ownerCollectionPropertyName
		);
		// The default owner join column can only be computed if it has a PK with 1 column.
		assertEquals( 1, ownerCollection.getOwner().getKey().getColumnSpan() );
		MappedColumn column = (MappedColumn) ownerCollection.getKey().getMappedColumns().get( 0 );
		assertEquals( ownerForeignKeyNameExpected, column.getText() );

		boolean hasOwnerFK = false;
		MappedTable mappedTable = ownerCollection.getMappedTable();
		java.util.Collection<MappedForeignKey> foreignKeys = mappedTable.getForeignKeys();
		for ( MappedForeignKey fk : foreignKeys ) {
			assertSame( ownerCollection.getMappedTable(), fk.getMappedTable() );
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

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		builer.applySetting( IMPLICIT_NAMING_STRATEGY, "legacy-hbm" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Boy.class,
				Country.class,
				TestCourse.class,
				Matrix.class,
				Owner.class,
				BugSystem.class
		};
	}
}
