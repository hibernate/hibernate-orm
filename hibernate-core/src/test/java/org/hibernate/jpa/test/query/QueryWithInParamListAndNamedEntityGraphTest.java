/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * Based on the test developed by Hans Desmet to reproduce the bug reported in HHH-9230
 *
 * @author Steve Ebersole
 */
@TestForIssue(jiraKey = "HHH-9230")
public class QueryWithInParamListAndNamedEntityGraphTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class};
	}

	@Test
	public void testInClause() {
		// this test works
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Set<Long> ids = new HashSet<Long>();
		ids.add( 1L );
		ids.add( 2L );
		TypedQuery<Person> query = em.createQuery( "select p from Person p where p.id  in :ids", Person.class );
		query.setParameter( "ids", ids );
		query.getResultList();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testEntityGraph() {
		// this test works
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		TypedQuery<Person> query = em.createQuery( "select p from Person p", Person.class );
		query.setHint( "jakarta.persistence.loadgraph", em.createEntityGraph( "withBoss" ) );
		query.getResultList();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testEntityGraphAndInClause() {
		// this test fails
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Set<Long> ids = new HashSet<Long>();
		ids.add( 1L );
		ids.add( 2L );
		TypedQuery<Person> query = em.createQuery( "select p from Person p where p.id  in :ids", Person.class );
		query.setHint( "jakarta.persistence.loadgraph", em.createEntityGraph( "withBoss" ) );
		query.setParameter( "ids", ids );
		query.getResultList();
		em.getTransaction().commit();
		em.close();
	}

	@Entity(name = "Person")
	@Table(name = "Person")
	@NamedEntityGraph(name = "withBoss", attributeNodes = @NamedAttributeNode("boss"))
	public static class Person {
		@Id
		@GeneratedValue
		private long id;
		private String name;
		@ManyToOne
		@JoinColumn
		private Person boss;
	}
}
