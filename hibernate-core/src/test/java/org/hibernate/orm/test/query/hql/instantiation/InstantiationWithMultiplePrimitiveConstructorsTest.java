/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.instantiation;

import org.hibernate.annotations.Imported;
import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		InstantiationWithMultiplePrimitiveConstructorsTest.TestEntity.class,
		InstantiationWithMultiplePrimitiveConstructorsTest.TestDto.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17155" )
public class InstantiationWithMultiplePrimitiveConstructorsTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity(
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				"test"
		) ) );
	}

	@Test
	public void testIntegerConstructor(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestDto result = session.createQuery(
					"select new TestDto(t.intValue, t.name) from TestEntity t",
					TestDto.class
			).getSingleResult();
			assertThat( result.getValue() ).isEqualTo( String.valueOf( Integer.MAX_VALUE ) );
		} );
	}

	@Test
	public void testIntegerInjection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestInjection result = session.createQuery(
					"select new TestInjection(t.intValue as value, t.name as title) from TestEntity t",
					TestInjection.class
			).getSingleResult();
			assertThat( result.getValue() ).isEqualTo( Integer.MAX_VALUE );
		} );
	}

	@Test
	public void testLongConstructor(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestDto result = session.createQuery(
					"select new TestDto(t.longValue, t.name) from TestEntity t",
					TestDto.class
			).getSingleResult();
			assertThat( result.getValue() ).isEqualTo( String.valueOf( Long.MAX_VALUE ) );
		} );
	}

	@Test
	public void testLongInjection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				final TestInjection result = session.createQuery(
						"select new TestInjection(t.longValue as value, t.name as title) from TestEntity t",
						TestInjection.class
				).getSingleResult();
				fail( "Long assignment to int should not be allowed" );
			}
			catch (Exception e) {
				assertThat( e.getCause() ).isInstanceOf( SemanticException.class );
			}
		} );
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;

		private Integer intValue;

		private Long longValue;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Integer intValue, Long longValue, String name) {
			this.intValue = intValue;
			this.longValue = longValue;
			this.name = name;
		}
	}

	@Imported
	public static class TestDto {
		private String value;
		private String title;

		public TestDto(String value, String title) {
			this.value = value;
			this.title = title;
		}

		public TestDto(long value, String title) {
			this( String.valueOf( value ), title );
		}

		public TestDto(int value, String title) {
			this( String.valueOf( value ), title );
		}

		public String getValue() {
			return value;
		}

		public String getTitle() {
			return title;
		}
	}

	public static class TestInjection {
		private int value;
		private String title;

		public void setValue(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
}
