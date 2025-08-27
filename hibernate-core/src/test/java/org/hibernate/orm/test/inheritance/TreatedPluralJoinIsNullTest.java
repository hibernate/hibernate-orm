/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		TreatedPluralJoinIsNullTest.MainEntity.class,
		TreatedPluralJoinIsNullTest.ParentEntity.class,
		TreatedPluralJoinIsNullTest.ChildEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17178" )
public class TreatedPluralJoinIsNullTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new MainEntity() );
			final MainEntity main = new MainEntity();
			final ChildEntity child = new ChildEntity( main, "test_child" );
			session.persist( main );
			session.persist( child );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ParentEntity" ).executeUpdate();
			session.createMutationQuery( "delete from MainEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testIsNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MainEntity result = session.createQuery(
					"select m from MainEntity m left join treat(m.parents as ChildEntity) c where c is null",
					MainEntity.class
			).getSingleResult();
			assertThat( result.getParents() ).isEmpty();
		} );
	}

	@Test
	public void testIsNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MainEntity result = session.createQuery(
					"select m from MainEntity m left join treat(m.parents as ChildEntity) c where c is not null",
					MainEntity.class
			).getSingleResult();
			assertThat( result.getParents() ).hasSize( 1 );
			final ParentEntity parent = result.getParents().iterator().next();
			assertThat( parent ).isInstanceOf( ChildEntity.class );
			assertThat( ( (ChildEntity) parent ).getData() ).isEqualTo( "test_child" );
		} );
	}

	@Entity( name = "MainEntity" )
	public static class MainEntity {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany( mappedBy = "main" )
		private Collection<ParentEntity> parents = new HashSet<>();

		public Long getId() {
			return id;
		}

		public Collection<ParentEntity> getParents() {
			return parents;
		}
	}

	@Entity( name = "ParentEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public abstract static class ParentEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private MainEntity main;

		public ParentEntity() {
		}

		public ParentEntity(MainEntity main) {
			this.main = main;
		}

		public Long getId() {
			return id;
		}

		public MainEntity getMain() {
			return main;
		}
	}

	@Entity( name = "ChildEntity" )
	public static class ChildEntity extends ParentEntity {
		private String data;

		public ChildEntity() {
		}

		public ChildEntity(MainEntity main, String data) {
			super( main );
			this.data = data;
		}

		public String getData() {
			return data;
		}
	}
}
