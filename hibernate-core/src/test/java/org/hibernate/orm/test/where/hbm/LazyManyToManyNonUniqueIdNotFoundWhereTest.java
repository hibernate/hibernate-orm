/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.hbm;

import org.hibernate.Hibernate;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "hbm/where/LazyManyToManyNonUniqueIdNotFoundWhereTest.hbm.xml")
@SessionFactory
public class LazyManyToManyNonUniqueIdNotFoundWhereTest {
	@AfterAll
	static void dropSchema(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeAll
	public void createSchema(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> session.doWork( connection -> {
			final Dialect dialect = session.getDialect();
			try (final Statement statement = connection.createStatement()) {
				statement.executeUpdate( dialect.getDropTableString( "MATERIAL_RATINGS" ) );
				statement.executeUpdate( dialect.getDropTableString( "BUILDING_RATINGS" ) );
				statement.executeUpdate( dialect.getDropTableString( "ASSOCIATION_TABLE" ) );
				statement.executeUpdate( dialect.getDropTableString( "MAIN_TABLE" ) );

				statement.executeUpdate( """
						create table MAIN_TABLE(
							ID integer not null,
							NAME varchar(255) not null,
							CODE varchar(10) not null,
							primary key (ID, CODE)
						)""" );
				statement.executeUpdate( """
						create table ASSOCIATION_TABLE(
							MAIN_ID integer not null,
							MAIN_CODE varchar(10) not null,
							ASSOCIATION_ID int not null,
							ASSOCIATION_CODE varchar(10) not null,
							primary key (MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE)
						)""" );
				statement.executeUpdate( """
						create table BUILDING_RATINGS(
							BUILDING_ID integer not null,
							RATING_ID integer not null,
							primary key (BUILDING_ID, RATING_ID)
						)""" );
				statement.executeUpdate("""
						create table MATERIAL_RATINGS(
							MATERIAL_ID integer not null,
							RATING_ID integer not null,
							primary key (MATERIAL_ID, RATING_ID)
						)""" );

				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'plastic', 'MATERIAL' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'house', 'BUILDING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'high', 'RATING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'small', 'SIZE' )" );

				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'MATERIAL', 1, 'RATING' )" );
				// add a collection element that won't be found
				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'MATERIAL', 2, 'RATING' )" );

				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'MATERIAL', 1, 'SIZE' )" );

				// add a collection element that won't be found
				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'MATERIAL', 2, 'SIZE' )" );

				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'BUILDING', 1, 'RATING' )" );

				// add a collection element that won't be found
				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'BUILDING', 2, 'RATING' )" );

				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'BUILDING', 1, 'SIZE' )" );

				// add a collection element that won't be found
				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'BUILDING', 2, 'SIZE' )" );

				statement.executeUpdate( "insert into MATERIAL_RATINGS(MATERIAL_ID, RATING_ID) VALUES( 1, 1 )" );

				// add a collection element that won't be found
				statement.executeUpdate( "insert into MATERIAL_RATINGS(MATERIAL_ID, RATING_ID) VALUES( 1, 2 )" );

				statement.executeUpdate( "insert into BUILDING_RATINGS(BUILDING_ID, RATING_ID) VALUES( 1, 1 )" );

				// add a collection element that won't be found
				statement.executeUpdate( "insert into BUILDING_RATINGS(BUILDING_ID, RATING_ID) VALUES( 1, 2 )" );
			}
		} ) );
	}

	@Test
	@JiraKey( value = "HHH-12875" )
	public void testInitializeFromUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Material material = session.find( Material.class, 1 );
			assertEquals( "plastic", material.getName() );

			// Material#ratings is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( material.getRatings() ) );
			assertEquals( 1, material.getRatings().size() );
			assertTrue( Hibernate.isInitialized( material.getRatings() ) );

			final Rating rating = material.getRatings().iterator().next();
			assertEquals( "high", rating.getName() );

			Building building = session.find( Building.class, 1 );
			assertEquals( "house", building.getName() );

			// Building#ratings is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( building.getRatings() ) );
			assertEquals( 1, building.getRatings().size() );
			assertTrue( Hibernate.isInitialized( building.getRatings() ) );
			assertSame( rating, building.getRatings().iterator().next() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-12875" )
	public void testInitializeFromNonUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Material material = session.find( Material.class, 1 );
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

			Building building = session.find( Building.class, 1 );

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
		} );
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
