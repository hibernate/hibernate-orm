/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.caching;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel( annotatedClasses = {
		CachingAndVersionOneToOneLazyTest.Child.class,
		CachingAndVersionOneToOneLazyTest.Parent.class,
		CachingAndVersionOneToOneLazyTest.VersionedParent.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-16745" )
public class CachingAndVersionOneToOneLazyTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Child child = new Child( "child" );
			session.persist( new VersionedParent( "versioned_parent", child ) );
			session.persist( new Parent( "normal_parent", child ) );
			session.persist( child );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Child" ).executeUpdate();
			session.createMutationQuery( "delete from Parent" ).executeUpdate();
			session.createMutationQuery( "delete from VersionedParent" ).executeUpdate();
		} );
	}

	@Test
	public void testSelectVersionedParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final VersionedParent parent = session.createQuery(
					"select p from VersionedParent p",
					VersionedParent.class
			).getSingleResult();
			assertThat( parent.getData() ).isEqualTo( "versioned_parent" );
			assertThat( parent.getChild().getData() ).isEqualTo( "child" );
			assertThat( Hibernate.isInitialized( parent.getChild().getVersionedParent() ) ).isTrue();
		} );
	}

	@Test
	public void testSelectNormalParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = session.createQuery(
					"select p from Parent p",
					Parent.class
			).getSingleResult();
			assertThat( parent.getData() ).isEqualTo( "normal_parent" );
			assertThat( parent.getChild().getData() ).isEqualTo( "child" );
			assertThat( Hibernate.isInitialized( parent.getChild().getParent() ) ).isTrue();
		} );
	}

	@Test
	public void testSelectChild(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Child domainId = session.createQuery(
					"select c from Child c",
					Child.class
			).getSingleResult();
			assertThat( domainId.getData() ).isEqualTo( "child" );
			assertThat( Hibernate.isInitialized( domainId.getVersionedParent() ) ).isFalse();
			assertThat( domainId.getVersionedParent().getData() ).isEqualTo( "versioned_parent" );
			assertThat( Hibernate.isInitialized( domainId.getParent() ) ).isFalse();
			assertThat( domainId.getParent().getData() ).isEqualTo( "normal_parent" );
		} );
	}

	@Entity( name = "Parent" )
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne( mappedBy = "parent" )
		private Child child;

		private String data;

		public Parent() {
		}

		public Parent(String data, Child child) {
			this.data = data;
			this.child = child;
			child.setParent( this );
		}

		public Long getId() {
			return id;
		}

		public Child getChild() {
			return child;
		}

		public String getData() {
			return data;
		}
	}

	@Entity( name = "VersionedParent" )
	public static class VersionedParent {
		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer rowVersion;

		@OneToOne( mappedBy = "versionedParent" )
		private Child child;

		private String data;

		public VersionedParent() {
		}

		public VersionedParent(String data, Child child) {
			this.data = data;
			this.child = child;
			child.setVersionedParent( this );
		}

		public Long getId() {
			return id;
		}

		public Child getChild() {
			return child;
		}

		public String getData() {
			return data;
		}
	}

	@Entity( name = "Child" )
	@Cacheable
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

		private String data;

		@OneToOne( fetch = FetchType.LAZY )
		private VersionedParent versionedParent;

		@OneToOne( fetch = FetchType.LAZY )
		private Parent parent;

		public Child() {
		}

		public Child(String data) {
			this.data = data;
		}

		public Long getId() {
			return id;
		}

		public VersionedParent getVersionedParent() {
			return versionedParent;
		}

		public void setVersionedParent(VersionedParent versionedParent) {
			this.versionedParent = versionedParent;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public String getData() {
			return data;
		}
	}
}
