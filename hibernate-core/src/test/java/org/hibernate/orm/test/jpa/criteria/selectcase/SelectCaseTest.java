/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.selectcase;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey(value = "HHH-9731")
@Jpa(annotatedClasses = {SelectCaseTest.Entity.class})
public class SelectCaseTest {

	@Test
	public void selectCaseWithValuesShouldWork(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			CriteriaBuilder.Case<EnumValue> selectCase = cb.selectCase();
			Predicate somePredicate = cb.equal( cb.literal( 1 ), 1 );
			selectCase.when( somePredicate, EnumValue.VALUE_1 );
			selectCase.otherwise( EnumValue.VALUE_2 );

			CriteriaQuery<Entity> query = cb.createQuery( Entity.class );
			Root<Entity> from = query.from( Entity.class );
			query.select( from ).where( cb.equal( from.get( "value" ), selectCase ) );

			entityManager.createQuery( query ).getResultList();
		} );
	}

	@Test
	public void selectCaseWithCastedTypeValuesShouldWork(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			CriteriaBuilder.Case<String> selectCase = cb.selectCase();
			Predicate somePredicate = cb.equal( cb.literal( 1 ), 1 );
			selectCase.when( somePredicate, EnumValue.VALUE_1.name() );
			selectCase.otherwise( EnumValue.VALUE_2.name() );

			CriteriaQuery<Entity> query = cb.createQuery( Entity.class );
			Root<Entity> from = query.from( Entity.class );
			query.select( from )
					.where( cb.equal( from.get( "value" ).as( String.class ), selectCase.as( String.class ) ) );

			entityManager.createQuery( query ).getResultList();
		} );
	}

	@Test
	public void simpleSelectCaseWithValuesShouldWork(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			CriteriaBuilder.SimpleCase<Integer, EnumValue> selectCase = cb.selectCase( cb.literal( 1 ) );
			selectCase.when( 1, EnumValue.VALUE_1 );
			selectCase.otherwise( EnumValue.VALUE_2 );

			CriteriaQuery<Entity> query = cb.createQuery( Entity.class );
			Root<Entity> from = query.from( Entity.class );
			query.select( from ).where( cb.equal( from.get( "value" ), selectCase ) );

			entityManager.createQuery( query ).getResultList();
		} );
	}

	@Test
	public void simpleSelectCaseWithCastedTypeValuesShouldWork(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			CriteriaBuilder.SimpleCase<Integer, String> selectCase = cb.selectCase( cb.literal( 1 ) );
			selectCase.when( 1, EnumValue.VALUE_1.name() );
			selectCase.otherwise( EnumValue.VALUE_2.name() );

			CriteriaQuery<Entity> query = cb.createQuery( Entity.class );
			Root<Entity> from = query.from( Entity.class );
			query.select( from )
					.where( cb.equal( from.get( "value" ).as( String.class ), selectCase.as( String.class ) ) );

			entityManager.createQuery( query ).getResultList();
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-12184")
	public void selectCaseEnumExpression(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Entity( 1L, EnumValue.VALUE_1 ) );
			entityManager.persist( new Entity( 2L, EnumValue.VALUE_2 ) );
			entityManager.flush();

			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Entity> query = cb.createQuery( Entity.class );
			final Root<Entity> root = query.from( Entity.class );
			query
					.select( cb.construct( Entity.class,
							root.get( "id" ),
							cb.selectCase()
									.when( cb.equal( root.get( "id" ), 1L ), EnumValue.VALUE_2 )
									.otherwise( EnumValue.VALUE_1 )
					) )
					.orderBy( cb.asc( root.get( "id" ) ) );

			final List<Entity> resultList = entityManager.createQuery( query ).getResultList();
			assertEquals( 2, resultList.size() );
			assertEquals( EnumValue.VALUE_2, resultList.get( 0 ).value );
			assertEquals( EnumValue.VALUE_1, resultList.get( 1 ).value );

		} );
	}

	@jakarta.persistence.Entity
	@Table(name = "entity")
	public static class Entity {

		@Id
		private Long id;

		@Enumerated(EnumType.STRING)
		@Column(name = "val")
		private EnumValue value;

		public Entity() {
		}

		public Entity(Long id, EnumValue value) {
			this.id = id;
			this.value = value;
		}

		public Entity(Long id, Object value) {
			this.id = id;
			this.value = (EnumValue) value;
		}
	}

	public enum EnumValue {
		VALUE_1,
		VALUE_2
	}
}
