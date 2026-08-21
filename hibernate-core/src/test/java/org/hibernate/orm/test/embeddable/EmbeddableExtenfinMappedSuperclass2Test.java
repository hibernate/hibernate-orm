/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


// It's important for the test that only the mapped superclass is listed in the @DomainModel annotatedClasses
// along with the AnotherEmbeddableName embeddabele class but not the EmbeddableName class,
// which is the actual type of the embedded attribute in TestEntity.
@DomainModel(
		annotatedClasses = {
				EmbeddableExtenfinMappedSuperclass2Test.TestEntity.class,
				EmbeddableExtenfinMappedSuperclass2Test.AbstractName.class,
				EmbeddableExtenfinMappedSuperclass2Test.AnotherEmbeddableName.class
		}
)
@SessionFactory
@JiraKey("HHH-19020")
public class EmbeddableExtenfinMappedSuperclass2Test {

	@Test
	public void testPersist(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( "1", new EmbeddableName( "Test" ) ) );
		} );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private String id;

		private EmbeddableName name;

		public TestEntity(String id, EmbeddableName name) {
			this.id = id;
			this.name = name;
		}
	}

	@MappedSuperclass
	public static class AbstractName {
		private String name;

		public AbstractName() {
		}

		public AbstractName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class EmbeddableName extends AbstractName {

		public EmbeddableName() {
		}

		public EmbeddableName(String name) {
			super( name );
		}
	}

	@Embeddable
	public static class AnotherEmbeddableName extends AbstractName {
		public AnotherEmbeddableName() {
		}

		public AnotherEmbeddableName(String name) {
			super( name );
		}
	}
}
