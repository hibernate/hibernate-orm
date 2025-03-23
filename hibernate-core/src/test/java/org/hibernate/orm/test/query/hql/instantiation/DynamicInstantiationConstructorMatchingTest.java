/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.instantiation;

import org.hibernate.annotations.Imported;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel( annotatedClasses = { DynamicInstantiationConstructorMatchingTest.TestEntity.class } )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18322" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18664" )
public class DynamicInstantiationConstructorMatchingTest {
	@BeforeAll
	public void prepareData(final SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity( 1, 42, "test", 13 ) ) );
	}

	@AfterAll
	public void cleanUpData(final SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void testExplicitConstructor(final SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var result = session.createQuery(
					"select new ConstructorDto(num, str) from TestEntity",
					ConstructorDto.class
			).getSingleResult();
			assertEquals( 42, result.getNum() );
			assertEquals( "test", result.getStr() );
		} );
	}

	@Test
	void testImplicitConstructor(final SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var result = session.createQuery( "select num, str from TestEntity", ConstructorDto.class )
					.getSingleResult();
			assertEquals( 42, result.getNum() );
			assertEquals( "test", result.getStr() );
		} );
	}

	@Test
	void testExplicitConstructorWithPrimitive(final SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var result = session.createQuery(
							"select new ConstructorWithPrimitiveDto(intValue, str) from TestEntity",
							ConstructorWithPrimitiveDto.class
					)
					.getSingleResult();
			assertEquals( 13, result.getIntValue() );
			assertEquals( "test", result.getStr() );
		} );
	}

	@Test
	void testImplicitConstructorWithPrimitive(final SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var result = session.createQuery(
							"select intValue, str from TestEntity",
							ConstructorWithPrimitiveDto.class
					)
					.getSingleResult();
			assertEquals( 13, result.getIntValue() );
			assertEquals( "test", result.getStr() );
		} );
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		private Integer id;

		private Integer num;

		private String str;

		private int intValue;

		public TestEntity() {
		}

		public TestEntity(final Integer id, final Integer num, final String str, final int intValue) {
			this.id = id;
			this.num = num;
			this.str = str;
			this.intValue = intValue;
		}
	}

	@Imported
	public static class ConstructorDto {
		private final Number num;
		private final String str;

		public ConstructorDto(final Number num, final String str) {
			this.num = num;
			this.str = str;
		}

		public Number getNum() {
			return num;
		}

		public String getStr() {
			return str;
		}
	}

	@Imported
	public static class ConstructorWithPrimitiveDto {
		private final int intValue;
		private final String str;

		public ConstructorWithPrimitiveDto(final int intValue, final String str) {
			this.intValue = intValue;
			this.str = str;
		}

		public int getIntValue() {
			return intValue;
		}

		public String getStr() {
			return str;
		}
	}
}
