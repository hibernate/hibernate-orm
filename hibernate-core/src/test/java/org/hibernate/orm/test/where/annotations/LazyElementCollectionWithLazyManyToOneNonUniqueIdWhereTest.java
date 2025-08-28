/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.hibernate.Session;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;
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
public class LazyElementCollectionWithLazyManyToOneNonUniqueIdWhereTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Material.class, Building.class, Rating.class, Size.class };
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
									"ASSOCIATION_ID int not null, ASSOCIATION_CODE varchar(10) not null, " +
									"primary key (MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE))"
					).executeUpdate();

					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 1, 'RATING' )"
					).executeUpdate();
					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 2, 'RATING' )"
					).executeUpdate();
					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 3, 'RATING' )"
					).executeUpdate();

					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 2, 'SIZE' )"
					).executeUpdate();

					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'BUILDING', 1, 'RATING' )"
					).executeUpdate();

					session.createNativeQuery(
							"insert into COLLECTION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'BUILDING', 1, 'SIZE' )"
					).executeUpdate();


					session.createNativeQuery(
							"create table MATERIAL_RATINGS( " +
									"MATERIAL_ID integer not null, RATING_ID integer not null," +
									" primary key (MATERIAL_ID, RATING_ID))"
					).executeUpdate();

					session.createNativeQuery(
							"insert into MATERIAL_RATINGS(MATERIAL_ID, RATING_ID) VALUES( 1, 1 )"
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
					assertFalse( Hibernate.isInitialized( material.getContainedRatings() ) );
					assertEquals( 1, material.getContainedRatings().size() );
					assertTrue( Hibernate.isInitialized( material.getContainedRatings() ) );

					final ContainedRating containedRating = material.getContainedRatings().iterator().next();
					assertTrue( Hibernate.isInitialized( containedRating ) );
					assertEquals( "high", containedRating.getRating().getName() );

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

					// Material#containedSizesFromCombined is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( material.getContainedSizesFromCombined() ) );
					assertEquals( 1, material.getContainedSizesFromCombined().size() );
					assertTrue( Hibernate.isInitialized( material.getContainedSizesFromCombined() ) );

					ContainedSize containedSize = material.getContainedSizesFromCombined().iterator().next();
					assertFalse( Hibernate.isInitialized( containedSize.getSize() ) );
					assertEquals( "medium", containedSize.getSize().getName() );

					Building building = session.get( Building.class, 1 );

					// building.ratingsFromCombined is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( building.getContainedRatingsFromCombined() ) );
					assertEquals( 1, building.getContainedRatingsFromCombined().size() );
					assertTrue( Hibernate.isInitialized( building.getContainedRatingsFromCombined() ) );
					ContainedRating containedRating = building.getContainedRatingsFromCombined().iterator().next();
					assertFalse( Hibernate.isInitialized( containedRating.getRating() ) );
					assertEquals( "high", containedRating.getRating().getName() );

					// Building#containedSizesFromCombined is mapped with lazy="true"
					assertFalse( Hibernate.isInitialized( building.getContainedSizesFromCombined() ) );
					assertEquals( 1, building.getContainedSizesFromCombined().size() );
					assertTrue( Hibernate.isInitialized( building.getContainedSizesFromCombined() ) );
					containedSize = building.getContainedSizesFromCombined().iterator().next();
					assertFalse( Hibernate.isInitialized( containedSize.getSize() ) );
					assertEquals( "small", containedSize.getSize().getName() );
		}
		session.getTransaction().commit();
		session.close();
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
