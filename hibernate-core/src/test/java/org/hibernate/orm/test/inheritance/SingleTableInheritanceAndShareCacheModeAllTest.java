/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SharedCacheMode;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(
		annotatedClasses = {
				SingleTableInheritanceAndShareCacheModeAllTest.Cheese.class,
				SingleTableInheritanceAndShareCacheModeAllTest.SpecialCheese.class,
				SingleTableInheritanceAndShareCacheModeAllTest.Hole.class
		},
		sharedCacheMode = SharedCacheMode.ALL
)
@JiraKey(value = "HHH-15699")
public class SingleTableInheritanceAndShareCacheModeAllTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			Cheese cheese = new Cheese();
			entityManager.persist( cheese );

			Hole hole = new Hole();
			hole.setCheese( cheese );
			entityManager.persist( hole );
		} );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testCriteria(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			var criteria = entityManager.getCriteriaBuilder().createQuery( Cheese.class );
			var cheeses = entityManager
					.createQuery( criteria.select( criteria.from( Cheese.class ) ) )
					.getResultList();
			assertThat( cheeses.size() ).isEqualTo( 1 );
			assertThat( cheeses.get( 0 ).getHoles().size() ).isEqualTo( 1 );
		} );
	}

	@SuppressWarnings({"unused", "FieldMayBeFinal"})
	@Entity(name = "Cheese")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Cheese {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_sequence")
		private Long id;

		private String name;

		@OneToMany(mappedBy = "cheese", fetch = FetchType.LAZY)
		private List<Hole> holes = new ArrayList<>();

		public List<Hole> getHoles() {
			return holes;
		}

		public void addHole(Hole hole) {
			this.holes.add( hole );
		}
	}

	@Entity(name = "SpecialCheese")
	public static class SpecialCheese extends Cheese {
	}

	@SuppressWarnings("unused")
	@Entity(name = "Hole")
	public static class Hole {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_sequence")
		private Long id;

		@Column(name = "SIZE_COLUMN")
		private int size;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "cheese", nullable = false)
		private Cheese cheese;

		public Cheese getCheese() {
			return cheese;
		}

		public void setCheese(Cheese cheese) {
			this.cheese = cheese;
			cheese.addHole( this );
		}
	}

}
