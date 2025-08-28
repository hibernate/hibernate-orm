/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.hbm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@RequiresDialect(H2Dialect.class)
public class LazyElementCollectionBasicNonUniqueIdWhereTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "where/hbm/LazyElementCollectionBasicNonUniqueIdWhereTest.hbm.xml" };
	}

	@Before
	public void setup() {
		Session session = openSession();
		session.beginTransaction();
		{
					session.createNativeQuery( getDialect().getDropTableString( "MAIN_TABLE" ) ).executeUpdate();
					session.createNativeQuery( getDialect().getDropTableString( "COLLECTION_TABLE" ) ).executeUpdate();
					session.createNativeQuery( getDialect().getDropTableString( "MATERIAL_RATINGS" ) ).executeUpdate();

					session.createNativeQuery(
							"create table MAIN_TABLE( " +
									"ID integer not null, NAME varchar(255) not null, CODE varchar(10) not null, " +
									"primary key (ID, CODE) )"
					).executeUpdate();

					session.createNativeQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'plastic', 'MATERIAL' )" )
							.executeUpdate();
					session.createNativeQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'house', 'BUILDING' )" )
							.executeUpdate();
					session.createNativeQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'high', 'RATING' )" )
							.executeUpdate();
					session.createNativeQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 2, 'medium', 'RATING' )" )
							.executeUpdate();
					session.createNativeQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 3, 'low', 'RATING' )" )
							.executeUpdate();
					session.createNativeQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'small', 'SIZE' )" )
							.executeUpdate();
					session.createNativeQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 2, 'medium', 'SIZE' )" )
							.executeUpdate();

					session.createNativeQuery(
							"create table COLLECTION_TABLE( " +
									"MAIN_ID integer not null, MAIN_CODE varchar(10) not null, " +
									"VAL varchar(10) not null, VALUE_CODE varchar(10) not null, " +
									"primary key (MAIN_ID, MAIN_CODE, VAL, VALUE_CODE))"
					).executeUpdate();

					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
									"VALUES( 1, 'MATERIAL', 'high', 'RATING' )"
					).executeUpdate();
					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
									"VALUES( 1, 'MATERIAL', 'medium', 'RATING' )"
					).executeUpdate();
					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
									"VALUES( 1, 'MATERIAL', 'low', 'RATING' )"
					).executeUpdate();

					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
									"VALUES( 1, 'MATERIAL', 'medium', 'SIZE' )"
					).executeUpdate();

					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
									"VALUES( 1, 'BUILDING', 'high', 'RATING' )"
					).executeUpdate();

					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
									"VALUES( 1, 'BUILDING', 'small', 'SIZE' )"
					).executeUpdate();


					session.createNativeQuery(
							"create table MATERIAL_RATINGS( " +
									"MATERIAL_ID integer not null, RATING varchar(10) not null," +
									" primary key (MATERIAL_ID, RATING))"
					).executeUpdate();

					session.createNativeQuery(
							"insert into MATERIAL_RATINGS(MATERIAL_ID, RATING) VALUES( 1, 'high' )"
					).executeUpdate();

		}
		session.getTransaction().commit();
		session.close();
	}

	@After
	public void cleanup() {
		Session session = openSession();
		session.beginTransaction();
		{
					session.createNativeQuery( "delete from MATERIAL_RATINGS" ).executeUpdate();
//					session.createSQLQuery( "delete from BUILDING_RATINGS" ).executeUpdate();
					session.createNativeQuery( "delete from COLLECTION_TABLE" ).executeUpdate();
					session.createNativeQuery( "delete from MAIN_TABLE" ).executeUpdate();
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@JiraKey( value = "HHH-12937")
	public void testInitializeFromUniqueAssociationTable() {
		Session session = openSession();
		session.beginTransaction();
		{
					Material material = session.get( Material.class, 1 );
					assertEquals( "plastic", material.getName() );

					// Material#ratings is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( material.getRatings() ) );
					assertEquals( 1, material.getRatings().size() );
					assertTrue( Hibernate.isInitialized( material.getRatings() ) );

					assertEquals( "high", material.getRatings().iterator().next() );
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@JiraKey( value = "HHH-12937")
	public void testInitializeFromNonUniqueAssociationTable() {
		Session session = openSession();
		session.beginTransaction();
		{
					Material material = session.get( Material.class, 1 );
					assertEquals( "plastic", material.getName() );

					// Material#sizesFromCombined is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( material.getSizesFromCombined() ) );
					assertEquals( 1, material.getSizesFromCombined().size() );
					assertTrue( Hibernate.isInitialized( material.getSizesFromCombined() ) );

					assertEquals( "medium", material.getSizesFromCombined().iterator().next() );

					Building building = session.get( Building.class, 1 );

					// building.ratingsFromCombined is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( building.getRatingsFromCombined() ) );
					assertEquals( 1, building.getRatingsFromCombined().size() );
					assertTrue( Hibernate.isInitialized( building.getRatingsFromCombined() ) );
					assertEquals( "high", building.getRatingsFromCombined().iterator().next() );

					// Building#sizesFromCombined is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( building.getSizesFromCombined() ) );
					assertEquals( 1, building.getSizesFromCombined().size() );
					assertTrue( Hibernate.isInitialized( building.getSizesFromCombined() ) );
					assertEquals( "small", building.getSizesFromCombined().iterator().next() );
		}
		session.getTransaction().commit();
		session.close();
	}

	public static class Material {
		private int id;

		private String name;
		private Set<String> sizesFromCombined = new HashSet<>();
		private List<String> mediumOrHighRatingsFromCombined = new ArrayList<>();
		private Set<String> ratings = new HashSet<>();

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

		public Set<String> getSizesFromCombined() {
			return sizesFromCombined;
		}
		public void setSizesFromCombined(Set<String> sizesFromCombined) {
			this.sizesFromCombined = sizesFromCombined;
		}

		public Set<String> getRatings() {
			return ratings;
		}
		public void setRatings(Set<String> ratings) {
			this.ratings = ratings;
		}
	}

	public static class Building {
		private int id;
		private String name;
		private Set<String> sizesFromCombined = new HashSet<>();
		private Set<String> ratingsFromCombined = new HashSet<>();
		private List<String> mediumOrHighRatings = new ArrayList<>();

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

		public Set<String> getSizesFromCombined() {
			return sizesFromCombined;
		}
		public void setSizesFromCombined(Set<String> sizesFromCombined) {
			this.sizesFromCombined = sizesFromCombined;
		}

		public Set<String> getRatingsFromCombined() {
			return ratingsFromCombined;
		}
		public void setRatingsFromCombined(Set<String> ratingsFromCombined) {
			this.ratingsFromCombined = ratingsFromCombined;
		}
	}
}
