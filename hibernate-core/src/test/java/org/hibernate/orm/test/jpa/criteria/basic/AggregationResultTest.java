/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.Product_;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {Product.class})
public class AggregationResultTest {
	private CriteriaBuilder builder;

	@BeforeEach
	public void createTestData(EntityManagerFactoryScope scope) {
		builder = scope.getEntityManagerFactory().getCriteriaBuilder();

		scope.inTransaction( entityManager -> {
			Product product = new Product();
			product.setId( "product1" );
			product.setPrice( 1.23d );
			product.setQuantity( 1000 );
			product.setPartNumber( ((long) Integer.MAX_VALUE) + 1 );
			product.setRating( 1.999f );
			product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
			product.setSomeBigDecimal( BigDecimal.valueOf( 987654.32 ) );
			entityManager.persist( product );
		} );
	}

	@AfterEach
	public void cleanUpTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	/**
	 * Sum of Longs should return a Long
	 */
	@Test
	public void testSumOfLongs(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Long> criteria = builder.createQuery( Long.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.partNumber ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( Long.class, sumResult );
		} );
	}

	/**
	 * Sum of Integers should return an Integer; note that this is distinctly different from JPAQL
	 */
	@Test
	public void testSumOfIntegers(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.quantity ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( Integer.class, sumResult );
		} );
	}

	/**
	 * Sum of Doubles should return a Double
	 */
	@Test
	public void testSumOfDoubles(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Double> criteria = builder.createQuery( Double.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.price ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( Double.class, sumResult );
		} );
	}

	/**
	 * Sum of Floats should return a Float; note that this is distinctly different from JPAQL
	 */
	@Test
	public void testSumOfFloats(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Float> criteria = builder.createQuery( Float.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.rating ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( Float.class, sumResult );
		} );
	}

	/**
	 * Sum of BigInteger should return a BigInteger
	 */
	@Test
	public void testSumOfBigIntegers(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<BigInteger> criteria = builder.createQuery( BigInteger.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.someBigInteger ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( BigInteger.class, sumResult );
		} );
	}

	/**
	 * Sum of BigDecimal should return a BigDecimal
	 */
	@Test
	public void testSumOfBigDecimals(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<BigDecimal> criteria = builder.createQuery( BigDecimal.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.someBigDecimal ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( BigDecimal.class, sumResult );
		} );
	}

	private void assertReturnType(Class<?> expectedType, Object value) {
		if ( value != null && ! expectedType.isInstance( value ) ) {
			fail(
					"Result value was not of expected type: expected [" + expectedType.getName()
							+ "] but found [" + value.getClass().getName() + "]"
			);
		}
	}
}
