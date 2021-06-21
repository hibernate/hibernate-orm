/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.components;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isInitialized;

/**
 * @author alan.oleary
 */
@Jpa( annotatedClasses = { Client.class, Alias.class } )
public class ComponentCriteriaTest {

	@Test
	void testEmbeddableInPath(EntityManagerFactoryScope scope) {
		Client client = scope.fromTransaction( em -> {
			Client c = new Client( 111, "steve", "ebersole" );
			em.persist( c );
			return c;
		} );

		scope.inTransaction( em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Client> cq = cb.createQuery( Client.class );
			Root<Client> root = cq.from( Client.class );
			cq.where( cb.equal( root.get( "name" ).get( "firstName" ), client.getName().getFirstName() ) );
			List<Client> list = em.createQuery( cq ).getResultList();
			assertThat( list, hasSize( 1 ) );
		} );

		// HHH-5792
		scope.inTransaction( em -> {
			TypedQuery<Client> q = em.createQuery(
					"SELECT c FROM Client c JOIN c.name n WHERE n.firstName = '"
							+ client.getName().getFirstName() + "'",
					Client.class
			);
			assertThat( q.getResultList(), hasSize( 1 ) );
		} );

		scope.inTransaction( em -> em.createQuery( "delete Client" ).executeUpdate() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9642")
	void testOneToManyJoinFetchedInEmbeddable(EntityManagerFactoryScope scope) {
		Client client = scope.fromTransaction( em -> {
			Client c = new Client( 111, "steve", "ebersole" );
			Alias alias = new Alias( "a", "guy", "work" );
			c.getName().getAliases().add( alias );
			em.persist( c );
			return c;
		} );

		List<Client> list = new ArrayList<>();
		scope.inTransaction( em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Client> cq = cb.createQuery( Client.class );
			Root<Client> root = cq.from( Client.class );
			root.fetch( Client_.NAME).fetch( Name_.ALIASES );
			cq.where( cb.equal( root.get( "name" ).get( "firstName" ), client.getName().getFirstName() ) );
			list.addAll( em.createQuery( cq ).getResultList() );
			assertThat( list, hasSize( 1 ) );
			Client c = list.get( 0 );
			assertThat( c.getName().getAliases(), isInitialized() );
		} );

		scope.inTransaction( em -> {
			TypedQuery<Client> q = em.createQuery(
					"SELECT c FROM Client c JOIN FETCH c.name.aliases WHERE c.name.firstName = '"
							+ client.getName().getFirstName() + "'",
					Client.class
			);
			assertThat( q.getResultList(), hasSize( 1 ) );
			Client c = list.get( 0 );
			assertThat( c.getName().getAliases(), isInitialized() );
		} );

		scope.inTransaction( em -> {
			TypedQuery<Client> q = em.createQuery(
					"SELECT c FROM Client c JOIN  c.name n join FETCH n.aliases WHERE c.name.firstName = '"
							+ client.getName().getFirstName() + "'",
					Client.class
			);
			assertThat( q.getResultList(), hasSize( 1 ) );
			Client c = list.get( 0 );
			assertThat( c.getName().getAliases(), isInitialized() );
		} );

		scope.inTransaction( em -> {
			client.getName().getAliases().clear();
			em.merge( client );
		} );
		scope.inTransaction( em -> {
			em.createQuery( "delete Alias" ).executeUpdate();
			em.createQuery( "delete Client" ).executeUpdate();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4586")
	void testParameterizedFunctions(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			// lower
			CriteriaQuery<Client> cq = cb.createQuery( Client.class );
			Root<Client> root = cq.from( Client.class );
			cq.where( cb.equal( cb.lower( root.get( Client_.NAME ).get( Name_.LAST_NAME ) ), "test" ) );
			em.createQuery( cq ).getResultList();
			// upper
			cq = cb.createQuery( Client.class );
			root = cq.from( Client.class );
			cq.where( cb.equal( cb.upper( root.get( Client_.NAME ).get( Name_.LAST_NAME ) ), "test" ) );
			em.createQuery( cq ).getResultList();
		} );
	}
}
