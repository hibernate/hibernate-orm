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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.ejb.metamodel.Address;
import org.hibernate.ejb.metamodel.Alias;
import org.hibernate.ejb.metamodel.Country;
import org.hibernate.ejb.metamodel.CreditCard;
import org.hibernate.ejb.metamodel.Customer;
import org.hibernate.ejb.metamodel.Info;
import org.hibernate.ejb.metamodel.LineItem;
import org.hibernate.ejb.metamodel.Order;
import org.hibernate.ejb.metamodel.Phone;
import org.hibernate.ejb.metamodel.Product;
import org.hibernate.ejb.metamodel.ShelfLife;
import org.hibernate.ejb.metamodel.Spouse;
import org.hibernate.ejb.test.TestCase;
import org.hibernate.ejb.test.callbacks.RemoteControl;
import org.hibernate.ejb.test.callbacks.Television;
import org.hibernate.ejb.test.callbacks.VideoSystem;
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
				Alias.class,
				Phone.class,
				Address.class,
				Country.class,
				CreditCard.class,
				Info.class,
				Spouse.class,
				LineItem.class,
				Order.class,
				Product.class,
				ShelfLife.class,
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

	public void testSerialization() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaQuery<Order> criteria = em.getCriteriaBuilder().createQuery( Order.class );
		Root<Order> root = criteria.from( Order.class );
		root.fetch( "lineItems" );
		criteria.select( root );

		criteria = serializeDeserialize( criteria );

		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}

	private <T> T serializeDeserialize(T object) {
		T serializedObject = null;
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream( stream );
			out.writeObject( object );
			out.close();
			byte[] serialized = stream.toByteArray();
			stream.close();
			ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
			ObjectInputStream in = new ObjectInputStream( byteIn );
			serializedObject = (T) in.readObject();
			in.close();
			byteIn.close();
		}
		catch (Exception e) {
			fail("Unable to serialize / deserialize the object: " + e.getMessage() );
		}
		return serializedObject;
	}

}
