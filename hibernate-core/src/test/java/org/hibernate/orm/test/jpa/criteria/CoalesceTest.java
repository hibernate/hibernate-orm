/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Will Dazy
 */
@Jpa( annotatedClasses = {
		CoalesceTest.HHH15291Entity.class,
		CoalesceTest.ComponentEntity.class,
		CoalesceTest.ComponentA.class,
} )
@JiraKey( value = "HHH-15291")
public class CoalesceTest {
	@Test
	public void hhh15291JPQL1Test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					TypedQuery<HHH15291Entity> query = entityManager.createQuery(
							"" + "SELECT t FROM HHH15291Entity t "
									+ "WHERE t.itemString2 = " + "COALESCE (t.itemString1, ?1)",
							HHH15291Entity.class
					);
					query.setParameter( 1, "Sample" );
					query.getResultList();
				}
		);
	}

	@Test
	public void hhh15291JPQL2Test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					TypedQuery<String> query2 = entityManager.createQuery(
							"" + "SELECT COALESCE (t.itemString2, ?1) FROM HHH15291Entity t ORDER BY t.itemInteger1 ASC",
							String.class
					);
					query2.setParameter( 1, "Sample" );
				}
		);
	}

	@Test
	public void hhh15291Criteria1Test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<HHH15291Entity> cquery = cb.createQuery( HHH15291Entity.class );
					Root<HHH15291Entity> root = cquery.from( HHH15291Entity.class );
					cquery.select( root );

					ParameterExpression<String> checkParam1 = cb.parameter( String.class );
					Expression<String> coalesce = cb.coalesce( root.get( "itemString1" ), checkParam1 );
					cquery.where( cb.equal( root.get( "itemString2" ), coalesce ) );

					TypedQuery<HHH15291Entity> query = entityManager.createQuery( cquery );
					query.setParameter( checkParam1, "Sample" );
					query.getResultList();
				}
		);
	}

	@Test
	public void hhh15291Criteria2Test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<HHH15291Entity> cquery = cb.createQuery( HHH15291Entity.class );
					Root<HHH15291Entity> root = cquery.from( HHH15291Entity.class );
					cquery.select( root );

					ParameterExpression<String> checkParam1 = cb.parameter( String.class );
					Expression<String> coalesce = cb.coalesce( root.get( "itemString1" ), checkParam1 );
					cquery.where( cb.equal( root.get( "itemString2" ), coalesce ) );

					TypedQuery<HHH15291Entity> query = entityManager.createQuery( cquery );
					query.setParameter( checkParam1, "Sample" );
					query.getResultList();
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18321" )
	public void testCoalesceInBinaryArithmetic(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Tuple> cquery = cb.createTupleQuery();
			final Root<ComponentEntity> root = cquery.from( ComponentEntity.class );

			cquery.select( cb.tuple(
					root.get( "id" ),
					cb.diff(
							cb.coalesce( root.get( "componentA" ).get( "income" ), BigDecimal.ZERO ),
							cb.coalesce( root.get( "componentA" ).get( "expense" ), BigDecimal.ZERO )
					)
			) );

			final List<Tuple> resultList = entityManager.createQuery( cquery ).getResultList();
			assertThat( resultList ).hasSize( 2 );
			for ( Tuple result : resultList ) {
				final Long id = result.get( 0, Long.class );
				assertThat( result.get( 1, BigDecimal.class ).intValue() ).isEqualTo( id == 1L ? 0 : 1 );
			}
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18321" )
	public void testCoalesceInBinaryArithmeticParam(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Tuple> cquery = cb.createTupleQuery();
			final Root<ComponentEntity> root = cquery.from( ComponentEntity.class );

			final ParameterExpression<BigDecimal> defaultValue = cb.parameter( BigDecimal.class, "default-value" );

			cquery.select( cb.tuple(
					root.get( "id" ),
					cb.diff(
							defaultValue,
							cb.coalesce( root.get( "componentA" ).get( "expense" ), defaultValue )
					)
			) );

			final List<Tuple> resultList = entityManager.createQuery( cquery )
					.setParameter( "default-value", BigDecimal.ZERO ).getResultList();
			assertThat( resultList ).hasSize( 2 );
			for ( Tuple result : resultList ) {
				final Long id = result.get( 0, Long.class );
				assertThat( result.get( 1, BigDecimal.class ).intValue() ).isEqualTo( id == 1L ? -1 : 0 );
			}
		} );
	}

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new ComponentEntity( 1L, new ComponentA( BigDecimal.ONE, BigDecimal.ONE ) ) );
			entityManager.persist( new ComponentEntity( 2L, new ComponentA( BigDecimal.ONE, null ) ) );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.createQuery( "delete from ComponentEntity" ).executeUpdate() );
	}

	@Entity(name = "HHH15291Entity")
	public static class HHH15291Entity {
		@Id
		@Column(name = "KEY_CHAR")
		private String KeyString;

		@Column(name = "ITEM_STRING1")
		private String itemString1;

		@Column(name = "ITEM_STRING2")
		private String itemString2;

		@Column(name = "ITEM_STRING3")
		private String itemString3;

		@Column(name = "ITEM_STRING4")
		private String itemString4;

		@Column(name = "ITEM_INTEGER1")
		private Integer itemInteger1;
	}

	@Entity( name = "ComponentEntity" )
	static class ComponentEntity {
		@Id
		private Long id;

		@Embedded
		private ComponentA componentA;

		public ComponentEntity() {
		}

		public ComponentEntity(Long id, ComponentA componentA) {
			this.id = id;
			this.componentA = componentA;
		}
	}

	@Embeddable
	static class ComponentA {
		private BigDecimal income;
		private BigDecimal expense;

		public ComponentA() {
		}

		public ComponentA(BigDecimal income, BigDecimal expense) {
			this.income = income;
			this.expense = expense;
		}
	}
}
