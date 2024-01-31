/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = ConvertedListAttributeQueryTest.Employee.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17393" )
public class ConvertedListAttributeQueryTest {
	private final static List<String> PHONE_NUMBERS = List.of( "0911 111 111", "0922 222 222" );

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Employee( 1, PHONE_NUMBERS ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Employee" ) );
	}

	@Test
	@SuppressWarnings( "rawtypes" )
	public void testListHQL(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List resultList = session.createQuery(
					"select emp.phoneNumbers from Employee emp where emp.id = :EMP_ID",
					List.class
			).setParameter( "EMP_ID", 1 ).getSingleResult();
			assertThat( resultList ).isEqualTo( PHONE_NUMBERS );
		} );
	}

	@Test
	@SuppressWarnings( "rawtypes" )
	public void testListCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<List> q = cb.createQuery( List.class );
			final Root<Employee> r = q.from( Employee.class );
			q.select( r.get( "phoneNumbers" ) );
			q.where( cb.equal( r.get( "id" ), 1 ) );
			final List resultList = session.createQuery( q ).getSingleResult();
			assertThat( resultList ).isEqualTo( PHONE_NUMBERS );
		} );
	}

	@Test
	public void testArrayCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Integer[]> q = cb.createQuery( Integer[].class );
			final Root<Employee> r = q.from( Employee.class );
			q.multiselect( r.get( "id" ), r.get( "id" ) );
			q.where( cb.equal( r.get( "id" ), 1 ) );
			final Object result = session.createQuery( q ).getSingleResult();
			assertThat( result ).isInstanceOf( Object[].class );
			assertThat( ( (Object[]) result ) ).containsExactly( 1, 1 );
		} );
	}

	@Entity( name = "Employee" )
	public static class Employee {
		@Id
		private Integer id;

		@Convert( converter = StringListConverter.class )
		private List<String> phoneNumbers;

		public Employee() {
		}

		public Employee(Integer id, List<String> phoneNumbers) {
			this.id = id;
			this.phoneNumbers = phoneNumbers;
		}

		public Integer getId() {
			return id;
		}

		public List<String> getPhoneNumbers() {
			return phoneNumbers;
		}
	}

	public static class StringListConverter implements AttributeConverter<List<String>, String> {
		@Override
		public String convertToDatabaseColumn(final List<String> elements) {
			return elements == null || elements.isEmpty() ? null : String.join( ",", elements );
		}

		@Override
		public List<String> convertToEntityAttribute(final String dbData) {
			return dbData == null ? null : List.of( dbData.split( "," ) );
		}
	}
}
