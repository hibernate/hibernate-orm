/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.notfound.ignore;

import java.util.List;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		NotFoundManyToOneCountTest.MyEntity.class,
		NotFoundManyToOneCountTest.AssociatedEntity.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17638" )
public class NotFoundManyToOneCountTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AssociatedEntity associated1 = new AssociatedEntity( 1L, "associated_1" );
			session.persist( associated1 );
			session.persist( new MyEntity( 1L, associated1 ) );
			final AssociatedEntity associated2 = new AssociatedEntity( 2L, "associated_2" );
			session.persist( associated2 );
			session.persist( new MyEntity( 2L, associated2 ) );
		} );

		scope.inTransaction( session -> session.createMutationQuery( "delete from AssociatedEntity where id = 1" ).executeUpdate() );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from MyEntity" ).executeUpdate();
			session.createMutationQuery( "delete from AssociatedEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testCountQuery(SessionFactoryScope scope) {
		final String hql = "select %s from MyEntity e";
		final List<MyEntity> resultList = scope.fromTransaction( session -> session.createQuery( String.format(
				hql,
				"e"
		), MyEntity.class ).getResultList() );
		final Long count = scope.fromTransaction( session -> session.createQuery(
				String.format( hql, "count(e)" ),
				Long.class
		).getSingleResult() );
		assertThat( resultList ).hasSize( count.intValue() );
		assertThat( resultList.stream().map( MyEntity::getId ) ).contains( 1L, 2L );
	}

	@Test
	public void testCountQueryComparisonPredicate(SessionFactoryScope scope) {
		final String hql = "select %s from MyEntity e where e.associated = :associated";
		final AssociatedEntity associated = scope.fromTransaction( session -> session.find(
				AssociatedEntity.class,
				2L
		) );
		final List<MyEntity> resultList = scope.fromTransaction( session -> session.createQuery(
				String.format( hql, "e" ),
				MyEntity.class
		).setParameter( "associated", associated ).getResultList() );
		final Long count = scope.fromTransaction( session -> session.createQuery(
				String.format( hql, "count(e)" ),
				Long.class
		).setParameter( "associated", associated ).getSingleResult() );
		assertThat( resultList ).hasSize( count.intValue() );
		assertThat( resultList.stream().map( MyEntity::getId ) ).contains( 2L );
	}

	@Test
	public void testCountQueryInListPredicate(SessionFactoryScope scope) {
		final String hql = "select %s from MyEntity e where e.associated in :associated";
		final AssociatedEntity associated = scope.fromTransaction( session -> session.find(
				AssociatedEntity.class,
				2L
		) );
		final List<MyEntity> resultList = scope.fromTransaction( session -> session.createQuery(
				String.format( hql, "e" ),
				MyEntity.class
		).setParameter( "associated", associated ).getResultList() );
		final Long count = scope.fromTransaction( session -> session.createQuery(
				String.format( hql, "count(e)" ),
				Long.class
		).setParameter( "associated", associated ).getSingleResult() );
		assertThat( resultList ).hasSize( count.intValue() );
		assertThat( resultList.stream().map( MyEntity::getId ) ).contains( 2L );
	}

	@Entity( name = "MyEntity" )
	public static class MyEntity {
		@Id
		private Long id;

		@ManyToOne
		@NotFound( action = NotFoundAction.IGNORE )
		@JoinColumn( name = "associated_id" )
		private AssociatedEntity associated;

		public MyEntity() {
		}

		public MyEntity(Long id, AssociatedEntity associated) {
			this.id = id;
			this.associated = associated;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "AssociatedEntity" )
	public static class AssociatedEntity {
		@Id
		private Long id;

		private String name;

		public AssociatedEntity() {
		}

		public AssociatedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
