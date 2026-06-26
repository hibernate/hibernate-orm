/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.inheritance.joined;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				ProxyEntityCoexistenceTest.Foo.class,
				ProxyEntityCoexistenceTest.Bar.class,
				ProxyEntityCoexistenceTest.Baz.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20621")
public class ProxyEntityCoexistenceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Bar bar = new Bar();
			bar.setId( 1L );
			bar.setName( "bar1" );
			session.persist( bar );

			Baz baz = new Baz();
			baz.setId( 1L );
			baz.setFoo( bar );
			session.persist( baz );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Baz" ).executeUpdate();
			session.createMutationQuery( "delete from Bar" ).executeUpdate();
			session.createMutationQuery( "delete from Foo" ).executeUpdate();
		} );
	}

	@Test
	public void testProxyEntityCoexistence(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Step 1: Load Baz#1, creates a lazy proxy for Foo#1 typed as Foo (not Bar)
			Baz baz = session.find( Baz.class, 1L );
			assertThat( baz ).isNotNull();

			// Step 2: Access the reference, does NOT initialize the proxy
			Foo foo = baz.getFoo();
			assertThat( foo ).isNotNull();

			// Step 3: Load Bar#1 with its bazList using join fetch
			Bar bar = session.createQuery( "from Bar bar join fetch bar.bazList where bar.id = :id", Bar.class )
					.setParameter( "id", foo.getId() )
					.getSingleResult();
			assertThat( bar ).isNotNull();
			assertThat( bar.getBazList() ).hasSize( 1 );

			// Step 4: Load Baz#1 again with join fetch on foo
			Baz baz2 = session.createQuery( "from Baz baz join fetch baz.foo where baz.id = :id", Baz.class )
					.setParameter( "id", baz.getId() )
					.getSingleResult();
			assertThat( baz2 ).isNotNull();
			assertThat( baz2.getFoo() ).isNotNull();
			assertThat( Hibernate.unproxy( foo ) ).isSameAs( bar );

			// Step 5: This should not throw NPE
			Bar bar2 = session.find( Bar.class, 1L );
			assertThat( bar2 ).isNotNull();
			assertThat( bar2.getId() ).isEqualTo( 1L );
		} );
	}

	@Entity(name = "Foo")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Foo {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "foo")
		private List<Baz> bazList = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Baz> getBazList() {
			return bazList;
		}

		public void setBazList(List<Baz> bazList) {
			this.bazList = bazList;
		}
	}

	@Entity(name = "Bar")
	public static class Bar extends Foo {
	}

	@Entity(name = "Baz")
	public static class Baz {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Foo foo;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Foo getFoo() {
			return foo;
		}

		public void setFoo(Foo foo) {
			this.foo = foo;
		}
	}
}
