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
				EmbeddableInsertTimeGeneratedPropertiesTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16957")
public class EmbeddableInsertTimeGeneratedPropertiesTest {

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
					assertThat( testEntity.anEmbeddable.nonGeneratedProperty )
							.isEqualTo( nonGeneratedPropertyValue );
					assertThat( testEntity.anEmbeddable.generatedProperty )
							.isNull();
					assertThat( testEntity.anEmbeddable.generatedButWritableProperty )
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
		@Generated
		private String generatedProperty;

		@Generated(writable = true)
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
