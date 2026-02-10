/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import org.hibernate.query.QueryArgumentException;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Jpa(
		annotatedClasses = {
				ObjectParameterTypeForEmbeddableTest.TestEntity.class
		}
)
@JiraKey(value = "HHH-16247")
public class ObjectParameterTypeForEmbeddableTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TestEntity entity = new TestEntity( 1l, "test", new AnEmbeddable( "foo", "bar" ) );
					entityManager.persist( entity );
				}
		);
	}

	@Test
	public void testSettingParameterOfTypeObject(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					final CriteriaQuery<TestEntity> query = cb.createQuery( TestEntity.class );
					final Root<TestEntity> root = query.from( TestEntity.class );
					//deliberately using Object type here, using AnEmbeddable works
					final ParameterExpression<Object> parameter = cb.parameter( Object.class );
					query.select( root ).where( cb.equal( root.get( "anEmbeddable" ), parameter ) );

					final TypedQuery<TestEntity> typedQuery = entityManager.createQuery( query );
					typedQuery.setParameter( parameter, new AnEmbeddable( "foo", "bar" ) );
					assertThat( typedQuery.getResultList().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testSettingParameterOfTypeAnEmbeddable(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					final CriteriaQuery<TestEntity> query = cb.createQuery( TestEntity.class );
					final Root<TestEntity> root = query.from( TestEntity.class );
					final ParameterExpression<AnEmbeddable> parameter = cb.parameter( AnEmbeddable.class );
					query.select( root ).where( cb.equal( root.get( "anEmbeddable" ), parameter ) );

					final TypedQuery<TestEntity> typedQuery = entityManager.createQuery( query );
					typedQuery.setParameter( parameter, new AnEmbeddable( "foo", "bar" ) );
					assertThat( typedQuery.getResultList().size() ).isEqualTo( 1 );

				}
		);
	}

	@Test
	public void testSettingParameterOfTypeWrongType(EntityManagerFactoryScope scope) {
		QueryArgumentException thrown = assertThrows(
				QueryArgumentException.class, () ->
						scope.inTransaction(
								entityManager -> {
									final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
									final CriteriaQuery<TestEntity> query = cb.createQuery( TestEntity.class );
									final Root<TestEntity> root = query.from( TestEntity.class );
									final ParameterExpression<Object> parameter = cb.parameter( Object.class );
									query.select( root ).where( cb.equal( root.get( "anEmbeddable" ), parameter ) );

									final TypedQuery<TestEntity> typedQuery = entityManager.createQuery( query );
									typedQuery.setParameter( parameter, new AnObject() );
									assertThat( typedQuery.getResultList().size() ).isEqualTo( 1 );
								}
						)
		);

		assertThat( thrown.getMessage() ).contains( "incompatible type" );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private long id;

		private String name;

		@Embedded
		private AnEmbeddable anEmbeddable;

		public TestEntity(long id, String name, AnEmbeddable e) {
			this.id = id;
			this.name = name;
			this.anEmbeddable = e;
		}

		public TestEntity() {
		}

		public long getId() {
			return id;
		}

		public AnEmbeddable getAnEmbeddable() {
			return anEmbeddable;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	public static class AnEmbeddable {
		private String foo;
		private String bar;

		public AnEmbeddable(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public AnEmbeddable() {
		}

		public String getFoo() {
			return foo;
		}

		public String getBar() {
			return bar;
		}
	}

	public static class AnObject {
		private String foo;
		private String bar;

		public AnObject(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public AnObject() {
		}

		public String getFoo() {
			return foo;
		}

		public String getBar() {
			return bar;
		}
	}

}
