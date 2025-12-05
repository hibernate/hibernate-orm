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
@DomainModel(xmlMappings = "hbm/where/LazyElementCollectionBasicNonUniqueIdWhereTest.hbm.xml")
@SessionFactory(exportSchema = false)
public class LazyElementCollectionBasicNonUniqueIdWhereTest {
	@BeforeAll
	static void createSchema(SessionFactoryScope factoryScope) {
		org.hibernate.orm.test.where.annotations.LazyElementCollectionBasicNonUniqueIdWhereTest.applySchema( factoryScope );
	}

	@AfterAll
	static void dropSchema(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-12937")
	public void testInitializeFromUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
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
