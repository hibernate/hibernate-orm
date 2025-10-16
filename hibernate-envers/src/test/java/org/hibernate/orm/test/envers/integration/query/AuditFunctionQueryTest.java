/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Jpa(annotatedClasses = {
		AuditFunctionQueryTest.TestEntity.class
})
@EnversTest
public class AuditFunctionQueryTest {

	@Entity(name = "TestEntity")
	@Audited
	public static class TestEntity {

		@Id
		private Long id;
		private String string1;
		private String string2;
		private Integer integer1;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getString1() {
			return string1;
		}

		public void setString1(String string1) {
			this.string1 = string1;
		}

		public String getString2() {
			return string2;
		}

		public Integer getInteger1() {
			return integer1;
		}

		public void setInteger1(Integer integer1) {
			this.integer1 = integer1;
		}

		public void setString2(String string2) {
			this.string2 = string2;
		}

	}

	private TestEntity testEntity1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		testEntity1 = scope.fromTransaction( em -> {
			TestEntity entity = new TestEntity();
			entity.setId( 1L );
			entity.setString1( "abcdef" );
			entity.setString2( "42 - the truth" );
			entity.setInteger1( 42 );
			em.persist( entity );
			return entity;
		} );
	}

	@Test
	public void testProjectionWithPropertyArgument(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Object actual = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
					.addProjection( AuditEntity.function( "upper", AuditEntity.property( "string1" ) ) ).getSingleResult();
			String expected = testEntity1.getString1().toUpperCase();
			assertEquals( expected, actual, "Expected the property string1 to be upper case" );
		} );
	}

	@Test
	public void testProjectionWithPropertyAndSimpleArguments(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Object actual = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
					.addProjection( AuditEntity.function( "substring", AuditEntity.property( "string1" ), 3, 2 ) ).getSingleResult();
			// the sql substring indices are 1 based while java substring indices are 0 based
			// the sql substring second parameter denotes the length of the substring
			// while in java the scond argument denotes the end index
			String expected = testEntity1.getString1().substring( 2, 4 );
			assertEquals( expected, actual, "Expected the substring of the property string1" );
		} );
	}

	@Test
	public void testProjectionWithNestedFunction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Object actual = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
					.addProjection( AuditEntity.function( "concat",
							AuditEntity.function( "upper", AuditEntity.property( "string1" ) ),
							AuditEntity.function( "substring", AuditEntity.property( "string2" ), 1, 2 ) ) )
					.getSingleResult();
			final String expected = testEntity1.getString1().toUpperCase().concat( testEntity1.getString2().substring( 0, 2 ) );
			assertEquals( expected, actual, "Expected the upper cased string1 to be concat with the first two characters of string2" );
		} );
	}

	@Test
	public void testComparisonFunctionWithValue(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			TestEntity entity = (TestEntity) AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
					.add( AuditEntity.function( "substring", AuditEntity.property( "string1" ), 3, 2 ).eq( "cd" ) ).getSingleResult();
			assertNotNull( entity, "Expected the entity to be returned" );
			assertEquals( testEntity1.getId(), entity.getId(), "Expected the entity to be returned" );
		} );
	}

	@Test
	public void testComparionFunctionWithProperty(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			TestEntity entity = (TestEntity) AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
					.add( AuditEntity.function( "length", AuditEntity.property( "string2" ) ).ltProperty( "integer1" ) ).getSingleResult();
			assertNotNull( entity, "Expected the entity to be returned" );
			assertEquals( testEntity1.getId(), entity.getId(), "Expected the entity to be returned" );
		} );
	}

	@Test
	public void testComparisonFunctionWithFunction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			TestEntity entity = (TestEntity) AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
					.add( AuditEntity.function( "substring", AuditEntity.property( "string2" ), 1, 2 )
							.eqFunction( AuditEntity.function( "str", AuditEntity.property( "integer1" ) ) ) )
					.getSingleResult();
			assertNotNull( entity, "Expected the entity to be returned" );
			assertEquals( testEntity1.getId(), entity.getId(), "Expected the entity to be returned" );
		} );
	}

	@Test
	public void testComparisonPropertyWithFunction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			TestEntity entity = (TestEntity) AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
					.add( AuditEntity.property( "integer1" ).gtFunction( AuditEntity.function( "length", AuditEntity.property( "string2" ) ) ) ).getSingleResult();
			assertNotNull( entity, "Expected the entity to be returned" );
			assertEquals( testEntity1.getId(), entity.getId(), "Expected the entity to be returned" );
		} );
	}

	@Test
	public void testFunctionOnIdProperty(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			TestEntity entity = (TestEntity) AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
					.add( AuditEntity.function( "str", AuditEntity.id() ).like( "%1%" ) ).getSingleResult();
			assertNotNull( entity, "Expected the entity to be returned" );
			assertEquals( testEntity1.getId(), entity.getId(), "Expected the entity to be returned" );
		} );
	}

}
