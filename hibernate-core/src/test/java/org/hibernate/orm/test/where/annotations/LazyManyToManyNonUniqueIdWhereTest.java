/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		LazyManyToManyNonUniqueIdWhereTest.Material.class,
		LazyManyToManyNonUniqueIdWhereTest.Building.class,
		LazyManyToManyNonUniqueIdWhereTest.Rating.class,
		LazyManyToManyNonUniqueIdWhereTest.Size.class
})
@SessionFactory
public class LazyManyToManyNonUniqueIdWhereTest {
	@BeforeAll
	public void createSchema(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> session.doWork( connection -> {
			final Dialect dialect = session.getDialect();
			try ( final Statement statement = connection.createStatement() ) {
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
				statement.executeUpdate("""
						create table MATERIAL_RATINGS(
							MATERIAL_ID integer not null,
							RATING_ID integer not null,
							primary key (MATERIAL_ID, RATING_ID)
						)""" );
				statement.executeUpdate( """
						create table BUILDING_RATINGS(
							BUILDING_ID integer not null,
							RATING_ID integer not null,
							primary key (BUILDING_ID, RATING_ID)
						)""" );

				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'plastic', 'MATERIAL' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'house', 'BUILDING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'high', 'RATING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 2, 'medium', 'RATING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 3, 'low', 'RATING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'small', 'SIZE' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 2, 'medium', 'SIZE' )" );

				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'MATERIAL', 1, 'RATING' )" );
				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'MATERIAL', 2, 'RATING' )" );
				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'MATERIAL', 3, 'RATING' )" );
				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'MATERIAL', 2, 'SIZE' )" );
				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'BUILDING', 1, 'RATING' )" );
				statement.executeUpdate( "insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
										"VALUES( 1, 'BUILDING', 1, 'SIZE' )" );

				statement.executeUpdate( "insert into MATERIAL_RATINGS(MATERIAL_ID, RATING_ID) VALUES( 1, 1 )" );

				statement.executeUpdate( "insert into BUILDING_RATINGS(BUILDING_ID, RATING_ID) VALUES( 1, 1 )" );
				statement.executeUpdate( "insert into BUILDING_RATINGS(BUILDING_ID, RATING_ID) VALUES( 1, 2 )" );
				statement.executeUpdate( "insert into BUILDING_RATINGS(BUILDING_ID, RATING_ID) VALUES( 1, 3 )" );
			}
		} ) );
	}

	@AfterAll
	public void dropSchema(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-12875")
	public void testInitializeFromUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var material = session.find( Material.class, 1 );
			assertEquals( "plastic", material.getName() );

			// Material#ratings is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( material.getRatings() ) );
			assertEquals( 1, material.getRatings().size() );
			assertTrue( Hibernate.isInitialized( material.getRatings() ) );

			final Rating rating = material.getRatings().iterator().next();
			assertEquals( "high", rating.getName() );

			var building = session.find( Building.class, 1 );
			assertEquals( "house", building.getName() );

			// Building#ratings is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( building.getMediumOrHighRatings() ) );
			checkMediumOrHighRatings( building.getMediumOrHighRatings() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-12875")
	public void testInitializeFromNonUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var material = session.find( Material.class, 1 );
			assertEquals( "plastic", material.getName() );

			// Material#mediumOrHighRatingsFromCombined is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( material.getMediumOrHighRatingsFromCombined() ) );
			checkMediumOrHighRatings( material.getMediumOrHighRatingsFromCombined() );
			Rating highRating = null;
			for ( Rating rating : material.getMediumOrHighRatingsFromCombined() ) {
				if ( "high".equals( rating.getName() ) ) {
					highRating = rating;
				}
			}
			assertNotNull( highRating );

			// Material#sizesFromCombined is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( material.getSizesFromCombined() ) );
			assertEquals( 1, material.getSizesFromCombined().size() );
			assertTrue( Hibernate.isInitialized( material.getSizesFromCombined() ) );

			final Size size = material.getSizesFromCombined().iterator().next();
			assertEquals( "medium", size.getName() );

			var building = session.find( Building.class, 1 );

			// building.ratingsFromCombined is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( building.getRatingsFromCombined() ) );
			assertEquals( 1, building.getRatingsFromCombined().size() );
			assertTrue( Hibernate.isInitialized( building.getRatingsFromCombined() ) );
			assertSame( highRating, building.getRatingsFromCombined().iterator().next() );

			// Building#sizesFromCombined is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( building.getSizesFromCombined() ) );
			assertEquals( 1, building.getSizesFromCombined().size() );
			assertTrue( Hibernate.isInitialized( building.getSizesFromCombined() ) );
			assertEquals( "small", building.getSizesFromCombined().iterator().next().getName() );
		} );
	}

	private void checkMediumOrHighRatings(List<Rating> mediumOrHighRatings) {
		assertEquals( 2, mediumOrHighRatings.size() );

		final Iterator<Rating> iterator = mediumOrHighRatings.iterator();
		final Rating firstRating = iterator.next();
		final Rating secondRating = iterator.next();
		if ( "high".equals( firstRating.getName() ) ) {
			assertEquals( "medium", secondRating.getName() );
		}
		else if ( "medium".equals( firstRating.getName() ) ) {
			assertEquals( "high", secondRating.getName() );
		}
		else {
			fail( "unexpected rating" );
		}
	}

	@Entity( name = "Material" )
	@Table( name = "MAIN_TABLE" )
	@SQLRestriction( "CODE = 'MATERIAL'" )
	public static class Material {
		private int id;

		private String name;
		private Set<Size> sizesFromCombined = new HashSet<>();
		private List<Rating> mediumOrHighRatingsFromCombined = new ArrayList<>();
		private Set<Rating> ratings = new HashSet<>();

		@Id
		@Column( name = "ID" )
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}

		@Column( name = "NAME")
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		@ManyToMany
		@JoinTable(
				name = "ASSOCIATION_TABLE",
				joinColumns = { @JoinColumn( name = "MAIN_ID" ) },
				inverseJoinColumns = { @JoinColumn( name = "ASSOCIATION_ID" ) }
		)
		@SQLJoinTableRestriction("MAIN_CODE='MATERIAL' AND ASSOCIATION_CODE='SIZE'")
		@Immutable
		public Set<Size> getSizesFromCombined() {
			return sizesFromCombined;
		}
		public void setSizesFromCombined(Set<Size> sizesFromCombined) {
			this.sizesFromCombined = sizesFromCombined;
		}

		@ManyToMany
		@JoinTable(
				name = "ASSOCIATION_TABLE",
				joinColumns = { @JoinColumn( name = "MAIN_ID" ) },
				inverseJoinColumns = { @JoinColumn( name = "ASSOCIATION_ID" ) }
		)
		@SQLJoinTableRestriction( "MAIN_CODE='MATERIAL' AND ASSOCIATION_CODE='RATING'" )
		@SQLRestriction( "NAME = 'high' or NAME = 'medium'" )
		@Immutable
		public List<Rating> getMediumOrHighRatingsFromCombined() {
			return mediumOrHighRatingsFromCombined;
		}
		public void setMediumOrHighRatingsFromCombined(List<Rating> mediumOrHighRatingsFromCombined) {
			this.mediumOrHighRatingsFromCombined = mediumOrHighRatingsFromCombined;
		}

		@ManyToMany
		@JoinTable(
				name = "MATERIAL_RATINGS",
				joinColumns = { @JoinColumn( name = "MATERIAL_ID") },
				inverseJoinColumns = { @JoinColumn( name = "RATING_ID" ) }
		)
		@Immutable
		public Set<Rating> getRatings() {
			return ratings;
		}
		public void setRatings(Set<Rating> ratings) {
			this.ratings = ratings;
		}
	}

	@Entity( name = "Building" )
	@Table( name = "MAIN_TABLE" )
	@SQLRestriction( "CODE = 'BUILDING'" )
	public static class Building {
		private int id;
		private String name;
		private Set<Size> sizesFromCombined = new HashSet<>();
		private Set<Rating> ratingsFromCombined = new HashSet<>();
		private List<Rating> mediumOrHighRatings = new ArrayList<>();

		@Id
		@Column( name = "ID" )
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}

		@Column( name = "NAME")
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		@ManyToMany
		@JoinTable(
				name = "ASSOCIATION_TABLE",
				joinColumns = { @JoinColumn( name = "MAIN_ID" ) },
				inverseJoinColumns = { @JoinColumn( name = "ASSOCIATION_ID" ) }
		)
		@SQLJoinTableRestriction("MAIN_CODE='BUILDING' AND ASSOCIATION_CODE='SIZE'")
		@Immutable
		public Set<Size> getSizesFromCombined() {
			return sizesFromCombined;
		}
		public void setSizesFromCombined(Set<Size> sizesFromCombined) {
			this.sizesFromCombined = sizesFromCombined;
		}

		@ManyToMany
		@JoinTable(
				name = "ASSOCIATION_TABLE",
				joinColumns = { @JoinColumn( name = "MAIN_ID" ) },
				inverseJoinColumns = { @JoinColumn( name = "ASSOCIATION_ID" ) }
		)
		@SQLJoinTableRestriction("MAIN_CODE='BUILDING' AND ASSOCIATION_CODE='RATING'" )
		@Immutable
		public Set<Rating> getRatingsFromCombined() {
			return ratingsFromCombined;
		}
		public void setRatingsFromCombined(Set<Rating> ratingsFromCombined) {
			this.ratingsFromCombined = ratingsFromCombined;
		}

		@ManyToMany
		@JoinTable(
				name = "BUILDING_RATINGS",
				joinColumns = { @JoinColumn( name = "BUILDING_ID") },
				inverseJoinColumns = { @JoinColumn( name = "RATING_ID" ) }
		)
		@SQLRestriction( "NAME = 'high' or NAME = 'medium'" )
		@Immutable
		public List<Rating> getMediumOrHighRatings() {
			return mediumOrHighRatings;
		}
		public void setMediumOrHighRatings(List<Rating> mediumOrHighRatings) {
			this.mediumOrHighRatings = mediumOrHighRatings;
		}
	}

	@Entity( name = "Size" )
	@Table( name = "MAIN_TABLE" )
	@SQLRestriction( "CODE = 'SIZE'" )
	public static class Size {
		private int id;
		private String name;

		@Id
		@Column( name = "ID" )
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}

		@Column( name = "NAME")
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "Rating" )
	@Table( name = "MAIN_TABLE" )
	@SQLRestriction( "CODE = 'RATING'" )
	public static class Rating {
		private int id;
		private String name;

		@Id
		@Column( name = "ID" )
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}

		@Column( name = "NAME")
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
}
