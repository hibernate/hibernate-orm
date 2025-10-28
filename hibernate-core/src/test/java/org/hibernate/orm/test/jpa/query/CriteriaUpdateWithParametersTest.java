/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;


@Jpa(annotatedClasses = {
		CriteriaUpdateWithParametersTest.Person.class,
		CriteriaUpdateWithParametersTest.Process.class
})
public class CriteriaUpdateWithParametersTest {

	@Test
	public void testCriteriaUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			final CriteriaUpdate<Person> criteriaUpdate = criteriaBuilder.createCriteriaUpdate( Person.class );
			final Root<Person> root = criteriaUpdate.from( Person.class );

			final ParameterExpression<Integer> intValueParameter = criteriaBuilder.parameter( Integer.class );
			final ParameterExpression<String> stringValueParameter = criteriaBuilder.parameter( String.class );

			final EntityType<Person> personEntityType = entityManager.getMetamodel().entity( Person.class );

			criteriaUpdate.set( root.get( personEntityType.getSingularAttribute( "age", Integer.class ) ),
					intValueParameter );
			criteriaUpdate.where(
					criteriaBuilder.equal( root.get( personEntityType.getSingularAttribute( "name", String.class ) ),
							stringValueParameter ) );

			final Query query = entityManager.createQuery( criteriaUpdate );
			query.setParameter( intValueParameter, 9 );
			query.setParameter( stringValueParameter, "Luigi" );

			query.executeUpdate();
		} );
	}

	@Test
	public void testCriteriaUpdate2(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			final CriteriaUpdate<Person> criteriaUpdate = criteriaBuilder.createCriteriaUpdate( Person.class );
			final Root<Person> root = criteriaUpdate.from( Person.class );

			final ParameterExpression<Integer> intValueParameter = criteriaBuilder.parameter( Integer.class );
			final ParameterExpression<String> stringValueParameter = criteriaBuilder.parameter( String.class );

			criteriaUpdate.set( "age", intValueParameter );
			criteriaUpdate.where( criteriaBuilder.equal( root.get( "name" ), stringValueParameter ) );

			final Query query = entityManager.createQuery( criteriaUpdate );
			query.setParameter( intValueParameter, 9 );
			query.setParameter( stringValueParameter, "Luigi" );

			query.executeUpdate();
		} );
	}

	@Test
	public void testCriteriaUpdate3(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// test separate value-bind parameters
			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaUpdate<Process> cu = cb.createCriteriaUpdate( Process.class );
			final Root<Process> root = cu.from( Process.class );
			cu.set( root.get( "name" ), (Object) null );
			cu.set( root.get( "payload" ), (Object) null );
			em.createQuery( cu ).executeUpdate();
		} );

		scope.inTransaction( em -> {
			// test with the same cb.value( null ) parameter instance
			final HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
			final CriteriaUpdate<Process> cu = cb.createCriteriaUpdate( Process.class );
			final Root<Process> root = cu.from( Process.class );
			final Expression<Object> nullValue = cb.value( null );
			// a bit unfortunate, but we need to cast here to prevent ambiguous method references
			final Path<String> name = root.get( "name" );
			final Path<byte[]> payload = root.get( "payload" );
			final Expression<? extends String> nullString = cast( nullValue );
			final Expression<? extends byte[]> nullBytes = cast( nullValue );
			cu.set( name, nullString );
			cu.set( payload, nullBytes );
			em.createQuery( cu ).executeUpdate();
		} );
	}

	private static <X> Expression<? extends X> cast(Expression<?> expression) {
		//noinspection unchecked
		return (Expression<? extends X>) expression;
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private String id;

		private String name;

		private Integer age;

		public Person() {
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Integer getAge() {
			return age;
		}
	}

	@Entity
	public static class Process {
		@Id
		@GeneratedValue
		private Long id;

		// All attributes below are necessary to reproduce the issue

		private String name;

		@Lob
		private byte[] payload;
	}
}
