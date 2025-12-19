/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.math.BigDecimal;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

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
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testToBigDecimal(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final var criteriaBuilder = entityManager.getCriteriaBuilder();
					final var query = criteriaBuilder.createQuery( BigDecimal.class );
					final var person = query.from( Person.class );
					final var ageAttribute = person.get( CriteriaToBigDecimalTest_.Person_.ageAsBigDecimal );
					final var prod = criteriaBuilder.prod( ageAttribute, new BigDecimal( "5.5" ) );
					query.select( criteriaBuilder.toBigDecimal( prod ) );
					query.where( criteriaBuilder.equal( ageAttribute, 20 ) );
					BigDecimal result = entityManager.createQuery( query ).getSingleResult();
					assertEquals( new BigDecimal( "110.0" ).stripTrailingZeros(), result.stripTrailingZeros() );
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = InformixDialect.class,
			reason = "Informix has needs a CAST here") // TODO: add the CAST automatically
	public void testToBigDecimal1(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final var criteriaBuilder = entityManager.getCriteriaBuilder();
					final var query = criteriaBuilder.createQuery( BigDecimal.class );
					final var person = query.from( Person.class );
					final var ageAttribute = person.get( CriteriaToBigDecimalTest_.Person_.age );
					final var prod = criteriaBuilder.prod( ageAttribute, new BigDecimal( "5.5" ) );
					query.select( criteriaBuilder.toBigDecimal( prod ) );
					query.where( criteriaBuilder.equal( ageAttribute, 20 ) );
					BigDecimal result = entityManager.createQuery( query ).getSingleResult();
					assertEquals( new BigDecimal( "110.0" ).stripTrailingZeros(), result.stripTrailingZeros() );
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = InformixDialect.class,
			reason = "Informix has needs a CAST here") // TODO: add the CAST automatically
	public void testToBigDecimal2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final var criteriaBuilder = entityManager.getCriteriaBuilder();
					final var query = criteriaBuilder.createQuery( BigDecimal.class );
					final var person = query.from( Person.class );
					final var ageAttribute = person.get( CriteriaToBigDecimalTest_.Person_.age );
					final var prod = criteriaBuilder.prod( ageAttribute, new BigDecimal( "5.501" ) );
					query.select( criteriaBuilder.toBigDecimal( prod ) );
					query.where( criteriaBuilder.equal( ageAttribute, 20 ) );
					BigDecimal result = entityManager.createQuery( query ).getSingleResult();
					assertEquals( new BigDecimal( "110.02" ).stripTrailingZeros(), result.stripTrailingZeros() );
				}
		);
	}

	@Test
	public void testCastToBigDecimal1(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final var criteriaBuilder = entityManager.getCriteriaBuilder();
					final var query = criteriaBuilder.createQuery( BigDecimal.class );
					final var person = query.from( Person.class );
					final var ageAttribute = person.get( CriteriaToBigDecimalTest_.Person_.age ).cast( BigDecimal.class );
					final var prod = criteriaBuilder.prod( ageAttribute, new BigDecimal( "5.5" ) );
					query.select( criteriaBuilder.toBigDecimal( prod ) );
					query.where( criteriaBuilder.equal( ageAttribute, 20 ) );
					BigDecimal result = entityManager.createQuery( query ).getSingleResult();
					assertEquals( new BigDecimal( "110.0" ).stripTrailingZeros(), result.stripTrailingZeros() );
				}
		);
	}

	@Test
	public void testCastToBigDecimal2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final var criteriaBuilder = entityManager.getCriteriaBuilder();
					final var query = criteriaBuilder.createQuery( BigDecimal.class );
					final var person = query.from( Person.class );
					final var ageAttribute = person.get( CriteriaToBigDecimalTest_.Person_.age ).cast( BigDecimal.class );
					final var prod = criteriaBuilder.prod( ageAttribute, new BigDecimal( "5.501" ) );
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
					final var criteriaBuilder = entityManager.getCriteriaBuilder();
					final var query = criteriaBuilder.createQuery();
					final var person = query.from( Person.class );
					query.select( criteriaBuilder.sum( person.get( CriteriaToBigDecimalTest_.Person_.ageAsBigDecimal ), 1 ) );
					var result = (BigDecimal) entityManager.createQuery( query ).getSingleResult();
					assertEquals( new BigDecimal( "21" ), result.stripTrailingZeros() );
				}
		);
	}

	@Test
	public void testToBigDecimal4(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					BigDecimal result = (BigDecimal)
							entityManager.createQuery( "select p.ageAsBigDecimal + :val from Person p" )
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
