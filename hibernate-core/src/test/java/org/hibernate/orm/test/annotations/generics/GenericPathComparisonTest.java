/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		GenericPathComparisonTest.IdEntity.class,
		GenericPathComparisonTest.UserEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17454" )
public class GenericPathComparisonTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new UserEntity( 1L, "user_1", 1L ) );
			session.persist( new UserEntity( 2L, "user_2", 99L ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from UserEntity" ).executeUpdate() );
	}

	@Test
	public void testParamComparison(SessionFactoryScope scope) {
		scope.inTransaction( session -> executeQuery( session.createQuery(
				"from UserEntity where id = :id",
				UserEntity.class
		).setParameter( "id", 1L ) ) );
	}

	@Test
	public void testParamComparisonCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<UserEntity> query = cb.createQuery( UserEntity.class );
			final Root<UserEntity> from = query.from( UserEntity.class );
			query.where( cb.equal( from.get( "id" ), cb.parameter( Long.class, "id" ) ) );
			executeQuery( session.createQuery( query ).setParameter( "id", 1L ) );
		} );
	}

	@Test
	public void testLiteralComparison(SessionFactoryScope scope) {
		scope.inTransaction( session -> executeQuery( session.createQuery(
				"from UserEntity where id = 1",
				UserEntity.class
		) ) );
	}

	@Test
	public void testLiteralComparisonCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<UserEntity> query = cb.createQuery( UserEntity.class );
			final Root<UserEntity> from = query.from( UserEntity.class );
			query.where( cb.equal( from.get( "id" ), 1L ) );
			executeQuery( session.createQuery( query ) );
		} );
	}

	@Test
	public void testPathComparison(SessionFactoryScope scope) {
		scope.inTransaction( session -> executeQuery( session.createQuery(
				"from UserEntity where id = longProp",
				UserEntity.class
		) ) );
	}

	@Test
	public void testPathComparisonCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<UserEntity> query = cb.createQuery( UserEntity.class );
			final Root<UserEntity> from = query.from( UserEntity.class );
			query.where( cb.equal( from.get( "longProp" ), from.get( "id" ) ) );
			executeQuery( session.createQuery( query ) );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17959" )
	public void testInSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> executeQuery( session.createQuery(
				"from UserEntity where id in (select id from UserEntity where id = 1)",
				UserEntity.class
		) ) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17959" )
	public void testInSubqueryCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<UserEntity> query = cb.createQuery( UserEntity.class );
			final Root<UserEntity> from = query.from( UserEntity.class );
			final Subquery<Long> subquery = query.subquery( Long.class );
			final Path<Long> id = subquery.from( UserEntity.class ).get( "id" );
			query.where( from.get( "id" ).in( subquery.select( id ).where( cb.equal( id, 1L ) ) ) );
			executeQuery( session.createQuery( query ) );
		} );
	}

	private void executeQuery(TypedQuery<UserEntity> query) {
		final UserEntity result = query.getSingleResult();
		assertThat( result.getId() ).isEqualTo( 1L );
		assertThat( result.getName() ).isEqualTo( "user_1" );
	}

	@MappedSuperclass
	public static class IdEntity<PK extends Serializable> {
		@Id
		private PK id;

		public IdEntity() {
		}

		public IdEntity(final PK id) {
			this.id = id;
		}

		public PK getId() {
			return id;
		}
	}

	@Entity( name = "UserEntity" )
	public static class UserEntity extends IdEntity<Long> {
		private String name;

		private Long longProp;

		public UserEntity() {
		}

		public UserEntity(final Long id, final String name, final Long longProp) {
			super( id );
			this.name = name;
			this.longProp = longProp;
		}

		public String getName() {
			return name;
		}

		public Long getLongProp() {
			return longProp;
		}
	}
}
