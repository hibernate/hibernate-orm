/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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
		LazyOneToManyNonUniqueIdWhereTest.Material.class,
		LazyOneToManyNonUniqueIdWhereTest.Building.class,
		LazyOneToManyNonUniqueIdWhereTest.Rating.class,
		LazyOneToManyNonUniqueIdWhereTest.Size.class
})
@SessionFactory
public class LazyOneToManyNonUniqueIdWhereTest {
	@BeforeEach
	void createSchema(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> session.doWork(  connection -> {
			var dialect = session.getDialect();
			try ( Statement statement = connection.createStatement() ) {
				statement.executeUpdate( dialect.getDropTableString( "MAIN_TABLE" ) );
				statement.executeUpdate( """
						create table MAIN_TABLE(
							ID integer not null,
							NAME varchar(255) not null,
							CODE varchar(10) not null,
							MATERIAL_OWNER_ID integer,
							BUILDING_OWNER_ID integer,
							primary key (ID, CODE)
						)""" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'plastic', 'MATERIAL' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'house', 'BUILDING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE, MATERIAL_OWNER_ID, BUILDING_OWNER_ID) " +
										"VALUES( 1, 'high', 'RATING', 1, 1 )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE, MATERIAL_OWNER_ID, BUILDING_OWNER_ID) " +
										"VALUES( 2, 'medium', 'RATING', 1, null )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE, MATERIAL_OWNER_ID, BUILDING_OWNER_ID) " +
										"VALUES( 3, 'low', 'RATING', 1, null )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE, MATERIAL_OWNER_ID, BUILDING_OWNER_ID) " +
										"VALUES( 1, 'small', 'SIZE', null, 1 )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE, MATERIAL_OWNER_ID, BUILDING_OWNER_ID) " +
										"VALUES( 2, 'medium', 'SIZE', 1, null )" );
			}
		} ) );
	}

	@AfterAll
	public void dropSchema(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-12875")
	public void testInitializeFromNonUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Material material = session.find( Material.class, 1 );
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

			Building building = session.get( Building.class, 1 );

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

		@OneToMany
		@JoinColumn( name = "MATERIAL_OWNER_ID" )
		@Immutable
		public Set<Size> getSizesFromCombined() {
			return sizesFromCombined;
		}
		public void setSizesFromCombined(Set<Size> sizesFromCombined) {
			this.sizesFromCombined = sizesFromCombined;
		}

		@OneToMany
		@JoinColumn( name = "MATERIAL_OWNER_ID")
		@SQLRestriction( "NAME = 'high' or NAME = 'medium'" )
		@Immutable
		public List<Rating> getMediumOrHighRatingsFromCombined() {
			return mediumOrHighRatingsFromCombined;
		}
		public void setMediumOrHighRatingsFromCombined(List<Rating> mediumOrHighRatingsFromCombined) {
			this.mediumOrHighRatingsFromCombined = mediumOrHighRatingsFromCombined;
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

		@OneToMany
		@JoinColumn( name = "BUILDING_OWNER_ID" )
		@Immutable
		public Set<Size> getSizesFromCombined() {
			return sizesFromCombined;
		}
		public void setSizesFromCombined(Set<Size> sizesFromCombined) {
			this.sizesFromCombined = sizesFromCombined;
		}

		@OneToMany
		@JoinColumn( name = "BUILDING_OWNER_ID" )
		@Immutable
		public Set<Rating> getRatingsFromCombined() {
			return ratingsFromCombined;
		}
		public void setRatingsFromCombined(Set<Rating> ratingsFromCombined) {
			this.ratingsFromCombined = ratingsFromCombined;
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
