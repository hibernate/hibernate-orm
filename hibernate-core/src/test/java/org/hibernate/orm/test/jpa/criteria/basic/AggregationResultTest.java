/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.Product_;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class AggregationResultTest extends AbstractMetamodelSpecificTest {
	private CriteriaBuilder builder;

	@BeforeEach
	public void createTestData() {
		builder = entityManagerFactory().getCriteriaBuilder();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Product product = new Product();
		product.setId( "product1" );
		product.setPrice( 1.23d );
		product.setQuantity( 1000 );
		product.setPartNumber( ( (long) Integer.MAX_VALUE ) + 1 );
		product.setRating( 1.999f );
		product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
		product.setSomeBigDecimal( BigDecimal.valueOf( 987654.32 ) );
		em.persist( product );
		em.getTransaction().commit();
		em.close();
	}

	@AfterEach
	public void cleanUpTestData() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Product" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of Longs should return a Long
	 */
	@Test
	public void testSumOfLongs() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Long> criteria = builder.createQuery( Long.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.partNumber ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( Long.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of Integers should return an Integer; note that this is distinctly different than JPAQL
	 */
	@Test
	public void testSumOfIntegers() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.quantity ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( Integer.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of Doubles should return a Double
	 */
	@Test
	public void testSumOfDoubles() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Double> criteria = builder.createQuery( Double.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.price ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( Double.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of Floats should return a Float; note that this is distinctly different than JPAQL
	 */
	@Test
	public void testSumOfFloats() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Float> criteria = builder.createQuery( Float.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.rating ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( Float.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of BigInteger should return a BigInteger
	 */
	@Test
	public void testSumOfBigIntegers() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<BigInteger> criteria = builder.createQuery( BigInteger.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.someBigInteger ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( BigInteger.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Sum of BigDecimal should return a BigDecimal
	 */
	@Test
	public void testSumOfBigDecimals() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<BigDecimal> criteria = builder.createQuery( BigDecimal.class );
		Root<Product> productRoot = criteria.from( Product.class );
		criteria.select( builder.sum( productRoot.get( Product_.someBigDecimal ) ) );
		Object sumResult = em.createQuery( criteria ).getSingleResult();
		assertReturnType( BigDecimal.class, sumResult );
		em.getTransaction().commit();
		em.close();
	}

	private void assertReturnType(Class expectedType, Object value) {
		if ( value != null && ! expectedType.isInstance( value ) ) {
			fail(
					"Result value was not of expected type: expected [" + expectedType.getName()
							+ "] but found [" + value.getClass().getName() + "]"
			);
		}
	}
}
