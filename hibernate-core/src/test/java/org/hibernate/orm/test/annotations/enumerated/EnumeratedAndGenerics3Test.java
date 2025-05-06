/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.enumerated;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EnumeratedAndGenerics3Test.TestEntity.class,
		}
)
@SessionFactory
@JiraKey(value = "HHH-16479")
public class EnumeratedAndGenerics3Test {

	@Test
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1l );
					entity.setState( TestEnum.BAR );
					session.persist( entity );

				}
		);

		scope.inTransaction(
				session -> {
					TestEntity entity = session.get( TestEntity.class, 1l );
					assertThat( entity ).isNotNull();
					assertThat( entity.getState() ).isEqualTo( TestEnum.BAR );

				}
		);
	}

	public interface EnumBase {
		String name();
	}

	public enum TestEnum implements EnumBase {
		FOO, BAR
	}

	@MappedSuperclass
	public static abstract class GenericBaseEntity<T extends TestEnum> {

		@Enumerated(EnumType.STRING)
		protected T state;

		public T getState() {
			return state;
		}

		public void setState(T state) {
			this.state = state;
		}
	}

	@Entity(name="TestEntity")
	public static class TestEntity extends GenericBaseEntity<TestEnum> {

		@Id
		private long id;

		protected TestEntity() {
		}

		public TestEntity(long id) {
			this.id = id;
		}

	}

}
