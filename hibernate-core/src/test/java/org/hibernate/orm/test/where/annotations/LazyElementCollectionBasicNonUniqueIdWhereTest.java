/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = {
		LazyElementCollectionBasicNonUniqueIdWhereTest.Material.class,
		LazyElementCollectionBasicNonUniqueIdWhereTest.Building.class
})
@SessionFactory(exportSchema = false)
public class LazyElementCollectionBasicNonUniqueIdWhereTest {

	@BeforeAll
	public void createSchema(SessionFactoryScope factoryScope) {
		applySchema( factoryScope );
	}

	public static void applySchema(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(  session -> session.doWork( connection -> {
			final Dialect dialect = session.getDialect();
			try ( final Statement statement = connection.createStatement() ) {
				statement.executeUpdate( dialect.getDropTableString( "MAIN_TABLE" ) );
				statement.executeUpdate( dialect.getDropTableString( "COLLECTION_TABLE" ) );
				statement.executeUpdate( dialect.getDropTableString( "MATERIAL_RATINGS" ) );

				// MAIN_TABLE
				statement.executeUpdate("""
						create table MAIN_TABLE(
							ID integer not null,
							NAME varchar(255) not null,
							CODE varchar(10) not null,
							primary key (ID, CODE)
						)
						""" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'plastic', 'MATERIAL' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'house', 'BUILDING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'high', 'RATING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 2, 'medium', 'RATING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 3, 'low', 'RATING' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'small', 'SIZE' )" );
				statement.executeUpdate( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 2, 'medium', 'SIZE' )" );

				// COLLECTION_TABLE
				statement.executeUpdate( """
						create table COLLECTION_TABLE(
							MAIN_ID integer not null,
							MAIN_CODE varchar(10) not null,
							VAL varchar(10) not null,
							VALUE_CODE varchar(10) not null,
							primary key (MAIN_ID, MAIN_CODE, VAL, VALUE_CODE)
						)
						""" );
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
						"VALUES( 1, 'MATERIAL', 'high', 'RATING' )"
				);
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
						"VALUES( 1, 'MATERIAL', 'medium', 'RATING' )"
				);
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
						"VALUES( 1, 'MATERIAL', 'low', 'RATING' )"
				);
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
						"VALUES( 1, 'MATERIAL', 'medium', 'SIZE' )"
				);
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
						"VALUES( 1, 'BUILDING', 'high', 'RATING' )"
				);
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, VAL, VALUE_CODE) " +
						"VALUES( 1, 'BUILDING', 'small', 'SIZE' )"
				);

				// MATERIAL_RATINGS
				statement.executeUpdate( """
						create table MATERIAL_RATINGS(
							MATERIAL_ID integer not null,
							RATING varchar(10) not null,
							primary key (MATERIAL_ID, RATING)
						)
						""" );
				statement.executeUpdate( "insert into MATERIAL_RATINGS(MATERIAL_ID, RATING) VALUES( 1, 'high' )" );
			}
		} ) );
	}

	@AfterAll
	public void dropSchema(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-12937")
	public void testInitializeFromUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) ->  {
			var material = session.find( Material.class, 1 );
			assertEquals( "plastic", material.getName() );

			// Material#ratings is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( material.getRatings() ) );
			assertEquals( 1, material.getRatings().size() );
			assertTrue( Hibernate.isInitialized( material.getRatings() ) );

			assertEquals( "high", material.getRatings().iterator().next() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-12937")
	public void testInitializeFromNonUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Material material = session.find( Material.class, 1 );
			assertEquals( "plastic", material.getName() );

			// Material#sizesFromCombined is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( material.getSizesFromCombined() ) );
			assertEquals( 1, material.getSizesFromCombined().size() );
			assertTrue( Hibernate.isInitialized( material.getSizesFromCombined() ) );

			assertEquals( "medium", material.getSizesFromCombined().iterator().next() );

			Building building = session.find( Building.class, 1 );

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
		} );
	}

	@Entity( name = "Material" )
	@Table( name = "MAIN_TABLE" )
	@SQLRestriction("CODE = 'MATERIAL'" )
	public static class Material {
		private int id;

		private String name;
		private Set<String> sizesFromCombined = new HashSet<>();
		private List<String> mediumOrHighRatingsFromCombined = new ArrayList<>();
		private Set<String> ratings = new HashSet<>();

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

		@ElementCollection
		@CollectionTable(
				name = "COLLECTION_TABLE",
				joinColumns = { @JoinColumn( name = "MAIN_ID" ) }
		)
		@Column( name="VAL")
		@SQLRestriction("MAIN_CODE='MATERIAL' AND VALUE_CODE='SIZE'")
		@Immutable
		public Set<String> getSizesFromCombined() {
			return sizesFromCombined;
		}
		public void setSizesFromCombined(Set<String> sizesFromCombined) {
			this.sizesFromCombined = sizesFromCombined;
		}

		@ElementCollection
		@CollectionTable(
				name = "MATERIAL_RATINGS",
				joinColumns = { @JoinColumn( name = "MATERIAL_ID" ) }
		)
		@Column( name="RATING")
		@Immutable
		public Set<String> getRatings() {
			return ratings;
		}
		public void setRatings(Set<String> ratings) {
			this.ratings = ratings;
		}
	}

	@Entity( name = "Building" )
	@Table( name = "MAIN_TABLE" )
	@SQLRestriction("CODE = 'BUILDING'" )
	public static class Building {
		private int id;
		private String name;
		private Set<String> sizesFromCombined = new HashSet<>();
		private Set<String> ratingsFromCombined = new HashSet<>();
		private List<String> mediumOrHighRatings = new ArrayList<>();

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

		@ElementCollection
		@CollectionTable(
				name = "COLLECTION_TABLE",
				joinColumns = { @JoinColumn( name = "MAIN_ID" ) }
		)
		@Column( name="VAL")
		@SQLRestriction("MAIN_CODE='BUILDING' AND VALUE_CODE='SIZE'")
		@Immutable
		public Set<String> getSizesFromCombined() {
			return sizesFromCombined;
		}
		public void setSizesFromCombined(Set<String> sizesFromCombined) {
			this.sizesFromCombined = sizesFromCombined;
		}

		@ElementCollection
		@CollectionTable(
				name = "COLLECTION_TABLE",
				joinColumns = { @JoinColumn( name = "MAIN_ID" ) }
		)
		@Column( name="VAL")
		@SQLRestriction( "MAIN_CODE='BUILDING' AND VALUE_CODE='RATING'" )
		@Immutable
		public Set<String> getRatingsFromCombined() {
			return ratingsFromCombined;
		}
		public void setRatingsFromCombined(Set<String> ratingsFromCombined) {
			this.ratingsFromCombined = ratingsFromCombined;
		}
	}
}
