/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.metamodel.Phone;
import org.hibernate.jpa.test.metamodel.Product;
import org.hibernate.jpa.test.metamodel.Product_;
import org.hibernate.orm.test.jpa.criteria.AbstractCriteriaTest;
import org.hibernate.query.Query;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Tests that various expressions operate as expected
 *
 * @author Steve Ebersole
 */
public class ExpressionsTest extends AbstractCriteriaTest {

	@BeforeEach
	void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Product product = new Product();
			product.setId( "product1" );
			product.setPrice( 1.23d );
			product.setQuantity( 2 );
			product.setPartNumber( (long)Integer.MAX_VALUE + 1 );
			product.setRating( 1.999f );
			product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
			product.setSomeBigDecimal( BigDecimal.valueOf( 987654.32 ) );
			em.persist( product );
		} );
	}

	@AfterEach
	void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.createQuery( "delete Product" ).executeUpdate() );
	}

	@Test
	void testEmptyConjunction(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
			criteria.from( Product.class );
			criteria.where( builder.and() );
			List<Product> result = em.createQuery( criteria ).getResultList();
			assertThat( result, hasSize( 1 ) );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-6876" )
	@RequiresDialect( H2Dialect.class )
	void testEmptyInList(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
			Root<Product> from = criteria.from( Product.class );
			criteria.where( from.get( Product_.PART_NUMBER ).in() ); // empty IN list
			List<Product> result = em.createQuery( criteria ).getResultList();
			assertThat( result, empty() );
		} );
	}

	@Test
	void testEmptyConjunctionIsTrue(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
			criteria.from( Product.class );
			criteria.where( builder.isTrue( builder.and() ) );
			List<Product> result = em.createQuery( criteria ).getResultList();
			assertThat( result, hasSize( 1 ) );
		} );
	}

	@Test
	void testEmptyConjunctionIsFalse(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
			criteria.from( Product.class );
			criteria.where( builder.isFalse( builder.and() ) );
			List<Product> result = em.createQuery( criteria ).getResultList();
			assertThat( result, empty() );
		} );
	}

	@Test
	void testEmptyDisjunction(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
			criteria.from( Product.class );
			criteria.where( builder.disjunction() );
			List<Product> result = em.createQuery( criteria ).getResultList();
			assertThat( result, empty() );
		} );
	}

	@Test
	void testEmptyDisjunctionIsTrue(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
			criteria.from( Product.class );
			criteria.where( builder.isTrue( builder.disjunction() ) );
			List<Product> result = em.createQuery( criteria ).getResultList();
			assertThat( result, empty() );
		} );
	}

	@Test
	void testEmptyDisjunctionIsFalse(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
			criteria.from( Product.class );
			criteria.where( builder.isFalse( builder.disjunction() ) );
			List<Product> result = em.createQuery( criteria ).getResultList();
			assertThat( result, hasSize( 1 ) );
		} );
	}

	@Test
	void testDiff(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
			criteria.from( Product.class );
			criteria.select( builder.diff( builder.literal( 5 ), builder.literal( 2 ) ) );
			Integer result = em.createQuery( criteria ).getSingleResult();
			assertThat( result, is( 3 ) );
		} );
	}

	@Test
	void testDiffWithQuotient(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Number> criteria = builder.createQuery( Number.class );
			criteria.from( Product.class );
			criteria.select(
					builder.quot(
							builder.diff(
									builder.literal( BigDecimal.valueOf( 2.0 ) ),
									builder.literal( BigDecimal.valueOf( 1.0 ) )
							),
							BigDecimal.valueOf( 2.0 )
					)
			);
			Number result = em.createQuery( criteria ).getSingleResult();
			assertThat( result.doubleValue(), closeTo( 0.5, 0.1 ) );
		} );
	}

	@Test
	void testSumWithQuotient(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Number> criteria = builder.createQuery( Number.class );
			criteria.from( Product.class );
			criteria.select(
					builder.quot(
							builder.sum(
									builder.literal( BigDecimal.valueOf( 0.0 ) ),
									builder.literal( BigDecimal.valueOf( 1.0 ) )
							),
							BigDecimal.valueOf( 2.0 )
					)
			);
			Number result = em.createQuery( criteria ).getSingleResult();
			assertThat( result.doubleValue(), closeTo( 0.5, 0.1 ) );
		} );
	}

	@Test
	void testParameterReuse(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaQuery<Product> criteria = em.getCriteriaBuilder().createQuery( Product.class );
			Root<Product> from = criteria.from( Product.class );
			ParameterExpression<String> param = em.getCriteriaBuilder().parameter( String.class );
			Predicate predicate = em.getCriteriaBuilder().equal( from.get( Product_.ID ), param );
			Predicate predicate2 = em.getCriteriaBuilder().equal( from.get( Product_.NAME ), param );
			criteria.where( em.getCriteriaBuilder().or( predicate, predicate2 ) );
			TypedQuery<Product> query = em.createQuery( criteria );
			assertThat( query.unwrap( Query.class ).getParameterMetadata().getParameterCount(), is( 1 ) );
			query.setParameter( param, "abc" ).getResultList();
		} );
	}

	@Test
	void testInExplicitTupleList(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaQuery<Product> criteria = em.getCriteriaBuilder().createQuery( Product.class );
			Root<Product> from = criteria.from( Product.class );
			criteria.where( from.get( Product_.PART_NUMBER ).in( Collections.singletonList( (long)Integer.MAX_VALUE + 1 ) ) );
			List<Product> result = em.createQuery( criteria ).getResultList();
			assertThat( result, hasSize( 1 ) );
		} );
	}

	@Test
	void testInExplicitTupleListVarargs(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaQuery<Product> criteria = em.getCriteriaBuilder().createQuery( Product.class );
			Root<Product> from = criteria.from( Product.class );
			criteria.where( from.get( Product_.PART_NUMBER ).in( (long)Integer.MAX_VALUE + 1 ) );
			List<Product> result = em.createQuery( criteria ).getResultList();
			assertThat( result, hasSize( 1 ) );
		} );
	}

	@Test
	void testInExpressionVarargs(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaQuery<Product> criteria = em.getCriteriaBuilder().createQuery( Product.class );
			Root<Product> from = criteria.from( Product.class );
			criteria.where( from.get( Product_.PART_NUMBER ).in( from.get( Product_.PART_NUMBER ) ) );
			List<Product> result = em.createQuery( criteria ).getResultList();
			assertThat( result, hasSize( 1 ) );
		} );
	}

	@Test
	void testJoinedElementCollectionValuesInTupleList(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaQuery<Phone> criteria = em.getCriteriaBuilder().createQuery( Phone.class );
			Root<Phone> from = criteria.from( Phone.class );
			criteria.where(
					from.join( "types" )
							.in( Collections.singletonList( Phone.Type.WORK ) )
			);
			em.createQuery( criteria ).getResultList();
		} );
	}

	@Test
	void testQuotientAndMultiply(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Number> criteria = builder.createQuery( Number.class );
			criteria.from( Product.class );
			criteria.select(
					builder.quot(
							builder.prod(
									builder.literal( BigDecimal.valueOf( 10.0 ) ),
									builder.literal( BigDecimal.valueOf( 5.0 ) )
							),
							builder.literal( BigDecimal.valueOf( 2.0 ) )
					)
			);
			Number result = em.createQuery( criteria ).getSingleResult();
			assertThat( result.doubleValue(), closeTo( 25.0d, 0.1d ) );

			criteria.select(
					builder.prod(
							builder.quot(
									builder.literal( BigDecimal.valueOf( 10.0 ) ),
									builder.literal( BigDecimal.valueOf( 5.0 ) )
							),
							builder.literal( BigDecimal.valueOf( 2.0 ) )
					)
			);
			result = em.createQuery( criteria ).getSingleResult();
			assertThat( result.doubleValue(), closeTo( 4.0d, 0.1d ) );

		} );
	}
}
