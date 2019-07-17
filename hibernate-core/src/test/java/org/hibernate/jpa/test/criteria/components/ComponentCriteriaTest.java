/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.components;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author alan.oleary
 */
public class ComponentCriteriaTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Client.class, Alias.class };
	}

	@Test
	public void testEmbeddableInPath() {
		Client client = new Client( 111, "steve", "ebersole" );
		doInJPA( this::entityManagerFactory, em -> {
			em.persist( client );
		} );

		doInJPA( this::entityManagerFactory, em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Client> cq = cb.createQuery( Client.class );
			Root<Client> root = cq.from( Client.class );
			cq.where( cb.equal( root.get( "name" ).get( "firstName" ), client.getName().getFirstName() ) );
			List<Client> list = em.createQuery( cq ).getResultList();
			Assert.assertEquals( 1, list.size() );
		} );

		// HHH-5792
		doInJPA( this::entityManagerFactory, em -> {
			TypedQuery<Client> q = em.createQuery(
					"SELECT c FROM Client c JOIN c.name n WHERE n.firstName = '"
							+ client.getName().getFirstName() + "'",
					Client.class
			);
			Assert.assertEquals( 1, q.getResultList().size() );
		} );

		doInJPA( this::entityManagerFactory, em -> {
			em.createQuery( "delete Client" ).executeUpdate();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9642")
	public void testOneToManyJoinFetchedInEmbeddable() {
		Client client = new Client( 111, "steve", "ebersole" );
		Alias alias = new Alias( "a", "guy", "work" );
		client.getName().getAliases().add( alias );
		doInJPA( this::entityManagerFactory, em -> {
			em.persist( client );
		} );

		List<Client> list = new ArrayList<>();
		doInJPA( this::entityManagerFactory, em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Client> cq = cb.createQuery( Client.class );
			Root<Client> root = cq.from( Client.class );
			root.fetch( Client_.name ).fetch( Name_.aliases );
			cq.where( cb.equal( root.get( "name" ).get( "firstName" ), client.getName().getFirstName() ) );
			list.addAll( em.createQuery( cq ).getResultList() );
			Assert.assertEquals( 1, list.size() );
			Client c = list.get( 0 );
			assertTrue( Hibernate.isInitialized( c.getName().getAliases() ) );
		} );

		doInJPA( this::entityManagerFactory, em -> {
			TypedQuery<Client> q = em.createQuery(
					"SELECT c FROM Client c JOIN FETCH c.name.aliases WHERE c.name.firstName = '"
							+ client.getName().getFirstName() + "'",
					Client.class
			);
			Assert.assertEquals( 1, q.getResultList().size() );
			Client c = list.get( 0 );
			assertTrue( Hibernate.isInitialized( c.getName().getAliases() ) );
		} );

		doInJPA( this::entityManagerFactory, em -> {
			TypedQuery<Client> q = em.createQuery(
					"SELECT c FROM Client c JOIN  c.name n join FETCH n.aliases WHERE c.name.firstName = '"
							+ client.getName().getFirstName() + "'",
					Client.class
			);
			Assert.assertEquals( 1, q.getResultList().size() );
			Client c = list.get( 0 );
			assertTrue( Hibernate.isInitialized( c.getName().getAliases() ) );
		} );

		doInJPA( this::entityManagerFactory, em -> {
			Client c = em.merge( client );
			em.remove( c );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4586")
	public void testParameterizedFunctions() {
		doInJPA( this::entityManagerFactory, em -> {
					 CriteriaBuilder cb = em.getCriteriaBuilder();
					 // lower
					 CriteriaQuery<Client> cq = cb.createQuery( Client.class );
					 Root<Client> root = cq.from( Client.class );
					 cq.where( cb.equal( cb.lower( root.get( Client_.name ).get( Name_.lastName ) ), "test" ) );
					 em.createQuery( cq ).getResultList();
					 // upper
					 cq = cb.createQuery( Client.class );
					 root = cq.from( Client.class );
					 cq.where( cb.equal( cb.upper( root.get( Client_.name ).get( Name_.lastName ) ), "test" ) );
					 em.createQuery( cq ).getResultList();
				 }
		);
	}
}
