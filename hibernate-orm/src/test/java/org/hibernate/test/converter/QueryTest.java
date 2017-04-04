/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test AttributeConverter functioning in various Query scenarios.
 *
 * @author Steve Ebersole
 */
public class QueryTest extends BaseNonConfigCoreFunctionalTestCase {

	public static final float SALARY = 267.89f;

	@Test
	public void testJpqlFloatLiteral() {
		Session session = openSession();
		session.getTransaction().begin();
		Employee jDoe = (Employee) session.createQuery( "from Employee e where e.salary = " + SALARY + "f" ).uniqueResult();
		assertNotNull( jDoe );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testJpqlBooleanLiteral() {
		Session session = openSession();
		session.getTransaction().begin();
		assertNotNull( session.createQuery( "from Employee e where e.active = true" ).uniqueResult() );
		assertNull( session.createQuery( "from Employee e where e.active = false" ).uniqueResult() );
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class, SalaryConverter.class };
	}

	@Before
	public void setUpTestData() {
		Session session = openSession();
		session.getTransaction().begin();
		session.persist( new Employee( 1, new Name( "John", "Q.", "Doe" ), SALARY ) );
		session.getTransaction().commit();
		session.close();
	}

	@After
	public void cleanUpTestData() {
		Session session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete Employee" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Entity( name = "Employee" )
	@Table( name = "EMP" )
	public static class Employee {
		@Id
		public Integer id;
		@Embedded
		public Name name;
		public Float salary;
		@Convert( converter = BooleanConverter.class )
		public boolean active = true;

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
		@Column(name = "first_name")
		public String first;
		public String middle;
		@Column(name = "last_name")
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

	public static class BooleanConverter implements AttributeConverter<Boolean,Integer> {

		@Override
		public Integer convertToDatabaseColumn(Boolean attribute) {
			if ( attribute == null ) {
				return null;
			}
			return attribute ? 1 : 0;
		}

		@Override
		public Boolean convertToEntityAttribute(Integer dbData) {
			if ( dbData == null ) {
				return null;
			}
			else if ( dbData == 0 ) {
				return false;
			}
			else if ( dbData == 1 ) {
				return true;
			}
			throw new HibernateException( "Unexpected boolean numeric; expecting null, 0 or 1, but found " + dbData );
		}
	}
}
