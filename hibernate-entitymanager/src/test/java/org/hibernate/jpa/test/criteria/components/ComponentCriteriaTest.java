/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.components;

import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Root;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertFalse;
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
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Client client = new Client( 111, "steve", "ebersole" );
		em.persist(client);
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Client> cq = cb.createQuery(Client.class);
		Root<Client> root = cq.from(Client.class);
		cq.where(cb.equal(root.get("name").get("firstName"), client.getName().getFirstName()));
		List<Client> list = em.createQuery(cq).getResultList();
		Assert.assertEquals( 1, list.size() );
		em.getTransaction().commit();
		em.close();
		
		// HHH-5792
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		TypedQuery< Client > q = em.createQuery(
				"SELECT c FROM Client c JOIN c.name n WHERE n.firstName = '"
						+ client.getName().getFirstName() + "'",
                 Client.class );
		Assert.assertEquals( 1, q.getResultList().size() );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Client" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9642")
	public void testOneToManyJoinFetchedInEmbeddable() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Client client = new Client( 111, "steve", "ebersole" );
		Alias alias = new Alias( "a", "guy", "work" );
		client.getName().getAliases().add( alias );
		em.persist(client);
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Client> cq = cb.createQuery(Client.class);
		Root<Client> root = cq.from(Client.class);
		root.fetch( Client_.name ).fetch( Name_.aliases );
		cq.where(cb.equal(root.get("name").get("firstName"), client.getName().getFirstName()));
		List<Client> list = em.createQuery(cq).getResultList();
		Assert.assertEquals( 1, list.size() );
		client = list.get( 0 );
		assertTrue( Hibernate.isInitialized( client.getName().getAliases() ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		TypedQuery< Client > q = em.createQuery(
				"SELECT c FROM Client c JOIN FETCH c.name.aliases WHERE c.name.firstName = '"
						+ client.getName().getFirstName() + "'",
				Client.class
		);
		Assert.assertEquals( 1, q.getResultList().size() );
		client = list.get( 0 );
		assertTrue( Hibernate.isInitialized( client.getName().getAliases() ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		q = em.createQuery(
				"SELECT c FROM Client c JOIN  c.name n join FETCH n.aliases WHERE c.name.firstName = '"
						+ client.getName().getFirstName() + "'",
				Client.class
		);
		Assert.assertEquals( 1, q.getResultList().size() );
		client = list.get( 0 );
		assertTrue( Hibernate.isInitialized( client.getName().getAliases() ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		client = em.merge( client );
		em.remove( client );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4586" )
	public void testParameterizedFunctions() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		// lower
		CriteriaQuery<Client> cq = cb.createQuery( Client.class );
		Root<Client> root = cq.from( Client.class );
		cq.where( cb.equal( cb.lower( root.get( Client_.name ).get( Name_.lastName ) ),"test" ) );
		em.createQuery( cq ).getResultList();
		// upper
		cq = cb.createQuery( Client.class );
		root = cq.from( Client.class );
		cq.where( cb.equal( cb.upper( root.get( Client_.name ).get( Name_.lastName ) ),"test" ) );
		em.createQuery( cq ).getResultList();
		em.getTransaction().commit();
		em.close();
	}
}
