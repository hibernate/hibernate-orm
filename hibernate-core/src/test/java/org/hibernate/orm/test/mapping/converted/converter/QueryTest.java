/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test AttributeConverter functioning in various Query scenarios.
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {QueryTest.Employee.class, QueryTest.SalaryConverter.class})
@SessionFactory
public class QueryTest {

	private static final float SALARY = 267.89f;
	private static final float EXPECTED_NON_CONVERTED = 26789f;

	@Test
	public void testJpqlFloatLiteral(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Employee jDoe = session.createQuery( "from Employee e where e.salary = " + SALARY + "f", Employee.class ).uniqueResult();
			assertNotNull( jDoe );
		} );
	}

	@Test
	public void testJpqlBooleanLiteral(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertNotNull( session.createQuery( "from Employee e where e.active = true", Employee.class ).uniqueResult() );
			assertNull( session.createQuery( "from Employee e where e.active = false", Employee.class ).uniqueResult() );
		} );
	}

	@Test
	@JiraKey( "HHH-13082" )
	public void testNativeQueryResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final NativeQuery<Object[]> query = session.createNativeQuery( "select id, salary from EMP", "emp_id_salary", Object[].class );

			final List<Object[]> results = query.list();
			assertThat( results ).hasSize( 1 );

			final Object[] values = results.get( 0 );
			assertThat( values[0] ).isEqualTo( 1 );
			assertThat( values[1] ).isEqualTo( SALARY );
		} );
	}

	@Test
	public void testNativeQueryResultWithResultClass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final NativeQuery<Object[]> query = session.createNativeQuery( "select id, salary from EMP", "emp_id_salary", Object[].class );

			final List<Object[]> results = query.list();
			assertThat( results ).hasSize( 1 );

			final Object[] values = results.get( 0 );
			assertThat( values[0] ).isEqualTo( 1 );
			assertThat( values[1] ).isEqualTo( SALARY );
		} );
	}

	@Test
	@JiraKey( "HHH-14975" )
	public void testAutoAppliedConverterAsNativeQueryResult(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final NativeQuery<Object[]> query = session.createNativeQuery( "select id, salary from EMP", "emp_id_salary2", Object[].class );

			final List<Object[]> results = query.list();
			assertThat( results ).hasSize( 1 );

			final Object[] values = results.get( 0 );
			assertThat( values[0] ).isEqualTo( 1 );
			assertThat( values[1] ).isEqualTo( EXPECTED_NON_CONVERTED );
		} );
	}

	@BeforeEach
	public void setUpTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(new Employee(1, new Name("John", "Q.", "Doe" ), SALARY)) );
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> scope.dropData() );
	}

	@Entity( name = "Employee" )
	@Table( name = "EMP" )
	@SqlResultSetMapping(
			name = "emp_id_salary",
			columns = {
					@ColumnResult( name = "id" ),
					@ColumnResult( name = "salary", type = SalaryConverter.class )
			}
	)
	@SqlResultSetMapping(
			name = "emp_id_salary2",
			columns = {
					@ColumnResult( name = "id" ),
					@ColumnResult( name = "salary", type = Float.class )
			}
	)
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

			return Long.valueOf( (long)(attribute*100) );
		}

		@Override
		@SuppressWarnings("UnnecessaryBoxing")
		public Float convertToEntityAttribute(Long dbData) {
			if ( dbData == null ) {
				return null;
			}

			return Float.valueOf( (dbData.floatValue()) / 100);
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
