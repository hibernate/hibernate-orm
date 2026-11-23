package org.hibernate.orm.test.eviction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@DomainModel(
		annotatedClasses = EvictAndGetTest.TestEntity.class
)
@SessionFactory
public class EvictAndGetTest {

	@Test
	public void testGetAfterEviction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 1L );

					TestEntity entity = new TestEntity( 1L );
					session.persist( entity );

					session.flush();

					session.evict( entity );

					session.get( TestEntity.class, 1L );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id) {
			this.id = id;
		}
	}
}
