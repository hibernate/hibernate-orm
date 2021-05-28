/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.metamodel.Product;
import org.hibernate.jpa.test.metamodel.Product_;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Steve Ebersole
 */
@Jpa( annotatedClasses = Product.class )
public class AggregationResultTest {

	@BeforeEach
	void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Product product = new Product();
			product.setId( "product1" );
			product.setPrice( 1.23d );
			product.setQuantity( 1000 );
			product.setPartNumber( (long)Integer.MAX_VALUE + 1 );
			product.setRating( 1.999f );
			product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
			product.setSomeBigDecimal( BigDecimal.valueOf( 987654.32 ) );
			entityManager.persist( product );
		} );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.createQuery( "delete Product" ).executeUpdate() );
	}

	/**
	 * Sum of Longs should return a Long
	 */
	@Test
	void testSumOfLongs(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Long> criteria = builder.createQuery( Long.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.PART_NUMBER ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( Long.class, sumResult );
		} );
	}

	/**
	 * Sum of Integers should return a Long
	 */
	@Test
	void testSumOfIntegers(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Long> criteria = builder.createQuery( Long.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sumAsLong( productRoot.get( Product_.quantity ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( Long.class, sumResult );
		} );
	}

	/**
	 * Sum of Doubles should return a Double
	 */
	@Test
	void testSumOfDoubles(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Double> criteria = builder.createQuery( Double.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.PRICE ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( Double.class, sumResult );
		} );
	}

	/**
	 * Sum of Floats should return a Double
	 */
	@Test
	void testSumOfFloats(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Double> criteria = builder.createQuery( Double.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sumAsDouble( productRoot.get( Product_.RATING ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( Double.class, sumResult );
		} );
	}

	/**
	 * Sum of BigInteger should return a BigInteger
	 */
	@Test
	void testSumOfBigIntegers(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<BigInteger> criteria = builder.createQuery( BigInteger.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.SOME_BIG_INTEGER ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( BigInteger.class, sumResult );
		} );
	}

	/**
	 * Sum of BigDecimal should return a BigDecimal
	 */
	@Test
	void testSumOfBigDecimals(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<BigDecimal> criteria = builder.createQuery( BigDecimal.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.SOME_BIG_DECIMAL ) ) );
			Object sumResult = entityManager.createQuery( criteria ).getSingleResult();
			assertReturnType( BigDecimal.class, sumResult );
		} );
	}

	/**
	 * Sum of Integers should return an Integer; note that this is distinctly different than JPAQL
	 */
	@Test
	@FailureExpected(reason = "we switched to strict JPA Spec")
	void testSumOfIntegersReturningInteger(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.quantity ) ) );
			Object sumResult = em.createQuery( criteria ).getSingleResult();
			assertReturnType( Integer.class, sumResult );
		} );
	}

	/**
	 * Sum of Floats should return a Float; note that this is distinctly different than JPAQL
	 */
	@Test
	@FailureExpected(reason = "we switched to strict JPA Spec")
	void testSumOfFloatsReturningFloats(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Float> criteria = builder.createQuery( Float.class );
			Root<Product> productRoot = criteria.from( Product.class );
			criteria.select( builder.sum( productRoot.get( Product_.rating ) ) );
			Object sumResult = em.createQuery( criteria ).getSingleResult();
			assertReturnType( Float.class, sumResult );
		} );
	}

	private void assertReturnType(Class<?> expectedType, Object actualValue) {
		assertThat( actualValue, notNullValue() );
		String reason = String.format("Result actualValue was not of expected type: expected [%s] but found [%s]", expectedType.getName(), actualValue.getClass().getName() );
		assertThat( reason, actualValue, instanceOf( expectedType ) );
	}
}
