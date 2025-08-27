/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters.legacy;

import static org.junit.Assert.assertEquals;

import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test AttributeConverter functioning in various Query scenarios.
 *
 * @author Etienne Miret
 */
public class QueryTest extends BaseEntityManagerFunctionalTestCase {

	public static final float SALARY = 267.89f;

	@Test
	@JiraKey( value = "HHH-9356" )
	public void testCriteriaBetween() {
		final EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		final CriteriaBuilder cb = em.getCriteriaBuilder();
		final CriteriaQuery<Employee> query = cb.createQuery( Employee.class );
		final Root<Employee> root = query.from( Employee.class );
		query.select( root );

		query.where( cb.between( root.<Float>get( "salary" ), new Float( 300f ), new Float( 400f ) ) );
		final List<Employee> result0 = em.createQuery( query ).getResultList();
		assertEquals( 0, result0.size() );

		query.where( cb.between( root.<Float>get( "salary" ), new Float( 100f ), new Float( 200f ) ) );
		final List<Employee> result1 = em.createQuery( query ).getResultList();
		assertEquals( 0, result1.size() );

		query.where( cb.between( root.<Float>get( "salary" ), new Float( 200f ), new Float( 300f ) ));
		final List<Employee> result2 = em.createQuery( query ).getResultList();
		assertEquals( 1, result2.size() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testJpqlLiteralBetween() {
		final EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		@SuppressWarnings("unchecked")
		final List<Employee> result0 = em.createQuery( "from Employee where salary between 300.0F and 400.0F" ).getResultList();
		assertEquals( 0, result0.size() );

		@SuppressWarnings("unchecked")
		final List<Employee> result1 = em.createQuery( "from Employee where salary between 100.0F and 200.0F" ).getResultList();
		assertEquals( 0, result1.size() );

		@SuppressWarnings("unchecked")
		final List<Employee> result2 = em.createQuery( "from Employee where salary between 200.0F and 300.0F" ).getResultList();
		assertEquals( 1, result2.size() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testJpqlParametrizedBetween() {
		final EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		final Query query = em.createQuery( "from Employee where salary between :low and :high" );

		query.setParameter( "low", new Float( 300f ) );
		query.setParameter( "high", new Float( 400f ) );
		assertEquals( 0, query.getResultList().size() );

		query.setParameter( "low", new Float( 100f ) );
		query.setParameter( "high", new Float( 200f ) );
		assertEquals( 0, query.getResultList().size() );

		query.setParameter( "low", new Float( 200f ) );
		query.setParameter( "high", new Float( 300f ) );
		assertEquals( 1, query.getResultList().size() );

		em.getTransaction().commit();
		em.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class, SalaryConverter.class };
	}

	@Before
	public void setUpTestData() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		em.persist( new Employee( 1, new Name( "John", "Q.", "Doe" ), SALARY ) );
		em.getTransaction().commit();
		em.close();
	}

	@After
	public void cleanUpTestData() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Employee" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
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
		@SuppressWarnings("UnnecessaryBoxing")
		public Long convertToDatabaseColumn(Float attribute) {
			if ( attribute == null ) {
				return null;
			}

			return new Long( (long)(attribute*100) );
		}

		@Override
		@SuppressWarnings("UnnecessaryBoxing")
		public Float convertToEntityAttribute(Long dbData) {
			if ( dbData == null ) {
				return null;
			}

			return new Float( ( dbData.floatValue() ) / 100 );
		}
	}
}
