/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytoone;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.CascadeType.ALL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ManyToOneBidirectionalEagerTest.OneEntity.class,
		ManyToOneBidirectionalEagerTest.ManyEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17783" )
public class ManyToOneBidirectionalEagerTest {
	@Test
	public void testQueryOneEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final OneEntity result = session.createQuery(
					"from OneEntity o",
					OneEntity.class
			).getSingleResult();
			assertThat( result.getMany() ).hasSize( 2 );
			result.getMany().forEach( m -> assertThat( m.getOne() ).isSameAs( result ) );
		} );
	}

	@Test
	public void testFindOneEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final OneEntity result = session.find( OneEntity.class, 1L );
			assertThat( result.getMany() ).hasSize( 2 );
			result.getMany().forEach( m -> assertThat( m.getOne() ).isSameAs( result ) );
		} );
	}

	@Test
	public void testQueryManyEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<ManyEntity> resultList = session.createQuery(
					"from ManyEntity m where m.id in ('a','b')",
					ManyEntity.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			final OneEntity one = resultList.get( 0 ).getOne();
			assertThat( one ).isSameAs( resultList.get( 1 ).getOne() );
			assertThat( one.getMany() ).containsAll( resultList );
		} );
	}

	@Test
	public void testFindManyEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ManyEntity a = session.find( ManyEntity.class, "a" );
			final ManyEntity b = session.find( ManyEntity.class, "b" );
			final OneEntity one = a.getOne();
			assertThat( one ).isSameAs( b.getOne() );
			assertThat( one.getMany() ).containsOnly( a, b );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ManyEntity many1 = new ManyEntity( "a", "many_1" );
			final ManyEntity many2 = new ManyEntity( "b", "many_2" );
			final OneEntity one = new OneEntity( 1L, "one_1" );
			one.addMany( many1 );
			one.addMany( many2 );
			session.persist( one );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ManyEntity" ).executeUpdate();
			session.createMutationQuery( "delete from OneEntity" ).executeUpdate();
		} );
	}

	@Entity( name = "ManyEntity" )
	public static class ManyEntity {
		@Id
		private String id;

		@Column( nullable = false )
		private String description;

		@ManyToOne
		@JoinColumn( name = "one_id" )
		private OneEntity one;

		public ManyEntity(String id, String description) {
			this.id = id;
			this.description = description;
		}

		public ManyEntity() {
		}

		public String getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

		public OneEntity getOne() {
			return one;
		}

		public void setOne(OneEntity one) {
			this.one = one;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			final ManyEntity that = (ManyEntity) o;
			return Objects.equals( id, that.id ) && Objects.equals( description, that.description );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, description );
		}
	}

	@Entity( name = "OneEntity" )
	public static class OneEntity {
		@Id
		private Long id;

		@Column( nullable = false )
		private String description;

		@OneToMany( mappedBy = "one", cascade = { ALL }, fetch = FetchType.EAGER )
		private Set<ManyEntity> many = new HashSet<>();

		public OneEntity(Long id, String description) {
			this.id = id;
			this.description = description;
		}

		public OneEntity() {
		}

		public Long getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

		public Set<ManyEntity> getMany() {
			return many;
		}

		public void addMany(ManyEntity newMany) {
			many.add( newMany );
			newMany.setOne( this );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			final OneEntity that = (OneEntity) o;
			return Objects.equals( id, that.id ) && Objects.equals( description, that.description );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, description );
		}
	}
}
