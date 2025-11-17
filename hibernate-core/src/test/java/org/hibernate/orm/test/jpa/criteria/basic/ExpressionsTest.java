/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.orm.test.jpa.metamodel.Address;
import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.Product_;
import org.hibernate.query.Query;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that various expressions operate as expected
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {Product.class, Phone.class, Address.class})
public class ExpressionsTest {
	private CriteriaBuilder builder;

	@BeforeEach
	public void prepareTestData(EntityManagerFactoryScope scope) {
		builder = scope.getEntityManagerFactory().getCriteriaBuilder();

		scope.inTransaction( entityManager -> {
					Product product = new Product();
					product.setId( "product1" );
					product.setPrice( 1.23d );
					product.setQuantity( 2 );
					product.setPartNumber( ( (long) Integer.MAX_VALUE ) + 1 );
					product.setRating( 1.999f );
					product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
					product.setSomeBigDecimal( BigDecimal.valueOf( 987654.32 ) );
					entityManager.persist( product );
				}
		);
	}

	@AfterEach
	public void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testEmptyConjunction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.and() );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15452")
	public void testGetConjunctionExpressionsAndAddPredicate(EntityManagerFactoryScope scope){
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery(Product.class);
					Root<Product> root = criteria.from(Product.class);

					Predicate conjunction = builder.conjunction();
					Predicate expr = builder.equal(root.get("id"), "NON existing id");
					// Modifications to the list do not affect the query
					List<Expression<Boolean>> expressions = conjunction.getExpressions();
					expressions.add( expr);

					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-6876")
	public void testEmptyInList(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> from = criteria.from( Product.class );
					criteria.where( from.get( Product_.partNumber ).in() ); // empty IN list
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 0, result.size() );
				}
		);
	}

	@Test
	public void testEmptyConjunctionIsTrue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.isTrue( builder.and() ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testEmptyConjunctionIsFalse(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.isFalse( builder.and() ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 0, result.size() );
				}
		);
	}

	@Test
	public void testEmptyDisjunction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.disjunction() );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 0, result.size() );
				}
		);
	}

	@Test
	public void testEmptyDisjunctionIsTrue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.isTrue( builder.disjunction() ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 0, result.size() );
				}
		);
	}

	@Test
	public void testEmptyDisjunctionIsFalse(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.isFalse( builder.disjunction() ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testDiff(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
					criteria.from( Product.class );
					criteria.select( builder.diff( builder.literal( 5 ), builder.literal( 2 ) ) );
					Integer result = entityManager.createQuery( criteria ).getSingleResult();
					assertEquals( Integer.valueOf( 3 ), result );
				}
		);
	}

	@Test
	public void testDiffWithQuotient(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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
					Number result = entityManager.createQuery( criteria ).getSingleResult();
					assertEquals( 0.5d, result.doubleValue(), 0.1d );
				}
		);
	}

	@Test
	public void testSumWithQuotient(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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
					Number result = entityManager.createQuery( criteria ).getSingleResult();
					assertEquals( 0.5d, result.doubleValue(), 0.1d );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17223" )
	public void testSumWithCoalesce(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					final CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
					final Root<Product> root = criteria.from( Product.class );
					criteria.select(
							builder.sum(
									builder.coalesce( root.get( "quantity" ), builder.literal( 5 ) )
							)
					).groupBy( root.get( "id" ) );
					final Integer result = entityManager.createQuery( criteria ).getSingleResult();
					assertEquals( 2, result );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17260" )
	public void testSumWithSubqueryPath(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					final HibernateCriteriaBuilder cb =  entityManager.unwrap( Session.class ).getCriteriaBuilder();
					final JpaCriteriaQuery<Integer> criteria = cb.createQuery( Integer.class );
					final JpaSubQuery<Tuple> subquery = criteria.subquery( Tuple.class );
					final Root<Product> product = subquery.from( Product.class );
					subquery.multiselect(
							product.get( "id" ).alias( "id" ),
							product.get( "quantity" ).alias( "quantity" )
					);
					final JpaDerivedRoot<Tuple> root = criteria.from( subquery );
					criteria.select( cb.sum( root.get( "quantity" ) ) );
					final Integer result = entityManager.createQuery( criteria ).getSingleResult();
					assertEquals( 2, result );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "does not support extract(epoch)")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "datediff overflow limits")
	@SkipForDialect(dialectClass = GaussDBDialect.class, reason = "type:resolved.date multi overflows")
	public void testDateTimeOperations(EntityManagerFactoryScope scope) {
		HibernateCriteriaBuilder builder = (HibernateCriteriaBuilder) this.builder;
		scope.inTransaction( entityManager -> {
					CriteriaQuery<LocalDate> criteria = builder.createQuery(LocalDate.class);
					criteria.select( builder.addDuration( builder.localDate(),
							builder.duration(2, TemporalUnit.YEAR) ) );
					entityManager.createQuery(criteria).getSingleResult();
				}
		);
		scope.inTransaction( entityManager -> {
					CriteriaQuery<LocalDate> criteria = builder.createQuery(LocalDate.class);
					criteria.select( builder.addDuration(
							// had to call literal() here because parameter-based binding caused error from
							// database since it couldn't tell what sort of dateadd() function was being called
							builder.literal( LocalDate.of(2000,1, 1) ),
							builder.duration(2, TemporalUnit.YEAR) ) );
					assertEquals( LocalDate.of(2002,1, 1),
							entityManager.createQuery(criteria).getSingleResult() );
				}
		);
		//SQL Server and others don't like this
//		doInJPA(
//				this::entityManagerFactory,
//				entityManager -> {
//					CriteriaQuery<LocalDate> criteria = builder.createQuery(LocalDate.class);
//					criteria.select( builder.after( builder.localDate(), Duration.ofDays(2*365) ) );
//					entityManager.createQuery(criteria).getSingleResult();
//				}
//		);
		scope.inTransaction( entityManager -> {
					CriteriaQuery<LocalDateTime> criteria = builder.createQuery(LocalDateTime.class);
					criteria.select( builder.subtractDuration( builder.localDateTime(), Duration.ofMinutes(30) ) );
					entityManager.createQuery(criteria).getSingleResult();
				}
		);
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Duration> criteria = builder.createQuery(Duration.class);
					criteria.select( builder.durationScaled( 5, builder.duration(2, TemporalUnit.HOUR ) ) );
					assertEquals( Duration.ofHours(10), entityManager.createQuery(criteria).getSingleResult() );
				}
		);
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Duration> criteria = builder.createQuery(Duration.class);
					criteria.select( builder.durationSum( builder.duration(30, TemporalUnit.MINUTE ),
							builder.duration(2, TemporalUnit.HOUR) ) );
					assertEquals( Duration.ofMinutes(150), entityManager.createQuery(criteria).getSingleResult() );
				}
		);
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
					criteria.select( builder.durationByUnit( TemporalUnit.SECOND,
							builder.durationSum( builder.duration(30, TemporalUnit.MINUTE),
								builder.duration(2, TemporalUnit.HOUR) ) ) );
					assertEquals( 150*60L, entityManager.createQuery(criteria).getSingleResult() );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "numeric overflows")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "does not support extract(epoch)")
	void testDurationBetween(EntityManagerFactoryScope scope) {
		HibernateCriteriaBuilder builder = (HibernateCriteriaBuilder) this.builder;
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Duration> criteria = builder.createQuery(Duration.class);
					criteria.select( builder.durationBetween( builder.localDate(),
							LocalDate.of(2000,1, 1) ) );
					entityManager.createQuery(criteria).getSingleResult();
				}
		);
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Duration> criteria = builder.createQuery(Duration.class);
					criteria.select( builder.durationBetween( builder.localDate(),
							builder.subtractDuration( builder.localDate(),
									builder.duration(2, TemporalUnit.DAY) ) ) );
					assertEquals( Duration.ofDays(2), entityManager.createQuery(criteria).getSingleResult() );
				}
		);
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Duration> criteria = builder.createQuery(Duration.class);
					criteria.select( builder.durationBetween( builder.localDateTime(),
							builder.subtractDuration( builder.localDateTime(),
									builder.duration(20, TemporalUnit.HOUR) ) ) );
					assertEquals( Duration.ofHours(20), entityManager.createQuery(criteria).getSingleResult() );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "By default, unless some kind of context enables inference," +
			"a numeric/decimal parameter has the type DECIMAL(31,31) which might cause an overflow on certain arithmetics." +
			"Fixing this would require a custom SqmToSqlAstConverter that creates a special JdbcParameter " +
			"that is always rendered as literal. Since numeric literal + parameter arithmetic is rare, we skip this for now.")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "Same reason as for Derby")
	public void testQuotientAndMultiply(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Number> criteria = builder.createQuery( Number.class );
					criteria.from( Product.class );
					criteria.select(
							builder.quot(
									builder.prod(
											builder.literal( BigDecimal.valueOf( 10.0 ) ),
											builder.literal( BigDecimal.valueOf( 5.0 ) )
									),
									BigDecimal.valueOf( 2.0 )
							)
					);
					Number result = entityManager.createQuery( criteria ).getSingleResult();
					assertEquals( 25.0d, result.doubleValue(), 0.1d );

					criteria.select(
							builder.prod(
									builder.quot(
											builder.literal( BigDecimal.valueOf( 10.0 ) ),
											builder.literal( BigDecimal.valueOf( 5.0 ) )
									),
									BigDecimal.valueOf( 2.0 )
							)
					);
					result = entityManager.createQuery( criteria ).getSingleResult();
					assertEquals( 4.0d, result.doubleValue(), 0.1d );
				}
		);
	}

	@Test
	public void testParameterReuse(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> from = criteria.from( Product.class );
					ParameterExpression<String> param = builder.parameter( String.class );
					Predicate predicate = builder.equal( from.get( Product_.id ), param );
					Predicate predicate2 = builder.equal( from.get( Product_.name ), param );
					criteria.where( builder.or( predicate, predicate2 ) );
					assertEquals( 1, criteria.getParameters().size() );
					TypedQuery<Product> query = entityManager.createQuery( criteria );
					int hqlParamCount = countGeneratedParameters( query.unwrap( Query.class ) );
					assertEquals( 1, hqlParamCount );
					query.setParameter( param, "abc" ).getResultList();
				}
		);
	}

	private int countGeneratedParameters(Query<?> query) {
		return query.getParameterMetadata().getParameterCount();
	}

	@Test
	public void testInExplicitTupleList(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> from = criteria.from( Product.class );
					criteria.where( from.get( Product_.partNumber )
											.in( Collections.singletonList( ( (long) Integer.MAX_VALUE ) + 1 ) ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testInExplicitTupleListVarargs(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> from = criteria.from( Product.class );
					criteria.where( from.get( Product_.partNumber ).in( ( (long) Integer.MAX_VALUE ) + 1 ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testInExpressionVarargs(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> from = criteria.from( Product.class );
					criteria.where( from.get( Product_.partNumber ).in( from.get( Product_.partNumber ) ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testJoinedElementCollectionValuesInTupleList(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaQuery<Phone> criteria = builder.createQuery( Phone.class );
					Root<Phone> from = criteria.from( Phone.class );
					criteria.where(
							from.join( "types" )
									.in( Collections.singletonList( Phone.Type.WORK ) )
					);
					entityManager.createQuery( criteria ).getResultList();
				}
		);
	}
}
