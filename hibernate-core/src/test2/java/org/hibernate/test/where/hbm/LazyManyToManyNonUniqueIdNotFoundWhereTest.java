/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.where.hbm;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class LazyManyToManyNonUniqueIdNotFoundWhereTest extends BaseCoreFunctionalTestCase {

	protected String[] getMappings() {
		return new String[] { "where/hbm/LazyManyToManyNonUniqueIdNotFoundWhereTest.hbm.xml" };
	}

	@Before
	public void setup() {
		doInHibernate(
				this::sessionFactory, session -> {

					session.createSQLQuery( "drop table MATERIAL_RATINGS" ).executeUpdate();
					session.createSQLQuery( "drop table BUILDING_RATINGS" ).executeUpdate();
					session.createSQLQuery( "drop table ASSOCIATION_TABLE" ).executeUpdate();
					session.createSQLQuery( "drop table MAIN_TABLE" ).executeUpdate();

					session.createSQLQuery(
							"create table MAIN_TABLE( " +
									"ID integer not null, NAME varchar(255) not null, CODE varchar(10) not null, " +
									"primary key (ID, CODE) )"
					).executeUpdate();

					session.createSQLQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'plastic', 'MATERIAL' )" )
							.executeUpdate();
					session.createSQLQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'house', 'BUILDING' )" )
							.executeUpdate();
					session.createSQLQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'high', 'RATING' )" )
							.executeUpdate();
					session.createSQLQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'small', 'SIZE' )" )
							.executeUpdate();

					session.createSQLQuery(
							"create table ASSOCIATION_TABLE( " +
									"MAIN_ID integer not null, MAIN_CODE varchar(10) not null, " +
									"ASSOCIATION_ID int not null, ASSOCIATION_CODE varchar(10) not null, " +
									"primary key (MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE))"
					).executeUpdate();

					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 1, 'RATING' )"
					).executeUpdate();

					// add a collection element that won't be found
					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 2, 'RATING' )"
					).executeUpdate();

					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 1, 'SIZE' )"
					).executeUpdate();

					// add a collection element that won't be found
					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 2, 'SIZE' )"
					).executeUpdate();

					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'BUILDING', 1, 'RATING' )"
					).executeUpdate();

					// add a collection element that won't be found
					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'BUILDING', 2, 'RATING' )"
					).executeUpdate();


					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'BUILDING', 1, 'SIZE' )"
					).executeUpdate();

					// add a collection element that won't be found
					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'BUILDING', 2, 'SIZE' )"
					).executeUpdate();

					session.createSQLQuery(
							"create table MATERIAL_RATINGS( " +
									"MATERIAL_ID integer not null, RATING_ID integer not null," +
									" primary key (MATERIAL_ID, RATING_ID))"
					).executeUpdate();

					session.createSQLQuery(
							"insert into MATERIAL_RATINGS(MATERIAL_ID, RATING_ID) VALUES( 1, 1 )"
					).executeUpdate();

					// add a collection element that won't be found
					session.createSQLQuery(
							"insert into MATERIAL_RATINGS(MATERIAL_ID, RATING_ID) VALUES( 1, 2 )"
					).executeUpdate();


					session.createSQLQuery(
							"create table BUILDING_RATINGS( " +
									"BUILDING_ID integer not null, RATING_ID integer not null," +
									" primary key (BUILDING_ID, RATING_ID))"
					).executeUpdate();

					session.createSQLQuery(
							"insert into BUILDING_RATINGS(BUILDING_ID, RATING_ID) VALUES( 1, 1 )"
					).executeUpdate();

					// add a collection element that won't be found
					session.createSQLQuery(
							"insert into BUILDING_RATINGS(BUILDING_ID, RATING_ID) VALUES( 1, 2 )"
					).executeUpdate();
				}
		);
	}

	@After
	public void cleanup() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.createSQLQuery( "delete from MATERIAL_RATINGS" ).executeUpdate();
					session.createSQLQuery( "delete from BUILDING_RATINGS" ).executeUpdate();
					session.createSQLQuery( "delete from ASSOCIATION_TABLE" ).executeUpdate();
					session.createSQLQuery( "delete from MAIN_TABLE" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12875")
	public void testInitializeFromUniqueAssociationTable() {
		doInHibernate(
				this::sessionFactory, session -> {

					Material material = session.get( Material.class, 1 );
					assertEquals( "plastic", material.getName() );

					// Material#ratings is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( material.getRatings() ) );
					assertEquals( 1, material.getRatings().size() );
					assertTrue( Hibernate.isInitialized( material.getRatings() ) );

					final Rating rating = material.getRatings().iterator().next();
					assertEquals( "high", rating.getName() );

					Building building = session.get( Building.class, 1 );
					assertEquals( "house", building.getName() );

					// Building#ratings is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( building.getRatings() ) );
					assertEquals( 1, building.getRatings().size() );
					assertTrue( Hibernate.isInitialized( building.getRatings() ) );
					assertSame( rating, building.getRatings().iterator().next() );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12875")
	public void testInitializeFromNonUniqueAssociationTable() {
		doInHibernate(
				this::sessionFactory, session -> {

					Material material = session.get( Material.class, 1 );
					assertEquals( "plastic", material.getName() );

					// Material#ratingsFromCombined is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( material.getRatingsFromCombined() ) );
					assertEquals( 1, material.getRatingsFromCombined().size() );
					assertTrue( Hibernate.isInitialized( material.getRatingsFromCombined() ) );

					final Rating rating = material.getRatingsFromCombined().iterator().next();
					assertEquals( "high", rating.getName() );

					// Material#sizesFromCombined is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( material.getSizesFromCombined() ) );
					assertEquals( 1, material.getSizesFromCombined().size() );
					assertTrue( Hibernate.isInitialized( material.getSizesFromCombined() ) );

					final Size size = material.getSizesFromCombined().iterator().next();
					assertEquals( "small", size.getName() );

					Building building = session.get( Building.class, 1 );

					// building.ratingsFromCombined is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( building.getRatingsFromCombined() ) );
					assertEquals( 1, building.getRatingsFromCombined().size() );
					assertTrue( Hibernate.isInitialized( building.getRatingsFromCombined() ) );
					assertSame( rating, building.getRatingsFromCombined().iterator().next() );

					// Building#sizesFromCombined is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( building.getSizesFromCombined() ) );
					assertEquals( 1, building.getSizesFromCombined().size() );
					assertTrue( Hibernate.isInitialized( building.getSizesFromCombined() ) );
					assertSame( size, building.getSizesFromCombined().iterator().next() );
				}
		);
	}

	public static class Material {
		private int id;

		private String name;
		private Set<Size> sizesFromCombined = new HashSet<>();
		private Set<Rating> ratingsFromCombined = new HashSet<>();
		private Set<Rating> ratings = new HashSet<>();

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Set<Size> getSizesFromCombined() {
			return sizesFromCombined;
		}
		public void setSizesFromCombined(Set<Size> sizesFromCombined) {
			this.sizesFromCombined = sizesFromCombined;
		}
		public Set<Rating> getRatingsFromCombined() {
			return ratingsFromCombined;
		}
		public void setRatingsFromCombined(Set<Rating> ratingsFromCombined) {
			this.ratingsFromCombined = ratingsFromCombined;
		}
		public Set<Rating> getRatings() {
			return ratings;
		}
		public void setRatings(Set<Rating> ratings) {
			this.ratings = ratings;
		}
	}

	public static class Building {
		private int id;
		private String name;
		private Set<Size> sizesFromCombined = new HashSet<>();
		private Set<Rating> ratingsFromCombined = new HashSet<>();
		private Set<Rating> ratings = new HashSet<>();

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Set<Size> getSizesFromCombined() {
			return sizesFromCombined;
		}
		public void setSizesFromCombined(Set<Size> sizesFromCombined) {
			this.sizesFromCombined = sizesFromCombined;
		}
		public Set<Rating> getRatingsFromCombined() {
			return ratingsFromCombined;
		}
		public void setRatingsFromCombined(Set<Rating> ratingsFromCombined) {
			this.ratingsFromCombined = ratingsFromCombined;
		}
		public Set<Rating> getRatings() {
			return ratings;
		}
		public void setRatings(Set<Rating> ratings) {
			this.ratings = ratings;
		}
	}

	public static class Size {
		private int id;
		private String name;

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}

	public static class Rating {
		private int id;
		private String name;

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
}
