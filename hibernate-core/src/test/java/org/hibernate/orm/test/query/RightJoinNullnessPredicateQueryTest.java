/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		RightJoinNullnessPredicateQueryTest.RelatedEntity.class,
		RightJoinNullnessPredicateQueryTest.MainEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17379" )
public class RightJoinNullnessPredicateQueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final RelatedEntity related = new RelatedEntity( 1L );
			session.persist( related );
			session.persist( new RelatedEntity( 2L ) );
			session.persist( new MainEntity( 3L, related ) );
			session.persist( new MainEntity( 4L, null ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from MainEntity" ).executeUpdate();
			session.createMutationQuery( "delete from RelatedEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testRightJoinIsNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Long> result = session.createQuery(
					"select r.id from MainEntity m right join m.related r where r is not null",
					Long.class
			).getResultList();
			assertThat( result ).hasSize( 2 );
			assertThat( result ).contains( 1L, 2L );
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsFullJoin.class )
	public void testFullJoinIsNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Long> result = session.createQuery(
					"select r.id from MainEntity m full join m.related r where r is null",
					Long.class
			).getResultList();
			assertThat( result ).hasSize( 1 );
			assertThat( result ).containsNull();
		} );
	}

	@Test
	public void testDereferenceIsNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Long> result = session.createQuery(
					"select r.id from MainEntity m right join m.related r where r.id is not null",
					Long.class
			).getResultList();
			assertThat( result ).hasSize( 2 );
			assertThat( result ).contains( 1L, 2L );
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsFullJoin.class )
	public void testDereferenceIsNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Long> result = session.createQuery(
					"select r.id from MainEntity m full join m.related r where r.id is null",
					Long.class
			).getResultList();
			assertThat( result ).hasSize( 1 );
			assertThat( result ).containsNull();
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17397" )
	public void testRightJoinCount(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long result = session.createQuery(
					"select count(r) from MainEntity m right join m.related r",
					Long.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 2L );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17397" )
	public void testDereferenceCount(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long result = session.createQuery(
					"select count(r.id) from MainEntity m right join m.related r",
					Long.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 2L );
		} );
	}

	@Entity( name = "RelatedEntity" )
	public static class RelatedEntity {
		@Id
		private Long id;

		public RelatedEntity() {
		}

		public RelatedEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}


	@Entity( name = "MainEntity" )
	public static class MainEntity {
		@Id
		private Long id;

		@OneToOne
		private RelatedEntity related;

		public MainEntity() {
		}

		public MainEntity(Long id, RelatedEntity related) {
			this.id = id;
			this.related = related;
		}

		public Long getId() {
			return id;
		}

		public RelatedEntity getRelated() {
			return related;
		}
	}
}
