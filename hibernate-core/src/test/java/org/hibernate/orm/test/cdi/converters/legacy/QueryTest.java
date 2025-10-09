/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters.legacy;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test AttributeConverter functioning in various Query scenarios.
 *
 * @author Etienne Miret
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = {QueryTest.Employee.class, QueryTest.SalaryConverter.class})
public class QueryTest {

	public static final float SALARY = 267.89f;

	@Test
	@JiraKey( value = "HHH-9356" )
	public void testCriteriaBetween(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( (em) -> {
			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaQuery<Employee> query = cb.createQuery( Employee.class );
			final Root<Employee> root = query.from( Employee.class );
			query.select( root );

			query.where( cb.between( root.get( "salary" ), 300f, 400f ) );
			final List<Employee> result0 = em.createQuery( query ).getResultList();
			assertEquals( 0, result0.size() );

			query.where( cb.between( root.get( "salary" ), 100f, 200f ) );
			final List<Employee> result1 = em.createQuery( query ).getResultList();
			assertEquals( 0, result1.size() );

			query.where( cb.between( root.get( "salary" ), 200f, 300f ));
			final List<Employee> result2 = em.createQuery( query ).getResultList();
			assertEquals( 1, result2.size() );
		} );
	}

	@Test
	public void testJpqlLiteralBetween(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( (em) -> {
			@SuppressWarnings("unchecked")
			final List<Employee> result0 = em.createQuery( "from Employee where salary between 300.0F and 400.0F" ).getResultList();
			assertEquals( 0, result0.size() );

			@SuppressWarnings("unchecked")
			final List<Employee> result1 = em.createQuery( "from Employee where salary between 100.0F and 200.0F" ).getResultList();
			assertEquals( 0, result1.size() );

			@SuppressWarnings("unchecked")
			final List<Employee> result2 = em.createQuery( "from Employee where salary between 200.0F and 300.0F" ).getResultList();
			assertEquals( 1, result2.size() );
		} );
	}

	@Test
	public void testJpqlParametrizedBetween(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( (em) -> {
			final Query query = em.createQuery( "from Employee where salary between :low and :high" );

			query.setParameter( "low", 300f );
			query.setParameter( "high", 400f );
			assertEquals( 0, query.getResultList().size() );

			query.setParameter( "low", 100f );
			query.setParameter( "high", 200f );
			assertEquals( 0, query.getResultList().size() );

			query.setParameter( "low", 200f );
			query.setParameter( "high", 300f );
			assertEquals( 1, query.getResultList().size() );
		} );
	}

	@BeforeEach
	public void setUpTestData(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( (em) -> {
			em.persist( new Employee( 1, new Name( "John", "Q.", "Doe" ), SALARY ) );
		} );
	}

	@AfterEach
	public void cleanUpTestData(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Entity( name = "Employee" )
	@Table( name = "EMP" )
	public static class Employee {
		@Id
		public Integer id;
		@Embedded
		public Name name;
		public Float salary;

		public Employee() {
		}

		public Employee(Integer id, Name name, Float salary) {
			this.id = id;
			this.name = name;
			this.salary = salary;
		}
	}

	@Embeddable
	public static class Name {
		public String firstName;
		public String middleName;
		public String lastName;

		public Name() {
		}

		public Name(String firstName, String middleName, String lastName) {
			this.firstName = firstName;
			this.middleName = middleName;
			this.lastName = lastName;
		}
	}

	@Converter( autoApply = true )
	public static class SalaryConverter implements AttributeConverter<Float,Long> {
		@Override
		public Long convertToDatabaseColumn(Float attribute) {
			if ( attribute == null ) {
				return null;
			}

			return (long) (attribute * 100);
		}

		@Override
		public Float convertToEntityAttribute(Long dbData) {
			if ( dbData == null ) {
				return null;
			}

			return dbData.floatValue() / 100;
		}
	}
}
