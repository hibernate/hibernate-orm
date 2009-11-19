/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria.components;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Expression;

import org.hibernate.ejb.test.TestCase;

/**
 *
 * @author alan.oleary
 */
public class ComponentCriteriaTest extends TestCase {
	public Class[] getAnnotatedClasses() {
		return new Class[] { Client.class };
	}

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
		assertEquals(1, list.size());
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Client" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	public void testParameterizedFunctions() {
		// HHH-4586
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
