/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				EntityGraphAttributeNodesTest.Human.class,
				EntityGraphAttributeNodesTest.House.class,
				EntityGraphAttributeNodesTest.Address.class,
		}
)
@JiraKey("HHH-16885")
public class EntityGraphAttributeNodesTest {

	private final static Integer HUMAN_ID = 1;

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Human human = new Human( HUMAN_ID );
					entityManager.persist( human );
				}
		);
	}

	@Test
	public void testIt(EntityManagerFactoryScope scope) {

		scope.inEntityManager(
				entityManager -> {
					EntityGraph<?> entityGraph = entityManager.createEntityGraph( Human.class );
					entityGraph.addSubgraph( "houses" ).addAttributeNodes( "address" );

					List results = entityManager.createQuery( "SELECT h FROM Human h WHERE h.id = ?1" )
							.setParameter( 1, HUMAN_ID )
							.setHint( "javax.persistence.fetchgraph", entityGraph )
							.getResultList();

					assertThat( results.size() ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).isNotNull();
				}
		);
	}

	@Entity(name = "Human")
	public static class Human {
		@Id
		private Integer id;

		private String name;

		@OneToMany(mappedBy = "human", cascade = CascadeType.ALL)
		@OnDelete(action = OnDeleteAction.CASCADE)
		private Collection<House> houses;

		public Human() {
		}

		public Human(Integer id) {
			this.id = id;
			this.houses = new ArrayList<>();
		}

		public Human(Integer id, String name, Collection<House> houses) {
			this.id = id;
			this.name = name;
			this.houses = houses;
		}
	}

	@Entity(name = "House")
	public static class House {
		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "human_fk", nullable = false, updatable = false)
		private Human human;

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "address_fk", nullable = false, updatable = false)
		private Address address;

		private String name;
	}

	@Entity(name = "Address")
	public static class Address {

		@Id
		@GeneratedValue
		private BigInteger id;

		private String street;

	}
}
