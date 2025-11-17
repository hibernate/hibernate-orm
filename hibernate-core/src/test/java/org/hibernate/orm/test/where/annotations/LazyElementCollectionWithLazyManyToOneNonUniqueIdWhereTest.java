/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = {
		LazyElementCollectionWithLazyManyToOneNonUniqueIdWhereTest.Material.class,
		LazyElementCollectionWithLazyManyToOneNonUniqueIdWhereTest.Building.class,
		LazyElementCollectionWithLazyManyToOneNonUniqueIdWhereTest.Rating.class,
		LazyElementCollectionWithLazyManyToOneNonUniqueIdWhereTest.Size.class
})
@SessionFactory(exportSchema = false)
public class LazyElementCollectionWithLazyManyToOneNonUniqueIdWhereTest {
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
							ASSOCIATION_ID int not null,
							ASSOCIATION_CODE varchar(10) not null,
							primary key (MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE)
						)""" );
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
						"VALUES( 1, 'MATERIAL', 1, 'RATING' )"
				);
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
						"VALUES( 1, 'MATERIAL', 2, 'RATING' )"
				);
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
						"VALUES( 1, 'MATERIAL', 3, 'RATING' )"
				);
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
						"VALUES( 1, 'MATERIAL', 2, 'SIZE' )"
				);
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
						"VALUES( 1, 'BUILDING', 1, 'RATING' )"
				);
				statement.executeUpdate(
						"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
						"VALUES( 1, 'BUILDING', 1, 'SIZE' )"
				);

				// MATERIAL_RATINGS
				statement.executeUpdate( """
						create table MATERIAL_RATINGS(
							MATERIAL_ID integer not null,
							RATING_ID integer not null,
							primary key (MATERIAL_ID, RATING_ID)
						)
						""" );
				statement.executeUpdate( "insert into MATERIAL_RATINGS(MATERIAL_ID, RATING_ID) VALUES( 1, 1 )" );
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
		factoryScope.inTransaction( (session) -> {
			var material = session.find( Material.class, 1 );
			assertEquals( "plastic", material.getName() );

			// Material#ratings is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( material.getContainedRatings() ) );
			assertEquals( 1, material.getContainedRatings().size() );
			assertTrue( Hibernate.isInitialized( material.getContainedRatings() ) );

			var containedRating = material.getContainedRatings().iterator().next();
			assertTrue( Hibernate.isInitialized( containedRating ) );
			assertEquals( "high", containedRating.getRating().getName() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-12937")
	public void testInitializeFromNonUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var material = session.find( Material.class, 1 );
			assertEquals( "plastic", material.getName() );

			// Material#containedSizesFromCombined is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( material.getContainedSizesFromCombined() ) );
			assertEquals( 1, material.getContainedSizesFromCombined().size() );
			assertTrue( Hibernate.isInitialized( material.getContainedSizesFromCombined() ) );

			var containedSize = material.getContainedSizesFromCombined().iterator().next();
			assertFalse( Hibernate.isInitialized( containedSize.getSize() ) );
			assertEquals( "medium", containedSize.getSize().getName() );

			var building = session.find( Building.class, 1 );

			// building.ratingsFromCombined is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( building.getContainedRatingsFromCombined() ) );
			assertEquals( 1, building.getContainedRatingsFromCombined().size() );
			assertTrue( Hibernate.isInitialized( building.getContainedRatingsFromCombined() ) );

			var containedRating = building.getContainedRatingsFromCombined().iterator().next();
			assertFalse( Hibernate.isInitialized( containedRating.getRating() ) );
			assertEquals( "high", containedRating.getRating().getName() );

			// Building#containedSizesFromCombined is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( building.getContainedSizesFromCombined() ) );
			assertEquals( 1, building.getContainedSizesFromCombined().size() );
			assertTrue( Hibernate.isInitialized( building.getContainedSizesFromCombined() ) );
			containedSize = building.getContainedSizesFromCombined().iterator().next();
			assertFalse( Hibernate.isInitialized( containedSize.getSize() ) );
			assertEquals( "small", containedSize.getSize().getName() );
		} );
	}

	@Entity( name = "Material" )
	@Table( name = "MAIN_TABLE" )
	@SQLRestriction( "CODE = 'MATERIAL'" )
	public static class Material {
		private int id;

		private String name;
		private Set<ContainedSize> containedSizesFromCombined = new HashSet<>();
		private List<Rating> mediumOrHighRatingsFromCombined = new ArrayList<>();
		private Set<ContainedRating> containedRatings = new HashSet<>();

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
		@AssociationOverrides(
				value = { @AssociationOverride( name = "size", joinColumns = { @JoinColumn(name = "ASSOCIATION_ID") } ) }
		)
		@SQLRestriction("MAIN_CODE='MATERIAL' AND ASSOCIATION_CODE='SIZE'")
		@Immutable
		public Set<ContainedSize> getContainedSizesFromCombined() {
			return containedSizesFromCombined;
		}
		public void setContainedSizesFromCombined(Set<ContainedSize> containedSizesFromCombined) {
			this.containedSizesFromCombined = containedSizesFromCombined;
		}

		@ElementCollection
		@CollectionTable(
				name = "MATERIAL_RATINGS",
				joinColumns = { @JoinColumn( name = "MATERIAL_ID" ) }
		)
		@AssociationOverrides(
				value = { @AssociationOverride( name = "rating", joinColumns = { @JoinColumn(name = "RATING_ID") } ) }
		)
		@Immutable
		public Set<ContainedRating> getContainedRatings() {
			return containedRatings;
		}
		public void setContainedRatings(Set<ContainedRating> containedRatings) {
			this.containedRatings = containedRatings;
		}
	}

	@Entity( name = "Building" )
	@Table( name = "MAIN_TABLE" )
	@SQLRestriction( "CODE = 'BUILDING'" )
	public static class Building {
		private int id;
		private String name;
		private Set<ContainedSize> containedSizesFromCombined = new HashSet<>();
		private Set<ContainedRating> containedRatingsFromCombined = new HashSet<>();
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

		@ElementCollection
		@CollectionTable(
				name = "COLLECTION_TABLE",
				joinColumns = { @JoinColumn( name = "MAIN_ID" ) }
		)
		@SQLRestriction("MAIN_CODE='BUILDING' AND ASSOCIATION_CODE='SIZE'")
		@Immutable
		public Set<ContainedSize> getContainedSizesFromCombined() {
			return containedSizesFromCombined;
		}
		public void setContainedSizesFromCombined(Set<ContainedSize> containedSizesFromCombined) {
			this.containedSizesFromCombined = containedSizesFromCombined;
		}

		@ElementCollection
		@CollectionTable(
				name = "COLLECTION_TABLE",
				joinColumns = { @JoinColumn( name = "MAIN_ID" ) }
		)
		@SQLRestriction( "MAIN_CODE='BUILDING' AND ASSOCIATION_CODE='RATING'" )
		@Immutable
		public Set<ContainedRating> getContainedRatingsFromCombined() {
			return containedRatingsFromCombined;
		}
		public void setContainedRatingsFromCombined(Set<ContainedRating> containedRatingsFromCombined) {
			this.containedRatingsFromCombined = containedRatingsFromCombined;
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

	@Embeddable
	public static class ContainedSize {
		private Size size;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "ASSOCIATION_ID" )
		public Size getSize() {
			return size;
		}
		public void setSize(Size size) {
			this.size = size;
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

	@Embeddable
	public static class ContainedRating {
		private Rating rating;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "ASSOCIATION_ID" )
		public Rating getRating() {
			return rating;
		}
		public void setRating(Rating rating) {
			this.rating = rating;
		}
	}
}
