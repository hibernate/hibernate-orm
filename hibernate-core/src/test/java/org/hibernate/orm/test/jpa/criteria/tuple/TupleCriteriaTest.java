/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.tuple;

import java.util.Date;
import java.util.List;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.metamodel.Address;
import org.hibernate.orm.test.jpa.metamodel.Alias;
import org.hibernate.orm.test.jpa.metamodel.Country;
import org.hibernate.orm.test.jpa.metamodel.CreditCard;
import org.hibernate.orm.test.jpa.metamodel.Customer;
import org.hibernate.orm.test.jpa.metamodel.Customer_;

import org.hibernate.orm.test.jpa.metamodel.Info;
import org.hibernate.orm.test.jpa.metamodel.LineItem;
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.ShelfLife;
import org.hibernate.orm.test.jpa.metamodel.Spouse;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Address.class, Alias.class, Country.class, CreditCard.class, Customer.class,
		Info.class, LineItem.class, Order.class, Phone.class, Product.class,
		ShelfLife.class, Spouse.class
})
public class TupleCriteriaTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testArray(EntityManagerFactoryScope scope) {
		final Customer c1 = new Customer();
		scope.inTransaction( entityManager -> {
			c1.setId( "c1" );
			c1.setAge( 18 );
			c1.setName( "Bob" );
			entityManager.persist( c1 );
		} );
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Object[]> q = cb.createQuery( Object[].class );
			Root<Customer> c = q.from( Customer.class );
			q.select( cb.array( c.get( Customer_.name ), c.get( Customer_.age ) ) );
			List<Object[]> result = entityManager.createQuery( q ).getResultList();
			assertEquals( 1, result.size() );
			assertEquals( c1.getName(), result.get( 0 )[0] );
			assertEquals( c1.getAge(), result.get( 0 )[1] );
		} );
	}

	@Test
	public void testTuple(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer c1 = new Customer();
			c1.setId( "c1" );
			c1.setAge( 18 );
			c1.setName( "Bob" );
			entityManager.persist( c1 );
		} );
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
			Root<Customer> customerRoot = criteria.from( Customer.class );
			Path<String> namePath = customerRoot.get( Customer_.name );
			Path<Integer> agePath = customerRoot.get( Customer_.age );
			agePath.alias( "age" );
			criteria.multiselect( namePath, agePath );
			List<Tuple> results = entityManager.createQuery( criteria ).getResultList();
			assertEquals( 1, results.size() );
			Object resultElement = results.get( 0 );
			assertTrue( Tuple.class.isInstance( resultElement ), "Check  result 'row' as Tuple" );
			Tuple resultElementTuple = (Tuple) resultElement;
			Object[] tupleArray = resultElementTuple.toArray();
			assertEquals( 2, tupleArray.length );
			assertEquals( tupleArray[0], resultElementTuple.get( 0 ) );
			assertEquals( resultElementTuple.get( namePath ), resultElementTuple.get( 0 ) );
			assertEquals( tupleArray[1], resultElementTuple.get( 1 ) );
			assertEquals( resultElementTuple.get( agePath ), resultElementTuple.get( 1 ) );
			assertEquals( resultElementTuple.get( agePath ), resultElementTuple.get( "age" ) );
		} );
	}

	@Test
	public void testIllegalArgumentExceptionBuildingTupleWithSameAliases(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
			Root<Customer> customerRoot = criteria.from( Customer.class );
			Path<String> namePath = customerRoot.get( Customer_.name );
			namePath.alias( "age" );
			Path<Integer> agePath = customerRoot.get( Customer_.age );
			agePath.alias( "age" );
			assertThrows(
					IllegalArgumentException.class,
					() -> criteria.multiselect( namePath, agePath ),
					"Attempt to define multi-select with same aliases should have thrown IllegalArgumentException"
			);
		} );
	}

	@Test
	public void testVariousTupleAccessMethods(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer c1 = new Customer();
			c1.setId( "c1" );
			c1.setAge( 18 );
			c1.setName( "Bob" );
			entityManager.persist( c1 );
		} );

		scope.inTransaction( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
			Root<Customer> customerRoot = criteria.from( Customer.class );
			Path<String> namePath = customerRoot.get( Customer_.name );
			namePath.alias( "NAME" );
			Path<Integer> agePath = customerRoot.get( Customer_.age );
			agePath.alias( "AGE" );
			criteria.multiselect( namePath, agePath );

			List<Tuple> results = entityManager.createQuery( criteria ).getResultList();
			Tuple tuple = results.get( 0 );
			assertNotNull( tuple );
			assertNotNull( tuple.get( "NAME" ) );
			assertNotNull( tuple.get( "NAME", String.class ) );
			assertThrows(
					IllegalArgumentException.class,
					() -> tuple.get( "NAME", Date.class ),
					"Accessing Customer#name tuple as Date should have thrown exception"
			);
		} );
	}

	@Test
	public void testIllegalArgumentExceptionBuildingSelectArrayWithSameAliases(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery criteria = builder.createQuery();
			Root<Customer> customerRoot = criteria.from( Customer.class );
			Path<String> namePath = customerRoot.get( Customer_.name );
			Path<Integer> agePath = customerRoot.get( Customer_.age );
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						CompoundSelection<Object[]> c = builder.array( namePath.alias( "SAME" ), agePath.alias( "SAME" ) );
						criteria.select( c );
					},
					"Attempt to define multi-select with same aliases should have thrown IllegalArgumentException"
			);
		} );
	}

	@Test
	public void testInvalidTupleIndexAccess(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer c1 = new Customer();
			c1.setId( "c1" );
			c1.setAge( 18 );
			c1.setName( "Bob" );
			entityManager.persist( c1 );
		} );
		// the actual assertion block
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
			Root<Customer> customerRoot = criteria.from( Customer.class );
			criteria.multiselect( customerRoot.get( Customer_.name ), customerRoot.get( Customer_.age ) );
			List<Tuple> results = entityManager.createQuery( criteria ).getResultList();
			assertEquals( 1, results.size() );
			Tuple tuple = results.get( 0 );
			assertThrows(
					IllegalArgumentException.class,
					() -> tuple.get( 99 ),
					"99 is invalid index"
			);

			assertThrows(
					IllegalArgumentException.class,
					() -> tuple.get( 99, String.class ),
					"99 is invalid index"
			);

			tuple.get( 0, String.class );
			tuple.get( 1, Integer.class );

			assertThrows(
					IllegalArgumentException.class,
					() -> tuple.get( 0, Date.class ),
					"Date is invalid type"
			);
		} );
	}

}
