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
package org.hibernate.ejb.criteria;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;

import org.hibernate.ejb.test.TestCase;
import org.hibernate.ejb.test.callbacks.VideoSystem;
import org.hibernate.ejb.test.callbacks.Television;
import org.hibernate.ejb.test.callbacks.RemoteControl;
import org.hibernate.ejb.test.inheritance.Fruit;
import org.hibernate.ejb.test.inheritance.Strawberry;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class CriteriaCompilingTest extends TestCase {
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Customer.class,
				Item.class,
				Order.class,
				Product.class,
				// @Inheritance
				Fruit.class,
				Strawberry.class,
				// @MappedSuperclass
				VideoSystem.class,
				Television.class,
				RemoteControl.class
		};
	}

	public void testJustSimpleRootCriteria() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		// First w/o explicit selection...
		CriteriaQuery<Customer> criteria = em.getCriteriaBuilder().createQuery( Customer.class );
		criteria.from( Customer.class );
		em.createQuery( criteria ).getResultList();

		// Now with...
		criteria = em.getCriteriaBuilder().createQuery( Customer.class );
		Root<Customer> root = criteria.from( Customer.class );
		criteria.select( root );
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}

	public void testSimpleJoinCriteria() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		// String based...
		CriteriaQuery<Order> criteria = em.getCriteriaBuilder().createQuery( Order.class );
		Root<Order> root = criteria.from( Order.class );
		root.join( "lineItems" );
		criteria.select( root );
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}

	public void testSimpleFetchCriteria() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		// String based...
		CriteriaQuery<Order> criteria = em.getCriteriaBuilder().createQuery( Order.class );
		Root<Order> root = criteria.from( Order.class );
		root.fetch( "lineItems" );
		criteria.select( root );
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}

}
