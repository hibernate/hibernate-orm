/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.collection;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa( annotatedClasses = { Child.class, Parent.class } )
public class PostLoadTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Parent parent1 = new Parent();
			parent1.setId( 1 );
			final Child child1 = new Child();
			child1.setId( 1 );
			child1.setDaddy( parent1 );
			entityManager.persist( parent1 );
			entityManager.persist( child1 );
			final Parent parent2 = new Parent();
			parent2.setId( 2 );
			final Child child2 = new Child();
			child2.setId( 2 );
			child2.setDaddy( parent2 );
			entityManager.persist( parent2 );
			entityManager.persist( child2 );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from Child" ).executeUpdate();
			entityManager.createQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	/**
	 * Load an entity with a collection of associated entities and verify that @PostLoad
	 * was invoked without accessing the association from the callback.
	 */
	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-6043" )
	public void testAssociatedSetAfterPostLoad(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Parent daddy = entityManager.find( Parent.class, 1 );
					assertTrue( daddy.isPostLoadCalled() );
					assertEquals( 1, daddy.getNrOfChildren() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17489" )
	public void testAssociatedSetAfterPostLoadQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final CriteriaQuery<Parent> cq = entityManager.getCriteriaBuilder().createQuery( Parent.class );
			final Root<Parent> from = cq.from( Parent.class );
			final List<Parent> parents = entityManager.createQuery( cq.select( from ) ).getResultList();
			assertEquals( 2, parents.size() );
			assertTrue( parents.get( 0 ).isPostLoadCalled() );
			assertEquals( 1, parents.get( 0 ).getNrOfChildren() );
			assertTrue( parents.get( 1 ).isPostLoadCalled() );
			assertEquals( 1, parents.get( 1 ).getNrOfChildren() );
		} );
	}
}
