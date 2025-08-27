/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.join;

import java.io.Serializable;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		JoinedHierarchyDiscIdSelectTest.DooredVehicle.class,
		JoinedHierarchyDiscIdSelectTest.BaseVehicle.class,
		JoinedHierarchyDiscIdSelectTest.BaseEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18503" )
public class JoinedHierarchyDiscIdSelectTest {
	@ParameterizedTest
	@ValueSource( classes = {
			DooredVehicle.class,
			BaseVehicle.class,
			BaseEntity.class,
	} )
	void testSelectCriteriaId(Class<?> klass, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery( Long.class );
			final Root<?> root = criteriaQuery.from( klass );
			criteriaQuery.select( root.get( "id" ) );
			final List<Long> resultList = session.createQuery( criteriaQuery ).getResultList();
			assertThat( resultList ).hasSize( 1 ).containsOnly( 1L );
		} );
	}

	@ParameterizedTest
	@ValueSource( classes = {
			DooredVehicle.class,
			BaseVehicle.class,
			BaseEntity.class,
	} )
	void testSelectId(Class<?> klass, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long result = session.createQuery(
					String.format( "select id from %s", klass.getSimpleName() ),
					Long.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 1L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( DooredVehicle.create( 1L ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "DooredVehicle" )
	static class DooredVehicle extends BaseVehicle {
		public String doorType;

		public static DooredVehicle create(Long id) {
			DooredVehicle vehicle = new DooredVehicle();
			vehicle.id = id;
			return vehicle;
		}
	}

	@Entity( name = "BaseVehicle" )
	static class BaseVehicle extends BaseEntity {
		public String bodyType;
	}

	@Entity( name = "BaseEntity" )
	@DiscriminatorColumn( name = "type" )
	@Inheritance( strategy = InheritanceType.JOINED )
	static class BaseEntity implements Serializable {
		@Id
		public Long id;
	}
}
