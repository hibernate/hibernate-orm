/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * Load an entity with a collection of associated entities, that uses a @PostLoad method to
	 * access the association.
	 */
	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-6043" )
	public void testAccessAssociatedSetInPostLoad(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Parent daddy = entityManager.find( Parent.class, 1 );
					assertEquals( 1, daddy.getNrOfChildren() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17489" )
	public void testAccessAssociatedSetInPostLoadQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final CriteriaQuery<Parent> cq = entityManager.getCriteriaBuilder().createQuery( Parent.class );
			final Root<Parent> from = cq.from( Parent.class );
			final List<Parent> parents = entityManager.createQuery( cq.select( from ) ).getResultList();
			assertEquals( 2, parents.size() );
			assertEquals( 1, parents.get( 0 ).getNrOfChildren() );
			assertEquals( 1, parents.get( 1 ).getNrOfChildren() );
		} );
	}
}
