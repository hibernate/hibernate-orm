/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable.generated;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddableUpdateTimeGeneratedPropertiesTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16957")
public class EmbeddableUpdateTimeGeneratedPropertiesTest {

	@AfterEach
	public void tearDown(SessionFactoryScope sessionFactoryScope) {
		sessionFactoryScope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testGeneratedProperties(SessionFactoryScope scope) {
		Long id = 1L;
		String nonGeneratedPropertyValue = "non generated";
		String generatedButWritablePropertyValue = "generated but writable";
		String generatedPropertyValue = "generated";

		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( id, generatedPropertyValue,
							generatedButWritablePropertyValue,
							nonGeneratedPropertyValue );
					session.persist( testEntity );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, id );
					assertThat( testEntity.anEmbeddable.nonGeneratedProperty )
							.isEqualTo( nonGeneratedPropertyValue );
					assertThat( testEntity.anEmbeddable.generatedProperty )
							.isEqualTo( generatedPropertyValue );
					assertThat( testEntity.anEmbeddable.generatedButWritableProperty )
							.isEqualTo( generatedButWritablePropertyValue );
				}
		);
	}

	@Test
	public void testGeneratedProperties2(SessionFactoryScope scope) {
		Long id = 1L;
		String nonGeneratedPropertyValue = "non generated";
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( id, nonGeneratedPropertyValue );
					session.persist( testEntity );
				}
		);

		String generatedButWritablePropertyValue = "generated but writable";
		String generatedPropertyValue = "generated";

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, id );
					assertThat( testEntity.anEmbeddable.nonGeneratedProperty )
							.isEqualTo( nonGeneratedPropertyValue );
					assertThat( testEntity.anEmbeddable.generatedProperty )
							.isEqualTo( null );
					assertThat( testEntity.anEmbeddable.generatedButWritableProperty )
							.isEqualTo( null );
					testEntity.anEmbeddable.generatedButWritableProperty = generatedButWritablePropertyValue;
					testEntity.anEmbeddable.generatedProperty = generatedPropertyValue;
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, id );
					assertThat( testEntity.anEmbeddable.nonGeneratedProperty )
							.isEqualTo( nonGeneratedPropertyValue );
					assertThat( testEntity.anEmbeddable.generatedProperty )
							.isEqualTo( null );
					assertThat( testEntity.anEmbeddable.generatedButWritableProperty )
							.isEqualTo( generatedButWritablePropertyValue );
					testEntity.name = "a";
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

		@Embedded
		private AnEmbeddable anEmbeddable;


		public TestEntity() {
		}

		public TestEntity(Long id, String nonGeneratedProperty) {
			this.id = id;
			this.anEmbeddable = new AnEmbeddable( nonGeneratedProperty );
		}

		public TestEntity(Long id, String generatedButWritableProperty, String nonGeneratedProperty) {
			this.id = id;
			this.anEmbeddable = new AnEmbeddable( generatedButWritableProperty, nonGeneratedProperty );
		}

		public TestEntity(Long id, String generatedProperty, String generatedButWritableProperty, String nonGeneratedProperty) {
			this.id = id;
			this.anEmbeddable = new AnEmbeddable( generatedProperty, generatedButWritableProperty,
					nonGeneratedProperty );
		}
	}

	@Embeddable
	public static class AnEmbeddable {
		@Generated(event = EventType.UPDATE)
		private String generatedProperty;

		@Generated(event = EventType.UPDATE, writable = true)
		private String generatedButWritableProperty;

		private String nonGeneratedProperty;


		public AnEmbeddable() {
		}

		public AnEmbeddable(String nonGeneratedProperty) {
			this.nonGeneratedProperty = nonGeneratedProperty;
		}

		public AnEmbeddable(String generatedButWritableProperty, String nonGeneratedProperty) {
			this.generatedButWritableProperty = generatedButWritableProperty;
			this.nonGeneratedProperty = nonGeneratedProperty;
		}

		public AnEmbeddable(String generatedProperty, String generatedButWritableProperty, String nonGeneratedProperty) {
			this.generatedProperty = generatedProperty;
			this.generatedButWritableProperty = generatedButWritableProperty;
			this.nonGeneratedProperty = nonGeneratedProperty;
		}
	}
}
