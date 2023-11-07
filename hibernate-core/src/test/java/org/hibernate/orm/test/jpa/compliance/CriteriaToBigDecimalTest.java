/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.compliance;

import java.math.BigDecimal;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = CriteriaToBigDecimalTest.Person.class
)
public class CriteriaToBigDecimalTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager ->
						entityManager.persist( new Person( 1, "Luigi", 20 ) )
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager ->
						entityManager.createQuery( "delete from Person" ).executeUpdate()
		);
	}

	@Test
	public void testToBigDecimal(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<BigDecimal> query = criteriaBuilder.createQuery( BigDecimal.class );

					final Root<Person> person = query.from( Person.class );

					final Path<BigDecimal> ageAttribute = person.get( "age" );

					final Expression<BigDecimal> prod = criteriaBuilder.prod(
							ageAttribute,
							new BigDecimal( "5.5" )
					);

					query.select( criteriaBuilder.toBigDecimal( prod ) );

					query.where( criteriaBuilder.equal( ageAttribute, 20 ) );

					BigDecimal result = entityManager.createQuery( query ).getSingleResult();

					assertEquals( new BigDecimal( "110.0" ).stripTrailingZeros(), result.stripTrailingZeros() );
				}
		);
	}

	@Test
	public void testToBigDecimal2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<BigDecimal> query = criteriaBuilder.createQuery( BigDecimal.class );

					final Root<Person> person = query.from( Person.class );

					final Path<BigDecimal> ageAttribute = person.get( "age" );

					final Expression<BigDecimal> prod = criteriaBuilder.prod(
							ageAttribute,
							new BigDecimal( "5.501" )
					);

					query.select( criteriaBuilder.toBigDecimal( prod ) );

					query.where( criteriaBuilder.equal( ageAttribute, 20 ) );

					BigDecimal result = entityManager.createQuery( query ).getSingleResult();

					assertEquals( new BigDecimal( "110.02" ).stripTrailingZeros(), result.stripTrailingZeros() );
				}
		);
	}

	@Test
	public void testToBigDecimal3(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<Object> query = criteriaBuilder.createQuery();
					final Root<Person> person = query.from( Person.class );
					query.select( criteriaBuilder.sum( person.get( "ageAsBigDecimal" ), 1 ) );

					BigDecimal result = (BigDecimal) entityManager.createQuery( query ).getSingleResult();

					assertEquals( new BigDecimal( "21" ), result.stripTrailingZeros() );
				}
		);
	}

	@Test
	public void testToBigDecimal4(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					BigDecimal result = (BigDecimal) entityManager.createQuery( "select p.ageAsBigDecimal + :val from Person p" )
							.setParameter( "val", 1 )
							.getSingleResult();

					assertEquals( new BigDecimal( "21" ), result.stripTrailingZeros() );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;

		private String name;

		private Integer age;
		private BigDecimal ageAsBigDecimal;

		Person() {
		}

		public Person(Integer id, String name, Integer age) {
			this.id = id;
			this.name = name;
			this.age = age;
			this.ageAsBigDecimal = new BigDecimal( age );
		}
	}
}