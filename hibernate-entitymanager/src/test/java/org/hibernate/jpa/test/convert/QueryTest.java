/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
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
	@TestForIssue( jiraKey = "HHH-9356" )
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
		public String first;
		public String middle;
		public String last;

		public Name() {
		}

		public Name(String first, String middle, String last) {
			this.first = first;
			this.middle = middle;
			this.last = last;
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
