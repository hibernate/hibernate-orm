/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.where.annotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.WhereJoinTable;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public class LazyManyToManyNonUniqueIdWhereTest extends BaseCoreFunctionalTestCase {

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Material.class, Building.class, Rating.class, Size.class };
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
					session.createSQLQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 2, 'medium', 'RATING' )" )
							.executeUpdate();
					session.createSQLQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 3, 'low', 'RATING' )" )
							.executeUpdate();
					session.createSQLQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 1, 'small', 'SIZE' )" )
							.executeUpdate();
					session.createSQLQuery( "insert into MAIN_TABLE(ID, NAME, CODE) VALUES( 2, 'medium', 'SIZE' )" )
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
					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 2, 'RATING' )"
					).executeUpdate();
					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 3, 'RATING' )"
					).executeUpdate();

					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'MATERIAL', 2, 'SIZE' )"
					).executeUpdate();

					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'BUILDING', 1, 'RATING' )"
					).executeUpdate();

					session.createSQLQuery(
							"insert into ASSOCIATION_TABLE(MAIN_ID, MAIN_CODE, ASSOCIATION_ID, ASSOCIATION_CODE) " +
									"VALUES( 1, 'BUILDING', 1, 'SIZE' )"
					).executeUpdate();


					session.createSQLQuery(
							"create table MATERIAL_RATINGS( " +
									"MATERIAL_ID integer not null, RATING_ID integer not null," +
									" primary key (MATERIAL_ID, RATING_ID))"
					).executeUpdate();

					session.createSQLQuery(
							"insert into MATERIAL_RATINGS(MATERIAL_ID, RATING_ID) VALUES( 1, 1 )"
					).executeUpdate();

					session.createSQLQuery(
							"create table BUILDING_RATINGS( " +
									"BUILDING_ID integer not null, RATING_ID integer not null," +
									" primary key (BUILDING_ID, RATING_ID))"
					).executeUpdate();

					session.createSQLQuery(
							"insert into BUILDING_RATINGS(BUILDING_ID, RATING_ID) VALUES( 1, 1 )"
					).executeUpdate();
					session.createSQLQuery(
							"insert into BUILDING_RATINGS(BUILDING_ID, RATING_ID) VALUES( 1, 2 )"
					).executeUpdate();
					session.createSQLQuery(
							"insert into BUILDING_RATINGS(BUILDING_ID, RATING_ID) VALUES( 1, 3 )"
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
					assertFalse( Hibernate.isInitialized( building.getMediumOrHighRatings() ) );
					checkMediumOrHighRatings( building.getMediumOrHighRatings() );
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
				}
		);
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
	@Where( clause = "CODE = 'MATERIAL'" )
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
		@WhereJoinTable( clause = "MAIN_CODE='MATERIAL' AND ASSOCIATION_CODE='SIZE'")
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
		@WhereJoinTable( clause = "MAIN_CODE='MATERIAL' AND ASSOCIATION_CODE='RATING'" )
		@Where( clause = "name = 'high' or name = 'medium'" )
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
	@Where( clause = "CODE = 'BUILDING'" )
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
		@WhereJoinTable( clause = "MAIN_CODE='BUILDING' AND ASSOCIATION_CODE='SIZE'")
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
		@WhereJoinTable( clause = "MAIN_CODE='BUILDING' AND ASSOCIATION_CODE='RATING'" )
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
		@Where( clause = "name = 'high' or name = 'medium'" )
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
	@Where( clause = "CODE = 'SIZE'" )
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
	@Where( clause = "CODE = 'RATING'" )
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
