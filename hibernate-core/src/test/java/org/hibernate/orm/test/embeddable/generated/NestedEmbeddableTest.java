package org.hibernate.orm.test.embeddable.generated;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@DomainModel(
		annotatedClasses = {
				NestedEmbeddableTest.TestEntity.class
		}
)
@SessionFactory
public class NestedEmbeddableTest {

	@Test
	public void testEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( 1l, "prop1", "prop2" );
					session.save( testEntity );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		@Embedded
		private AnEmbeddable anEmbeddable;

		@ElementCollection
		@CollectionTable(name = "EmbSetEnt_set")
		private Set<AnEmbeddable> componentSet = new HashSet<>();

		public TestEntity() {
		}

		public TestEntity(Long id, String property1, String property2) {
			this.id = id;
			this.anEmbeddable = new AnEmbeddable( property1, property2 );
		}
	}

	@Embeddable
	public static class AnEmbeddable {
		@Generated(GenerationTime.INSERT)
		private String property1;

		@Embedded
		private AnotherEmbeddable anotherEmbeddable;

		public AnEmbeddable() {
		}

		public AnEmbeddable(String property1, String property2) {
			this.property1 = property1;
			this.anotherEmbeddable = new AnotherEmbeddable( property2 );
		}
	}

	@Embeddable
	public static class AnotherEmbeddable {
		@Generated(GenerationTime.UPDATE)
		private String property2;

		public AnotherEmbeddable() {
		}

		public AnotherEmbeddable(String property2) {
			this.property2 = property2;
		}
	}
}
