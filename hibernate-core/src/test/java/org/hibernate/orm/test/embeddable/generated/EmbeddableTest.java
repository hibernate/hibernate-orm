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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddableTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16957")
public class EmbeddableTest {

	@Test
	public void testEmbeddable(SessionFactoryScope scope) {
		Long id = 1L;
		String nonGeneratedPropertyValue = "non generated";
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( id, nonGeneratedPropertyValue );
					session.persist( testEntity );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, id );
					assertThat( testEntity.anEmbeddable.nonGeneratedProperty ).isEqualTo( nonGeneratedPropertyValue );
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

		public TestEntity(Long id, String generatedProperty, String nonGeneratedProperty) {
			this.id = id;
			this.anEmbeddable = new AnEmbeddable( generatedProperty, nonGeneratedProperty );
		}

		public TestEntity(Long id, String nonGeneratedProperty) {
			this.id = id;
			this.anEmbeddable = new AnEmbeddable( nonGeneratedProperty );
		}
	}

	@Embeddable
	public static class AnEmbeddable {
		@Generated
		private String generatedProperty;

		private String nonGeneratedProperty;


		public AnEmbeddable() {
		}

		public AnEmbeddable(String generatedProperty, String nonGeneratedProperty) {
			this.generatedProperty = generatedProperty;
			this.nonGeneratedProperty = nonGeneratedProperty;
		}

		public AnEmbeddable(String nonGeneratedProperty) {
			this.nonGeneratedProperty = nonGeneratedProperty;
		}
	}
}
