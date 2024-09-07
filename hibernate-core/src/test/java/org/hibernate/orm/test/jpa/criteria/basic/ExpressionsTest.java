/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.orm.test.jpa.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.Product_;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.common.TemporalUnit;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that various expressions operate as expected
 *
 * @author Steve Ebersole
 */
public class ExpressionsTest extends AbstractMetamodelSpecificTest {
	private CriteriaBuilder builder;

	@BeforeEach
	public void prepareTestData() {
		builder = entityManagerFactory().getCriteriaBuilder();

		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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
	public void cleanupTestData() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					entityManager.createQuery( "delete from Product" ).executeUpdate();
				}
		);
	}

	@Test
	public void testEmptyConjunction() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.and() );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-15452")
	public void testGetConjunctionExpressionsAndAddPredicate(){
		inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Product> criteria = builder.createQuery(Product.class);
					Root<Product> rootClaseGrid = criteria.from(Product.class);

					Predicate conjuncion = builder.conjunction();
					Predicate expr = builder.equal(rootClaseGrid.get("id"), "NON existing id");
					// Modifications to the list do not affect the query
					List<Expression<Boolean>> expressions = conjuncion.getExpressions();
					expressions.add( expr);

					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6876")
	public void testEmptyInList() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> from = criteria.from( Product.class );
					criteria.where( from.get( Product_.partNumber ).in() ); // empty IN list
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 0, result.size() );
				}
		);
	}

	@Test
	public void testEmptyConjunctionIsTrue() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.isTrue( builder.and() ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testEmptyConjunctionIsFalse() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.isFalse( builder.and() ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 0, result.size() );
				}
		);
	}

	@Test
	public void testEmptyDisjunction() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.disjunction() );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 0, result.size() );
				}
		);
	}

	@Test
	public void testEmptyDisjunctionIsTrue() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.isTrue( builder.disjunction() ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 0, result.size() );
				}
		);
	}

	@Test
	public void testEmptyDisjunctionIsFalse() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					criteria.from( Product.class );
					criteria.where( builder.isFalse( builder.disjunction() ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testDiff() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
					criteria.from( Product.class );
					criteria.select( builder.diff( builder.literal( 5 ), builder.literal( 2 ) ) );
					Integer result = entityManager.createQuery( criteria ).getSingleResult();
					assertEquals( Integer.valueOf( 3 ), result );
				}
		);
	}

	@Test
	public void testDiffWithQuotient() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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
	public void testSumWithQuotient() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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
	public void testSumWithCoalesce() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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
	public void testSumWithSubqueryPath() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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

	@Test @SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "numeric overflows")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "does not support extract(epoch)")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "datediff overflow limits")
	public void testDateTimeOperations() {
		HibernateCriteriaBuilder builder = (HibernateCriteriaBuilder) this.builder;
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<LocalDate> criteria = builder.createQuery(LocalDate.class);
					criteria.select( builder.addDuration( builder.localDate(),
							builder.duration(2, TemporalUnit.YEAR) ) );
					entityManager.createQuery(criteria).getSingleResult();
				}
		);
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<LocalDateTime> criteria = builder.createQuery(LocalDateTime.class);
					criteria.select( builder.subtractDuration( builder.localDateTime(), Duration.ofMinutes(30) ) );
					entityManager.createQuery(criteria).getSingleResult();
				}
		);
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Duration> criteria = builder.createQuery(Duration.class);
					criteria.select( builder.durationScaled( 5, builder.duration(2, TemporalUnit.HOUR ) ) );
					assertEquals( Duration.ofHours(10), entityManager.createQuery(criteria).getSingleResult() );
				}
		);
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Duration> criteria = builder.createQuery(Duration.class);
					criteria.select( builder.durationSum( builder.duration(30, TemporalUnit.MINUTE ),
							builder.duration(2, TemporalUnit.HOUR) ) );
					assertEquals( Duration.ofMinutes(150), entityManager.createQuery(criteria).getSingleResult() );
				}
		);
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
					criteria.select( builder.durationByUnit( TemporalUnit.SECOND,
							builder.durationSum( builder.duration(30, TemporalUnit.MINUTE),
								builder.duration(2, TemporalUnit.HOUR) ) ) );
					assertEquals( 150*60L, entityManager.createQuery(criteria).getSingleResult() );
				}
		);
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Duration> criteria = builder.createQuery(Duration.class);
					criteria.select( builder.durationBetween( builder.localDate(),
							LocalDate.of(2000,1, 1) ) );
					entityManager.createQuery(criteria).getSingleResult();
				}
		);
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Duration> criteria = builder.createQuery(Duration.class);
					criteria.select( builder.durationBetween( builder.localDate(),
							builder.subtractDuration( builder.localDate(),
									builder.duration(2, TemporalUnit.DAY) ) ) );
					assertEquals( Duration.ofDays(2), entityManager.createQuery(criteria).getSingleResult() );
				}
		);
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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
	public void testQuotientAndMultiply() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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
	public void testParameterReuse() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Product> criteria = entityManager.getCriteriaBuilder().createQuery( Product.class );
					Root<Product> from = criteria.from( Product.class );
					ParameterExpression<String> param = entityManager.getCriteriaBuilder().parameter( String.class );
					Predicate predicate = entityManager.getCriteriaBuilder().equal( from.get( Product_.id ), param );
					Predicate predicate2 = entityManager.getCriteriaBuilder().equal( from.get( Product_.name ), param );
					criteria.where( entityManager.getCriteriaBuilder().or( predicate, predicate2 ) );
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
	public void testInExplicitTupleList() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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
	public void testInExplicitTupleListVarargs() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> from = criteria.from( Product.class );
					criteria.where( from.get( Product_.partNumber ).in( ( (long) Integer.MAX_VALUE ) + 1 ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testInExpressionVarargs() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> from = criteria.from( Product.class );
					criteria.where( from.get( Product_.partNumber ).in( from.get( Product_.partNumber ) ) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testJoinedElementCollectionValuesInTupleList() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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
