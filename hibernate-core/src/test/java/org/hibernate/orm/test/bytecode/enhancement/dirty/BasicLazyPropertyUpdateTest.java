/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import org.hibernate.Hibernate;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jira("HHH-14244")
@DomainModel(
		annotatedClasses = {
				BasicLazyPropertyUpdateTest.TestEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced(testEnhancedClasses = BasicLazyPropertyUpdateTest.TestEntity.class)
public class BasicLazyPropertyUpdateTest {

	private static final Long ID = 1L;
	private static final String NEW_ENTITY_NAME = "new name";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( ID, "name", "lazy value" );
					session.persist( testEntity );
				}
		);
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope)  {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdateNonLazyProperty(SessionFactoryScope scope) {

		TestEntity testEntity = scope.fromTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, ID );
					assertThatLazyPropertyIsNotInitialized( entity );
					entity.setName( NEW_ENTITY_NAME );
					return entity;
				}
		);

		assertThatLazyPropertyIsNotInitialized( testEntity );

		scope.inTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, ID );
					assertThat( entity.getName() ).isEqualTo( NEW_ENTITY_NAME );
				}
		);
	}

	@Test
	public void testUpdateLazyProperty(SessionFactoryScope scope) {
		String newLazyPropertyValue = "changed";

		TestEntity testEntity =  scope.fromTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, ID );
					assertThatLazyPropertyIsNotInitialized( entity );
					entity.setLazyProperty( newLazyPropertyValue );
					return entity;
				}
		);

		assertThatLazyPropertyIsInitialized( testEntity );

		assertThatLazyFieldIsEqualTo( newLazyPropertyValue, scope );
	}

	@Test
	public void testUpdateLazyPropertyToNull(SessionFactoryScope scope) {
		TestEntity testEntity =scope.fromTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, ID );
					assertThatLazyPropertyIsNotInitialized( entity );
					entity.setLazyProperty( null );
					return entity;
				}
		);

		assertThatLazyPropertyIsInitialized( testEntity );


		assertThatLazyFieldIsEqualTo( null, scope );
	}

	private static void assertThatLazyPropertyIsInitialized(TestEntity testEntity) {
		assertThat( Hibernate.isPropertyInitialized( testEntity, "lazyProperty" ) ).as( "Lazy property has not been initialized" ).isTrue();
	}

	private static void assertThatLazyPropertyIsNotInitialized(TestEntity entity) {
		assertThat( Hibernate.isPropertyInitialized( entity, "lazyProperty" ) ).as( "Lazy property has been initialized" ).isFalse();
	}

	private void assertThatLazyFieldIsEqualTo(String expected, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, ID );
					assertThatLazyPropertyIsNotInitialized( testEntity );
					assertThat( testEntity.getLazyProperty() ).isEqualTo(expected);
					assertThatLazyPropertyIsInitialized( testEntity );
				}
		);
	}

	@Entity(name= "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

		@Basic(fetch = FetchType.LAZY)
		private String lazyProperty;

		public TestEntity() {
		}

		public TestEntity(Long id, String name, String lazyProperty) {
			this.id = id;
			this.name = name;
			this.lazyProperty = lazyProperty;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setLazyProperty(String lazyProperty) {
			this.lazyProperty = lazyProperty;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getLazyProperty() {
			return lazyProperty;
		}
	}
}
