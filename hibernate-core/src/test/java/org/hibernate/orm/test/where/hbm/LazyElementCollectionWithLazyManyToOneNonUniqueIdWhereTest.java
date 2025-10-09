/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.hbm;

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
@DomainModel(xmlMappings = "hbm/where/LazyElementCollectionWithLazyManyToOneNonUniqueIdWhereTest.hbm.xml")
@SessionFactory(exportSchema = false)
public class LazyElementCollectionWithLazyManyToOneNonUniqueIdWhereTest {
	@BeforeAll
	public void createSchema(SessionFactoryScope factoryScope) {
		org.hibernate.orm.test.where.annotations.LazyElementCollectionWithLazyManyToOneNonUniqueIdWhereTest.applySchema( factoryScope );
	}

	@AfterAll
	public void dropSchema(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-12937")
	public void testInitializeFromUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Material material = session.find( Material.class, 1 );
			assertEquals( "plastic", material.getName() );

			// Material#ratings is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( material.getContainedRatings() ) );
			assertEquals( 1, material.getContainedRatings().size() );
			assertTrue( Hibernate.isInitialized( material.getContainedRatings() ) );

			final ContainedRating containedRating = material.getContainedRatings().iterator().next();
			assertTrue( Hibernate.isInitialized( containedRating ) );
			assertEquals( "high", containedRating.getRating().getName() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-12937")
	public void testInitializeFromNonUniqueAssociationTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Material material = session.find( Material.class, 1 );
			assertEquals( "plastic", material.getName() );

			// Material#containedSizesFromCombined is mapped with lazy="true"
			assertFalse( Hibernate.isInitialized( material.getContainedSizesFromCombined() ) );
			assertEquals( 1, material.getContainedSizesFromCombined().size() );
			assertTrue( Hibernate.isInitialized( material.getContainedSizesFromCombined() ) );

			ContainedSize containedSize = material.getContainedSizesFromCombined().iterator().next();
			assertFalse( Hibernate.isInitialized( containedSize.getSize() ) );
			assertEquals( "medium", containedSize.getSize().getName() );

			Building building = session.find( Building.class, 1 );

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
		} );
	}

	public static class Material {
		private int id;

		private String name;
		private Set<ContainedSize> containedSizesFromCombined = new HashSet<>();
		private List<Rating> mediumOrHighRatingsFromCombined = new ArrayList<>();
		private Set<ContainedRating> containedRatings = new HashSet<>();

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

		public Set<ContainedSize> getContainedSizesFromCombined() {
			return containedSizesFromCombined;
		}
		public void setContainedSizesFromCombined(Set<ContainedSize> containedSizesFromCombined) {
			this.containedSizesFromCombined = containedSizesFromCombined;
		}

		public Set<ContainedRating> getContainedRatings() {
			return containedRatings;
		}
		public void setContainedRatings(Set<ContainedRating> containedRatings) {
			this.containedRatings = containedRatings;
		}
	}

	public static class Building {
		private int id;
		private String name;
		private Set<ContainedSize> containedSizesFromCombined = new HashSet<>();
		private Set<ContainedRating> containedRatingsFromCombined = new HashSet<>();
		private List<Rating> mediumOrHighRatings = new ArrayList<>();

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

		public Set<ContainedSize> getContainedSizesFromCombined() {
			return containedSizesFromCombined;
		}
		public void setContainedSizesFromCombined(Set<ContainedSize> containedSizesFromCombined) {
			this.containedSizesFromCombined = containedSizesFromCombined;
		}

		public Set<ContainedRating> getContainedRatingsFromCombined() {
			return containedRatingsFromCombined;
		}
		public void setContainedRatingsFromCombined(Set<ContainedRating> containedRatingsFromCombined) {
			this.containedRatingsFromCombined = containedRatingsFromCombined;
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

	public static class ContainedSize {
		private Size size;

		public Size getSize() {
			return size;
		}
		public void setSize(Size size) {
			this.size = size;
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

	public static class ContainedRating {
		private Rating rating;

		public Rating getRating() {
			return rating;
		}
		public void setRating(Rating rating) {
			this.rating = rating;
		}
	}
}
