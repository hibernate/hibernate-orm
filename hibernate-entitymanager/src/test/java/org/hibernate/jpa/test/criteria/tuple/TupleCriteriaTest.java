/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.test.criteria.tuple;

import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.junit.Test;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Customer;
import org.hibernate.jpa.test.metamodel.Customer_;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class TupleCriteriaTest extends AbstractMetamodelSpecificTest {

	@Test
	public void testArray() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		Customer c1 = new Customer();
		c1.setId( "c1" );
		c1.setAge( 18 );
		c1.setName( "Bob" );
		em.persist( c1 );
		em.getTransaction().commit();
		em.close();

		em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		final CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
		Root<Customer> c = q.from(Customer.class);
		q.select( cb.array( c.get(Customer_.name), c.get(Customer_.age) ) );
		List<Object[]> result = em.createQuery(q).getResultList();
		assertEquals( 1, result.size() );
		assertEquals( c1.getName(), result.get( 0 )[0] );
		assertEquals( c1.getAge(), result.get( 0 )[1] );
		em.getTransaction().commit();
		em.close();

		em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Customer" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testTuple() {
		EntityManager em = entityManagerFactory().createEntityManager();

		em.getTransaction().begin();
		Customer c1 = new Customer();
		c1.setId( "c1" );
		c1.setAge( 18 );
		c1.setName( "Bob" );
		em.persist( c1 );
		em.getTransaction().commit();
		em.close();

		em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
		Root<Customer> customerRoot = criteria.from( Customer.class );
		Path<String> namePath = customerRoot.get( Customer_.name );
		Path<Integer> agePath = customerRoot.get( Customer_.age );
		agePath.alias( "age" );
		criteria.multiselect( namePath, agePath );
		List<Tuple> results = em.createQuery( criteria ).getResultList();
		assertEquals( 1, results.size() );
		Object resultElement = results.get( 0 );
		assertTrue( "Check  result 'row' as Tuple", Tuple.class.isInstance( resultElement ) );
		Tuple resultElementTuple = (Tuple) resultElement;
		Object[] tupleArray = resultElementTuple.toArray();
		assertEquals( 2, tupleArray.length );
		assertEquals( tupleArray[0], resultElementTuple.get( 0 ) );
		assertEquals( resultElementTuple.get( namePath ), resultElementTuple.get( 0 ) );
		assertEquals( tupleArray[1], resultElementTuple.get( 1 ) );
		assertEquals( resultElementTuple.get( agePath ), resultElementTuple.get( 1 ) );
		assertEquals( resultElementTuple.get( agePath ), resultElementTuple.get( "age" ) );
		em.getTransaction().commit();
		em.close();

		em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Customer" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testIllegalArgumentExceptionBuildingTupleWithSameAliases() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
		Root<Customer> customerRoot = criteria.from( Customer.class );
		Path<String> namePath = customerRoot.get( Customer_.name );
		namePath.alias( "age" );
		Path<Integer> agePath = customerRoot.get( Customer_.age );
		agePath.alias( "age" );
		try {
			criteria.multiselect( namePath, agePath );
			fail( "Attempt to define multi-select with same aliases should have thrown IllegalArgumentException" );
		}
		catch (IllegalArgumentException expected) {
		}
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testVariousTupleAccessMethods() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		Customer c1 = new Customer();
		c1.setId( "c1" );
		c1.setAge( 18 );
		c1.setName( "Bob" );
		em.persist( c1 );
		em.getTransaction().commit();
		em.close();

		em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();

		final CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
		Root<Customer> customerRoot = criteria.from( Customer.class );
		Path<String> namePath = customerRoot.get( Customer_.name );
		namePath.alias( "NAME" );
		Path<Integer> agePath = customerRoot.get( Customer_.age );
		agePath.alias( "AGE" );
		criteria.multiselect( namePath, agePath );

		List<Tuple> results = em.createQuery( criteria ).getResultList();
		Tuple tuple = results.get( 0 );
		assertNotNull( tuple );
		assertNotNull( tuple.get( "NAME" ) );
		assertNotNull( tuple.get( "NAME", String.class ) );
		try {
			tuple.get( "NAME", Date.class );
			fail( "Accessing Customer#name tuple as Date should have thrown exception" );
		}
		catch (IllegalArgumentException expected) {
		}

		em.getTransaction().commit();
		em.close();

		em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Customer" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testIllegalArgumentExceptionBuildingSelectArrayWithSameAliases() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery criteria = builder.createQuery();
		Root<Customer> customerRoot = criteria.from( Customer.class );
		Path<String> namePath = customerRoot.get( Customer_.name );
		Path<Integer> agePath = customerRoot.get( Customer_.age );
		try {
			CompoundSelection<Object[]> c = builder.array( namePath.alias( "SAME" ), agePath.alias( "SAME" ) );
			criteria.select( c );
			fail( "Attempt to define multi-select with same aliases should have thrown IllegalArgumentException" );
		}
		catch (IllegalArgumentException expected) {
		}
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testInvalidTupleIndexAccess() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		Customer c1 = new Customer();
		c1.setId( "c1" );
		c1.setAge( 18 );
		c1.setName( "Bob" );
		em.persist( c1 );
		em.getTransaction().commit();
		em.close();

		// the actual assertion block
		em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
		Root<Customer> customerRoot = criteria.from( Customer.class );
		criteria.multiselect( customerRoot.get( Customer_.name ), customerRoot.get( Customer_.age ) );
		List<Tuple> results = em.createQuery( criteria ).getResultList();
		assertEquals( 1, results.size() );
		Tuple tuple = results.get( 0 );
		try {
			tuple.get( 99 );
			fail( "99 is invalid index" );
		}
		catch (IllegalArgumentException expected) {
		}

		try {
			tuple.get( 99, String.class );
			fail( "99 is invalid index" );
		}
		catch (IllegalArgumentException expected) {
		}

		tuple.get( 0, String.class );
		tuple.get( 1, Integer.class );

		try {
			tuple.get( 0, java.util.Date.class );
			fail( "Date is invalid type" );
		}
		catch (IllegalArgumentException expected) {
		}

		em.getTransaction().commit();
		em.close();

		em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Customer" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

}
