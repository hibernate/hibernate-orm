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
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				NestedEmbeddableInsertTimeGeneratedPropertiesTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16957")
public class NestedEmbeddableInsertTimeGeneratedPropertiesTest {

	@Test
	public void testGeneratedProperties(SessionFactoryScope scope) {
		Long id = 1L;
		String nonGeneratedPropertyValue = "non generated";
		String generatedButWritablePropertyValue = "generated but writable";
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( id, "generated", generatedButWritablePropertyValue,
							nonGeneratedPropertyValue );
					session.persist( testEntity );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, id );
					assertThat( testEntity.anEmbeddable.secondEmbeddable.secondNonGeneratedProperty )
							.isEqualTo( nonGeneratedPropertyValue );
					assertThat( testEntity.anEmbeddable.secondEmbeddable.secondGeneratedProperty )
							.isNull();
					assertThat( testEntity.anEmbeddable.secondEmbeddable.secondGeneratedButWritableProperty )
							.isEqualTo( generatedButWritablePropertyValue );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

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
		ASecondEmbeddable secondEmbeddable;


		public AnEmbeddable() {
		}

		public AnEmbeddable(String nonGeneratedProperty) {

			this.secondEmbeddable = new ASecondEmbeddable( nonGeneratedProperty );
		}

		public AnEmbeddable(String generatedButWritableProperty, String nonGeneratedProperty) {
			this.secondEmbeddable = new ASecondEmbeddable( generatedButWritableProperty, nonGeneratedProperty );
		}

		public AnEmbeddable(String generatedProperty, String generatedButWritableProperty, String nonGeneratedProperty) {
			this.secondEmbeddable = new ASecondEmbeddable( generatedProperty, generatedButWritableProperty,
					nonGeneratedProperty );
		}
	}

	@Embeddable
	public static class ASecondEmbeddable {
		@Generated
		private String secondGeneratedProperty;

		@Generated(writable = true)
		private String secondGeneratedButWritableProperty;

		private String secondNonGeneratedProperty;

		public ASecondEmbeddable() {
		}

		public ASecondEmbeddable(String secondNonGeneratedProperty) {
			this.secondNonGeneratedProperty = secondNonGeneratedProperty;
		}

		public ASecondEmbeddable(String secondGeneratedProperty, String secondGeneratedButWritableProperty) {
			this.secondGeneratedButWritableProperty = secondGeneratedProperty;
			this.secondNonGeneratedProperty = secondGeneratedButWritableProperty;
		}

		public ASecondEmbeddable(String secondGeneratedProperty, String secondGeneratedButWritableProperty, String secondNonGeneratedProperty) {
			this.secondGeneratedProperty = secondGeneratedProperty;
			this.secondGeneratedButWritableProperty = secondGeneratedButWritableProperty;
			this.secondNonGeneratedProperty = secondNonGeneratedProperty;
		}
	}
}
