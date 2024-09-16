/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.nestedfetch;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa( annotatedClasses = {
		NestedFetchTest.RootEntity.class,
		NestedFetchTest.Chil1PK.class,
		NestedFetchTest.Child1Entity.class,
		NestedFetchTest.Child2Entity.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16905" )
public class NestedFetchTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final RootEntity root = new RootEntity();
			entityManager.persist( root );
			final Child2Entity child2 = new Child2Entity( "222" );
			entityManager.persist( child2 );
			final Child1Entity child1 = new Child1Entity( root, child2 );
			entityManager.persist( child1 );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from Child1Entity" ).executeUpdate();
			entityManager.createQuery( "delete from Child2Entity" ).executeUpdate();
			entityManager.createQuery( "delete from RootEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testNestedFetch(EntityManagerFactoryScope scope) {
		RootEntity rootEntity = scope.fromTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<RootEntity> cq = cb.createQuery( RootEntity.class );
			final Root<RootEntity> fromRoot = cq.from( RootEntity.class );
			final Fetch<RootEntity, Child1Entity> fetchDepth1 = fromRoot.fetch(
					"child1Entities",
					JoinType.LEFT
			);
			final Fetch<Child1Entity, Child2Entity> fetchDepth2 = fetchDepth1.fetch(
					"child2Entity",
					JoinType.LEFT
			);
			return entityManager.createQuery( cq.select( fromRoot ) ).getSingleResult();
		} );
		//dependent relations should already be fetched and be available outside the transaction
		assertThat( rootEntity.getChild1Entities().size() ).isEqualTo( 1 );
		final Child1Entity depth1Entity = rootEntity.getChild1Entities().iterator().next();
		final Child2Entity depth2Entity = depth1Entity.getChild2Entity();
		assertThat( depth2Entity ).isNotNull();
		assertThat( depth2Entity.getVal() ).isEqualTo( "222" );
	}

	@Entity( name = "RootEntity" )
	public static class RootEntity {
		@Id
		@GeneratedValue
		private int id;

		@OneToMany( mappedBy = "rootEntity" )
		private Set<Child1Entity> child1Entities;

		public Set<Child1Entity> getChild1Entities() {
			return child1Entities;
		}
	}

	@Embeddable
	public static class Chil1PK implements Serializable {
		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "rootId", referencedColumnName = "id", nullable = false )
		private RootEntity rootEntity;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "child2Id", referencedColumnName = "id", nullable = false )
		private Child2Entity child2Entity;
	}

	@Entity( name = "Child1Entity" )
	@IdClass( Chil1PK.class )
	public static class Child1Entity {
		@Id
		@PrimaryKeyJoinColumn( name = "rootId", referencedColumnName = "id" )
		@ManyToOne( fetch = FetchType.LAZY )
		private RootEntity rootEntity;

		@Id
		@PrimaryKeyJoinColumn( name = "child2Id", referencedColumnName = "id" )
		@ManyToOne( fetch = FetchType.LAZY )
		private Child2Entity child2Entity;

		public Child1Entity() {
		}

		public Child1Entity(RootEntity rootEntity, Child2Entity child2Entity) {
			this.rootEntity = rootEntity;
			this.child2Entity = child2Entity;
		}

		public Child2Entity getChild2Entity() {
			return child2Entity;
		}
	}


	@Entity( name = "Child2Entity" )
	public static class Child2Entity {
		@Id
		@GeneratedValue
		private int id;

		@OneToMany( mappedBy = "child2Entity" )
		private Set<Child1Entity> child1Entities = new HashSet<>();

		private String val;

		public Child2Entity() {
		}

		public Child2Entity(String val) {
			this.val = val;
		}

		public String getVal() {
			return val;
		}

		public Set<Child1Entity> getChild1Entities() {
			return child1Entities;
		}
	}
}
